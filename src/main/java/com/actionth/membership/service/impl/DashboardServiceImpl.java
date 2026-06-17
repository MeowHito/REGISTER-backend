package com.actionth.membership.service.impl;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.AgeGroup.Gender;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventPermission;
import com.actionth.membership.model.EventType;
import com.actionth.membership.model.User;
import com.actionth.membership.model.dto.DashboardEventDTO;
import com.actionth.membership.model.dto.DashboardOrganizerDTO;
import com.actionth.membership.model.dto.DashboardOverviewDTO;
import com.actionth.membership.model.dto.DashboardRegistrationDTO;
import com.actionth.membership.model.dto.EventDto;
import com.actionth.membership.repository.DashboardRepository;
import com.actionth.membership.repository.EventRepository;
import com.actionth.membership.repository.EventTypeRepository;
import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.service.DashboardService;
import com.actionth.membership.service.UserService;

import lombok.RequiredArgsConstructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardServiceImpl.class);

    private final DashboardRepository dashboardRepository;
    private final EventRepository eventRepository;
    private final EventTypeRepository eventTypeRepository;
    private final OrderRepository orderRepository;
    private final UserService userService;

    private User requireUser() {
        User user = userService.getCurrentUserSession();
        if (user == null || user.getId() == null || user.getUuid() == null || user.getUuid().isBlank()) {
            throw new org.springframework.security.access.AccessDeniedException("Unauthenticated or missing user");
        }
        return user;
    }

    private boolean isAdmin(User u) {
        return u != null && u.getRole() != null && "admin".equalsIgnoreCase(u.getRole().getRoleType());
    }

    @Override
    public List<EventDto> getAllEvents() {
        User user = requireUser();
        boolean admin = isAdmin(user);

        Integer userId = user.getId();

        Sort sort = Sort.by(Sort.Order.desc("createdTime").with(Sort.NullHandling.NULLS_LAST));

        Specification<Event> spec = (root, query, cb) -> {
            query.distinct(true);

            if (admin) {
                return cb.conjunction();
            }

            Predicate isOwner = cb.and(
                    cb.isNotNull(root.get("organizer")),
                    cb.equal(root.get("organizer").get("id"), userId));

            var sq = query.subquery(Integer.class);
            var ep = sq.from(EventPermission.class);
            sq.select(cb.literal(1));
            sq.where(
                    cb.equal(ep.get("event"), root),
                    cb.equal(ep.get("user").get("id"), userId),
                    cb.isTrue(ep.get("canRead")),
                    cb.isTrue(ep.get("active")));

            return cb.or(isOwner, cb.exists(sq));
        };

        return eventRepository.findAll(spec, sort).stream()
                .filter(e -> e != null && e.getUuid() != null && e.getName() != null)
                .map(e -> EventDto.builder()
                        .id(e.getUuid())
                        .name(e.getName())
                        .eventDate(e.getEventDate())
                        .build())
                .toList();
    }

    @Override
    public DashboardOverviewDTO getDashboardOverview(String eventUuid) {
        User user = requireUser();
        boolean admin = isAdmin(user);
        
        String userUuid = user.getUuid();
        Integer userId  = user.getId();

        Event event = eventRepository.findByUuid(eventUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        Long totalParticipants = dashboardRepository.countByEvent(eventUuid, userUuid, admin);

        List<EventType> eventTypes = eventTypeRepository.findByEventUuid(event.getUuid());

        int capacityByEvent = eventTypes.stream()
                .mapToInt(et -> et.getQuota() != null ? et.getQuota() : 0)
                .sum();

        Map<String, Integer> quotaByType = eventTypes.stream()
                .collect(Collectors.toMap(
                        EventType::getName,
                        et -> Optional.ofNullable(et.getQuota()).orElse(0),
                        (a, b) -> a,
                        LinkedHashMap::new));

        Map<String, Map<String, Integer>> participantWithCapacity = new LinkedHashMap<>();
        quotaByType.forEach((name, quota) -> {
            participantWithCapacity.put(name, Map.of(
                    "participant", 0,
                    "capacityByEventType", quota));
        });

        List<Map<String, Object>> breakdown;
        try {
            breakdown = Optional.ofNullable(dashboardRepository.countByEventType(eventUuid, userUuid, admin))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.error("Error in countParticipantsByEventType", e);
            breakdown = Collections.emptyList();
        }

        for (Map<String, Object> entry : breakdown) {
            String eventTypeName = Optional.ofNullable(entry.get("eventType"))
                    .map(Object::toString)
                    .orElse("unknown");

            int count = Optional.ofNullable(entry.get("count"))
                    .map(val -> ((Number) val).intValue())
                    .orElse(0);

            int quota = quotaByType.getOrDefault(eventTypeName, 0);

            participantWithCapacity.put(eventTypeName, Map.of(
                    "participant", count,
                    "capacityByEventType", quota));
        }

        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime open = event.getStartRegistrationDate();
        OffsetDateTime close = event.getEndRegistrationDate();

        if (open == null || close == null) {
            throw new IllegalStateException("startRegistrationDate/endRegistrationDate is required");
        }

        if (!close.isAfter(open)) {
            throw new IllegalStateException("endRegistrationDate must be after startRegistrationDate");
        }

        String operationStatusCode;
        int progressOperation;

        if (now.isBefore(open)) {
            operationStatusCode = "REGISTRATION_NOT_OPEN_YET";
            progressOperation = 0;
        } else if (now.isBefore(close)) {
            long total = Duration.between(open, close).toMillis();
            long elapsed = Duration.between(open, now).toMillis();
            progressOperation = (int) Math.min(100, Math.max(0, Math.round(elapsed * 100.0 / total)));
            operationStatusCode = "REGISTRATION_OPEN";
        } else {
            operationStatusCode = "REGISTRATION_CLOSED";
            progressOperation = 100;
        }

        long elapsedDays = ChronoUnit.DAYS.between(
                open.toLocalDate(),
                (now.isAfter(close) ? close : now).toLocalDate()) + 1;
        if (elapsedDays < 0)
            elapsedDays = 0;

        int progressApplicants = 0;
        if (capacityByEvent > 0) {
            progressApplicants = (int) ((totalParticipants * 100) / capacityByEvent);
        }

        Long totalRegistrationFee = Optional.ofNullable(orderRepository.sumRegistrationUnitPrice(eventUuid)).orElse(0L);
        Long totalShippingFee = Optional.ofNullable(orderRepository.sumShippingFee(eventUuid)).orElse(0L);
        Long totalNetRevenue = Optional.ofNullable(orderRepository.sumTotalNetAmount(eventUuid)).orElse(0L);

        List<Map<String, Object>> paymentStatusByMethodRaw = Collections.emptyList();
        try {
            paymentStatusByMethodRaw = Optional
                    .ofNullable(dashboardRepository.countPaymentStatusByMethod(eventUuid, userId, admin))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to count payment status by method. Fallback to empty list.", e);
        }
        List<Map<String, Object>> paymentStatusByMethod = normalizePaymentStatusByMethod(paymentStatusByMethodRaw);

        List<Map<String, Object>> participantsPerDayRaw = Collections.emptyList();
        try {
            participantsPerDayRaw = Optional.ofNullable(
                    dashboardRepository.countPerDay(eventUuid, userUuid, admin)).orElse(Collections.emptyList());
        } catch (Exception e) {
            log.warn("Failed to count participants per day, fallback to empty list", e);
        }

        List<Map<String, Object>> participantsPerDay = buildParticipantsPerDayWithCumulative(participantsPerDayRaw);

        List<Map<String, Object>> paidParticipantsPerDay = Collections.emptyList();
        try {
            List<Map<String, Object>> paidRaw = Optional.ofNullable(
                    dashboardRepository.countPaidParticipantsByPaymentDate(eventUuid, userUuid, admin))
                    .orElse(Collections.emptyList());
            paidParticipantsPerDay = buildPaidParticipantsPerDay(participantsPerDay, paidRaw);
        } catch (Exception e) {
            log.warn("Failed to get paid payments by date, fallback to empty list", e);
        }

        List<Map<String, Object>> failureReasonsRaw = Collections.emptyList();
        try {
            failureReasonsRaw = Optional
                    .ofNullable(dashboardRepository.countFailureReasons(eventUuid, userId, admin))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to count failure reasons. Fallback to empty list.", e);
        }
        List<Map<String, Object>> failureReasons = normalizeFailureReasons(failureReasonsRaw);

        LinkedHashMap<String, Integer> paidByEventType = new LinkedHashMap<>();
        quotaByType.keySet().forEach(name -> paidByEventType.put(name, 0));

        try {
            List<Map<String, Object>> rows = Optional
                    .ofNullable(dashboardRepository.countPaidByEventType(eventUuid, userUuid, admin))
                    .orElse(List.of());

            for (Map<String, Object> row : rows) {
                if (row == null)
                    continue;

                String type = String.valueOf(row.get("eventType"));
                int total = Optional.ofNullable(row.get("total"))
                        .map(v -> ((Number) v).intValue())
                        .orElse(0);

                paidByEventType.put(type, total);
            }
        } catch (Exception e) {
            log.warn("Failed to count paid participants by event type. Fallback to zeros.", e);
        }

        long paidSum = paidByEventType.values().stream()
                .mapToLong(Integer::longValue).sum();

        int progressPayment = 0;
        if (totalParticipants > 0) {
            progressPayment = (int) Math.round(paidSum * 100.0 / totalParticipants);
        }

        return DashboardOverviewDTO.builder()
                .participantByEvent(totalParticipants.intValue())
                .participantByEventType(participantWithCapacity)
                .capacityByEvent(capacityByEvent)
                .operationStatusCode(operationStatusCode)
                .elapsedDays(elapsedDays)
                .progressOperation(progressOperation)
                .progressApplicants(progressApplicants)
                .paidByEvent((int) paidSum)
                .paidByEventType(paidByEventType)
                .progressPayment(progressPayment)
                .totalRegistrationFee(totalRegistrationFee)
                .totalShippingFee(totalShippingFee)
                .totalNetRevenue(totalNetRevenue)
                .paymentStatusByMethod(paymentStatusByMethod)
                .participantsPerDay(participantsPerDay)
                .paidParticipantsPerDay(paidParticipantsPerDay)
                .failureReasons(failureReasons)
                .build();
    }

    private List<Map<String, Object>> buildParticipantsPerDayWithCumulative(
            List<Map<String, Object>> dailyList) {

        List<Map<String, Object>> result = new ArrayList<>();
        if (dailyList == null || dailyList.isEmpty())
            return result;

        Map<LocalDate, Integer> perDay = new TreeMap<>();
        for (Map<String, Object> day : dailyList) {
            Object dateObj = day.get("date");
            LocalDate d;
            if (dateObj instanceof LocalDate ld)
                d = ld;
            else if (dateObj instanceof OffsetDateTime odt)
                d = odt.toLocalDate();
            else if (dateObj instanceof java.sql.Timestamp ts)
                d = ts.toLocalDateTime().toLocalDate();
            else if (dateObj instanceof java.sql.Date sd)
                d = sd.toLocalDate();
            else
                throw new IllegalArgumentException("Unsupported date type: " +
                        dateObj.getClass());

            int total = ((Number) day.get("total")).intValue();
            perDay.merge(d, total, Integer::sum);
        }

        int cumulative = 0;
        for (Map.Entry<LocalDate, Integer> e : perDay.entrySet()) {
            cumulative += e.getValue();
            Map<String, Object> m = new HashMap<>();
            m.put("dateTime", e.getKey().atStartOfDay().atOffset(ZoneOffset.UTC).toString());
            m.put("daily", e.getValue());
            m.put("cumulative", cumulative);
            result.add(m);
        }
        return result;
    }

    private List<Map<String, Object>> buildPaidParticipantsPerDay(
            List<Map<String, Object>> referenceDays,
            List<Map<String, Object>> paidRows) {

        Map<LocalDate, Integer> paidMap = new HashMap<>();
        for (Map<String, Object> row : paidRows) {
            if (row == null)
                continue;

            Object dateObj = row.get("date");
            int total = ((Number) row.get("total")).intValue();

            LocalDate d;
            if (dateObj instanceof LocalDate ld)
                d = ld;
            else if (dateObj instanceof java.sql.Date sd)
                d = sd.toLocalDate();
            else if (dateObj instanceof java.sql.Timestamp ts)
                d = ts.toLocalDateTime().toLocalDate();
            else if (dateObj instanceof OffsetDateTime odt)
                d = odt.toLocalDate();
            else
                d = LocalDate.parse(String.valueOf(dateObj));

            paidMap.merge(d, total, Integer::sum);
        }

        SortedSet<LocalDate> allDays = new TreeSet<>();

        for (Map<String, Object> ref : referenceDays) {
            if (ref == null)
                continue;
            String dtStr = String.valueOf(ref.get("dateTime"));
            if (dtStr == null || "null".equals(dtStr))
                continue;

            LocalDate d;
            try {
                d = OffsetDateTime.parse(dtStr).toLocalDate();
            } catch (Exception ignore) {
                d = LocalDateTime.parse(dtStr).toLocalDate();
            }
            allDays.add(d);
        }
        allDays.addAll(paidMap.keySet());

        List<Map<String, Object>> result = new ArrayList<>();
        int cumulative = 0;
        for (LocalDate d : allDays) {
            int daily = paidMap.getOrDefault(d, 0);
            cumulative += daily;

            Map<String, Object> m = new HashMap<>();
            m.put("dateTime", d.atStartOfDay().atOffset(ZoneOffset.UTC).toString());
            m.put("daily", daily);
            m.put("cumulative", cumulative);
            result.add(m);
        }
        return result;
    }

    private enum StatusGroup {
        SUCCESS, PENDING, FAILED
    }

    private static final List<StatusGroup> STATUS_ORDER = List.of(StatusGroup.SUCCESS, StatusGroup.PENDING,
            StatusGroup.FAILED);

    private static int asInt(Object v) {
        if (v instanceof Number n)
            return n.intValue();
        if (v == null)
            return 0;
        try {
            return Integer.parseInt(String.valueOf(v));
        } catch (Exception ignore) {
            return 0;
        }
    }

    private List<Map<String, Object>> normalizePaymentStatusByMethod(List<Map<String, Object>> rows) {
        Map<String, EnumMap<StatusGroup, Integer>> agg = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        for (Map<String, Object> r : rows == null ? List.<Map<String, Object>>of() : rows) {
            if (r == null)
                continue;

            String method = String.valueOf(r.getOrDefault("method", "UNKNOWN")).trim();
            String s = String.valueOf(r.getOrDefault("status", "FAILED")).trim().toUpperCase();

            StatusGroup status;
            try {
                status = StatusGroup.valueOf(s);
            } catch (Exception e) {
                status = StatusGroup.FAILED;
            }

            int cnt = asInt(r.get("count"));

            String key = method.isEmpty() ? "UNKNOWN" : method;

            agg.computeIfAbsent(key, k -> {
                EnumMap<StatusGroup, Integer> m = new EnumMap<>(StatusGroup.class);
                for (StatusGroup st : StatusGroup.values()) m.put(st, 0);
                return m;
            });

            agg.get(key).merge(status, cnt, Integer::sum);
        }

        var methodsSorted = new ArrayList<>(agg.keySet());
        methodsSorted.sort((a, b) -> {
            boolean ua = "UNKNOWN".equalsIgnoreCase(a);
            boolean ub = "UNKNOWN".equalsIgnoreCase(b);
            if (ua && !ub)
                return 1;
            if (!ua && ub)
                return -1;
            return a.compareToIgnoreCase(b);
        });

        List<Map<String, Object>> out = new ArrayList<>();
        for (String method : methodsSorted) {
            EnumMap<StatusGroup, Integer> m = agg.get(method);
            for (StatusGroup st : STATUS_ORDER) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("method", method);
                row.put("status", st.name());
                row.put("count", m.getOrDefault(st, 0));
                out.add(row);
            }
        }
        return out;
    }

    private List<Map<String, Object>> normalizeFailureReasons(List<Map<String, Object>> rows) {
        int failed = 0, canceled = 0;
        for (Map<String, Object> r : rows == null ? List.<Map<String, Object>>of() : rows) {
            if (r == null)
                continue;
            String reason = String.valueOf(r.get("reason")).trim().toUpperCase();
            int cnt = asInt(r.get("count"));
            if ("FAILED".equals(reason))
                failed += cnt;
            else if ("CANCELED".equals(reason) || "CANCELLED".equals(reason))
                canceled += cnt;
        }

        return List.of(
                Map.of("reason", "FAILED", "count", failed),
                Map.of("reason", "CANCELED", "count", canceled));
    }

    @Override
    public List<DashboardOrganizerDTO> getEventsAndOrganizers() {
        User user = requireUser();
        boolean admin = isAdmin(user);
        
        Integer userId  = user.getId();

        Specification<Event> spec = (root, query, cb) -> {
            query.distinct(true);

            if (admin) {
                return cb.conjunction();
            }

            Predicate isOwner = cb.and(
                    cb.isNotNull(root.get("organizer")),
                    cb.equal(root.get("organizer").get("id"), userId));

            var sq = query.subquery(Integer.class);
            var ep = sq.from(EventPermission.class);
            sq.select(cb.literal(1));
            sq.where(
                    cb.equal(ep.get("event"), root),
                    cb.equal(ep.get("user").get("id"), userId),
                    cb.isTrue(ep.get("canRead")),
                    cb.isTrue(ep.get("active")));

            return cb.or(isOwner, cb.exists(sq));
        };

        Sort sort = Sort.by(Sort.Order.desc("createdTime").with(Sort.NullHandling.NULLS_LAST));
        List<Event> events = eventRepository.findAll(spec, sort);

        Map<String, DashboardOrganizerDTO> organizerMap = new LinkedHashMap<>();
        for (Event event : events) {
            if (event == null || event.getUuid() == null || event.getName() == null)
                continue;

            String organizerId = event.getOrganizer() != null ? event.getOrganizer().getUuid() : "UNKNOWN";

            String organizerName = (event.getOrganizer() != null)
                    ? (Optional.ofNullable(event.getOrganizer().getFirstName()).orElse("") + " " +
                            Optional.ofNullable(event.getOrganizer().getLastName()).orElse("")).trim()
                    : Optional.ofNullable(event.getOrganizerName()).orElse("Unknown Organizer");

            String eventDateStr = event.getEventDate() != null ? event.getEventDate().toString() : null;

            DashboardOrganizerDTO dto = organizerMap.computeIfAbsent(
                    organizerId, k -> new DashboardOrganizerDTO(organizerId, organizerName, new ArrayList<>()));

            dto.getEvents().add(new DashboardEventDTO(event.getUuid(), event.getName(), eventDateStr));
        }

        return new ArrayList<>(organizerMap.values());
    }

    @Override
    public DashboardRegistrationDTO getDashboardRegistration(String eventUuid) {
        User user = requireUser();
        boolean admin = isAdmin(user);
        
        String userUuid = user.getUuid();
        Integer userId  = user.getId();

        Event event = eventRepository.findByUuid(eventUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        Long totalParticipants;
        try {
            totalParticipants = Optional.ofNullable(
                    dashboardRepository.countByEvent(eventUuid, userUuid, admin)).orElse(0L);
        } catch (Exception e) {
            log.error("Failed to count participants by event", e);
            totalParticipants = 0L;
        }

        List<EventType> eventTypes = eventTypeRepository.findByEventUuid(event.getUuid());

        int totalCapacity = eventTypes.stream()
                .mapToInt(et -> et.getQuota() != null ? et.getQuota() : 0)
                .sum();

        Map<String, Integer> quotaByType = eventTypes.stream()
                .collect(Collectors.toMap(
                        EventType::getName,
                        et -> Optional.ofNullable(et.getQuota()).orElse(0),
                        (a, b) -> a,
                        LinkedHashMap::new));

        LinkedHashMap<String, Integer> paidByEventType = quotaByType.keySet().stream()
                .collect(Collectors.toMap(
                        k -> k, k -> 0,
                        (a, b) -> a,
                        LinkedHashMap::new));

        try {
            List<Map<String, Object>> rows = Optional
                    .ofNullable(dashboardRepository.countPaidByEventType(eventUuid, userUuid, admin))
                    .orElse(List.of());

            for (Map<String, Object> row : rows) {
                if (row == null)
                    continue;

                String eventType = String.valueOf(row.get("eventType"));
                int total = Optional.ofNullable(row.get("total"))
                        .map(v -> ((Number) v).intValue())
                        .orElse(0);

                paidByEventType.put(eventType, total);
            }
        } catch (Exception e) {
            log.warn("Failed to count paid participants by event type. Fallback to zeros.", e);
        }

        long paidSum = paidByEventType.values().stream()
                .mapToLong(Integer::longValue).sum();

        Long paid = 0L;
        Long pending = 0L;
        Long unpaid = 0L;
        try {
            paid = Optional.ofNullable(
                    orderRepository.countOrdersByStatus(eventUuid, "SUCCESS")).orElse(0L);

            pending = Optional.ofNullable(
                    orderRepository.countOrdersByStatus(eventUuid, "PENDING")).orElse(0L);

            unpaid = Optional.ofNullable(
                    orderRepository.countOrdersByStatuses(
                            eventUuid,
                            List.of("FAILED", "CANCELLED", "CANCELED", "CANCEL")))
                    .orElse(0L);
        } catch (Exception e) {
            log.error("Failed to count orders by payment status", e);
        }

        Map<String, Map<String, Integer>> participantWithCapacity = new LinkedHashMap<>();
        quotaByType.forEach((name, quota) -> {
            participantWithCapacity.put(name, Map.of(
                    "participant", 0,
                    "capacity", quota));
        });

        List<Map<String, Object>> breakdown;
        try {
            breakdown = Optional.ofNullable(dashboardRepository.countByEventType(eventUuid, userUuid, admin))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to count participants by event type", e);
            breakdown = Collections.emptyList();
        }

        for (Map<String, Object> entry : breakdown) {
            String eventTypeName = Optional.ofNullable(entry.get("eventType"))
                    .map(Object::toString)
                    .orElse("unknown");

            int count = Optional.ofNullable(entry.get("count"))
                    .map(val -> ((Number) val).intValue())
                    .orElse(0);

            int quota = quotaByType.getOrDefault(eventTypeName, 0);

            participantWithCapacity.put(eventTypeName, Map.of(
                    "participant", count,
                    "capacity", quota));
        }

        Map<String, Integer> genderByEvent = new HashMap<>();
        List<Map<String, Object>> genderCounts;

        try {
            genderCounts = dashboardRepository.countPaidGenderByEvent(eventUuid, userUuid, admin);
        } catch (Exception e) {
            log.error("DB error while fetching gender counts", e);
            genderCounts = Collections.emptyList();
        }

        for (Map<String, Object> row : genderCounts) {
            if (row == null)
                continue;

            Object genderObj = row.get("gender");
            Object countObj = row.get("count");

            if (genderObj instanceof String gender && countObj instanceof Number countNum) {
                String genderNormalized = normalizeGender(gender);
                Integer count = countNum.intValue();
                genderByEvent.merge(genderNormalized, count, Integer::sum);
            } else {
                log.warn("Invalid row data found in genderCounts: {}", row);
            }
        }

        Map<String, Map<String, Integer>> genderByEventType = new HashMap<>();
        List<Map<String, Object>> counts;

        try {
            counts = dashboardRepository.countPaidGenderByEventType(eventUuid, userUuid, admin);
            if (counts == null) {
                counts = Collections.emptyList();
            }
        } catch (Exception e) {
            log.error("DB error while fetching gender counts by event type", e);
            counts = Collections.emptyList();
        }

        for (Map<String, Object> row : counts) {
            if (row == null)
                continue;

            Object eventTypeObj = row.get("eventType");
            Object genderObj = row.get("gender");
            Object countObj = row.get("count");

            if (eventTypeObj instanceof String eventType
                    && genderObj instanceof String gender
                    && countObj instanceof Number countNum) {

                String genderNormalized = normalizeGender(gender);
                int count = countNum.intValue();

                genderByEventType
                        .computeIfAbsent(eventType, k -> new HashMap<>())
                        .merge(genderNormalized, count, Integer::sum);

            } else {
                log.warn("Invalid row in genderByEventType result: {}", row);
            }
        }

        Map<String, Map<String, Integer>> ageGroupByEvent = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = dashboardRepository.countPaidAgeGroupByEvent(eventUuid, userId, admin);

            for (Map<String, Object> row : rows) {
                String ageGroup = String.valueOf(row.get("ageGroup"));
                String gender = normalizeGender(String.valueOf(row.get("gender")));
                Integer count = ((Number) row.get("count")).intValue();

                ageGroupByEvent
                        .computeIfAbsent(ageGroup, k -> new LinkedHashMap<>())
                        .merge(gender, count, Integer::sum);
            }
        } catch (Exception e) {
            log.error("Failed to count age group by gender", e);
        }

        Map<String, Map<String, Map<String, Integer>>> ageGroupByEventType = new LinkedHashMap<>();
        try {
            List<Map<String, Object>> rows = dashboardRepository.countPaidAgeGroupByEventType(eventUuid, userId, admin);

            for (Map<String, Object> row : rows) {
                String eventType = String.valueOf(row.get("eventType"));
                String ageGroup = String.valueOf(row.get("ageGroup"));
                String gender = normalizeGender(String.valueOf(row.get("gender")));
                Integer count = ((Number) row.get("count")).intValue();

                ageGroupByEventType
                        .computeIfAbsent(eventType, k -> new LinkedHashMap<>())
                        .computeIfAbsent(ageGroup, k -> new LinkedHashMap<>())
                        .merge(gender, count, Integer::sum);
            }
        } catch (Exception e) {
            log.error("Failed to count age group by gender and event type", e);
        }

        Map<String, Map<String, Integer>> shirtByEvent = new HashMap<>();

        List<Map<String, Object>> shirtRows;
        try {
            shirtRows = Optional
                    .ofNullable(dashboardRepository.countPaidShirtByEvent(eventUuid, userUuid, admin))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to count paid shirts by event", e);
            shirtRows = Collections.emptyList();
        }

        for (Map<String, Object> row : shirtRows) {
            if (row == null)
                continue;

            Object typeObj = row.get("shirtType");
            Object sizeObj = row.get("shirtSize");
            Object countObj = row.get("count");

            if (typeObj instanceof String shirtType &&
                    sizeObj instanceof String size &&
                    countObj instanceof Number countNum) {

                int count = countNum.intValue();

                shirtByEvent
                        .computeIfAbsent(shirtType, k -> new HashMap<>())
                        .merge(size, count, Integer::sum);
            } else {
                log.warn("Invalid row data in shirtByEvent: {}", row);
            }
        }

        Map<String, Map<String, Map<String, Integer>>> shirtByEventType = new HashMap<>();

        List<Map<String, Object>> shirtByTypeRows;
        try {
            shirtByTypeRows = Optional
                    .ofNullable(dashboardRepository.countPaidShirtByEventType(eventUuid, userUuid, admin))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to count paid shirts by event type", e);
            shirtByTypeRows = Collections.emptyList();
        }

        for (Map<String, Object> row : shirtByTypeRows) {
            if (row == null)
                continue;

            Object eventTypeObj = row.get("eventType");
            Object typeObj = row.get("shirtType");
            Object sizeObj = row.get("shirtSize");
            Object countObj = row.get("count");

            if (eventTypeObj instanceof String eventType &&
                    typeObj instanceof String shirtType &&
                    sizeObj instanceof String size &&
                    countObj instanceof Number countNum) {

                int count = countNum.intValue();

                shirtByEventType
                        .computeIfAbsent(eventType, k -> new HashMap<>())
                        .computeIfAbsent(shirtType, k -> new HashMap<>())
                        .merge(size, count, Integer::sum);
            } else {
                log.warn("Invalid row data in shirtByEventType: {}", row);
            }
        }

        List<Map<String, Object>> provinceStats;
        try {
            provinceStats = Optional
                    .ofNullable(dashboardRepository.countPaidByProvince(eventUuid, userUuid, admin))
                    .orElse(Collections.emptyList());
        } catch (Exception e) {
            log.error("Failed to count paid participants by province", e);
            provinceStats = Collections.emptyList();
        }

        Map<String, Integer> provinceCountMap = new HashMap<>();
        for (Map<String, Object> entry : provinceStats) {
            if (entry == null)
                continue;

            Object provinceObj = entry.get("province");
            Object countObj = entry.get("cnt");

            if (provinceObj instanceof String province &&
                    countObj instanceof Number countNum) {
                provinceCountMap.put(province, countNum.intValue());
            } else {
                log.warn("Invalid province stat row: {}", entry);
            }
        }

        List<DashboardRegistrationDTO.TimeBucketCountDto> registerDateCountMap;
        try {
            registerDateCountMap = Optional
                    .ofNullable(dashboardRepository.countPaidByRegisterDate(eventUuid, userId, admin))
                    .orElse(Collections.emptyList())
                    .stream()
                    .filter(Objects::nonNull)
                    .map(r -> DashboardRegistrationDTO.TimeBucketCountDto.builder()
                            .dateTime(r.getDateTime() == null ? null : r.getDateTime().toInstant().atOffset(ZoneOffset.UTC))
                            .count(Optional.ofNullable(r.getCount()).orElse(0))
                            .build())
                    .filter(x -> x.getDateTime() != null)
                    .toList();
        } catch (Exception e) {
            log.error("Failed to count paid participants by register date", e);
            registerDateCountMap = Collections.emptyList();
        }

        OffsetDateTime registrationOpen = toUtc(event.getStartRegistrationDate());
        OffsetDateTime registrationClose = toUtc(event.getEndRegistrationDate());

        int countInternal = 0; // ยังไม่มีข้อมูลจริง
        int countExternal = 0; // ยังไม่มีข้อมูลจริง

        return DashboardRegistrationDTO.builder()
                .eventId(event.getUuid())
                .eventName(event.getName())
                .participantByEvent(totalParticipants.intValue())
                .participantByEventType(participantWithCapacity)
                .capacityByEvent(totalCapacity)
                .paidByEvent((int) paidSum)
                .paidByEventType(paidByEventType)
                .paidPayment(paid.intValue())
                .unpaidPayment(unpaid.intValue())
                .pendingPayment(pending.intValue())
                .genderByEvent(genderByEvent)
                .genderByEventType(genderByEventType)
                .ageGroupByEvent(ageGroupByEvent)
                .ageGroupByEventType(ageGroupByEventType)
                .shirtByEvent(shirtByEvent)
                .shirtByEventType(shirtByEventType)
                .participantByProvince(provinceCountMap)
                .participantRegisterDate(registerDateCountMap)
                .registrationOpen(registrationOpen)
                .registrationClose(registrationClose)
                .countInternalParticipant(countInternal)
                .countExternalParticipant(countExternal)
                .build();
    }

    private String normalizeGender(Object gender) {
        if (gender == null)
            return "unknown";

        final String genderStr;
        if (gender instanceof Gender g) {
            genderStr = g.name().toLowerCase();
        } else {
            genderStr = gender.toString().trim().toLowerCase();
        }

        if (genderStr.equals("f") || genderStr.equals("female"))
            return "female";
        if (genderStr.equals("m") || genderStr.equals("male"))
            return "male";
        return "unknown";
    }

    private static OffsetDateTime toUtc(OffsetDateTime odt) {
        return odt == null ? null : odt.withOffsetSameInstant(ZoneOffset.UTC);
    }

}
