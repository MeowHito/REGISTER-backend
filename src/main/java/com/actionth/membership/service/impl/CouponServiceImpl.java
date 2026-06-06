package com.actionth.membership.service.impl;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.actionth.membership.constant.CouponStatus;
import com.actionth.membership.constant.CouponType;
import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.exception.ValidationException;
import com.actionth.membership.model.Coupon;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventPermission;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.User;
import com.actionth.membership.model.dto.CouponDownloadDTO;
import com.actionth.membership.model.request.CouponDTO;
import com.actionth.membership.repository.CouponRepository;
import com.actionth.membership.repository.EventRepository;
import com.actionth.membership.service.CouponService;
import com.actionth.membership.service.ParticipantService;
import com.actionth.membership.service.UserService;
import com.actionth.membership.utils.CouponCodeGenerator;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CouponServiceImpl implements CouponService {

    private final CouponRepository couponRepository;

    private final ParticipantService participantService;

    private final ModelMapper modelMapper;

    private final EventRepository eventRepository;

    private final UserService userService;

    private final CouponCodeGenerator couponCodeGenerator;

    @Override
    public Page<CouponDTO> findAll(PagingData pagingData) {
        User user = userService.getCurrentUserSession();

        if (user == null) {
            return Page.empty();
        }

        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        if (pagingData != null && pagingData.getSortField() != null && pagingData.getSortDirection() != null) {
            sort = Sort.by(
                    "DESC".equalsIgnoreCase(pagingData.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                    pagingData.getSortField());
        }

        Pageable pageable = (pagingData != null)
                ? PageRequest.of(pagingData.getPage(), pagingData.getSize(), sort)
                : Pageable.unpaged();

        boolean isAdmin = user.getRole() != null && "admin".equalsIgnoreCase(user.getRole().getRoleType());

        Specification<Coupon> spec = (root, query, cb) -> {
            query.distinct(true);

            Join<Coupon, Event> event = root.join("event", JoinType.LEFT);

            List<Predicate> predicates = new ArrayList<>();

            if (!isAdmin && "organizer".equalsIgnoreCase(user.getRole().getRoleType())) {
                Subquery<Integer> permSub = query.subquery(Integer.class);
                Root<EventPermission> epRoot = permSub.from(EventPermission.class);
                permSub.select(epRoot.get("event").get("id"));
                permSub.where(
                        cb.equal(epRoot.get("user").get("id"), user.getId()),
                        cb.isTrue(epRoot.get("active")));

                predicates.add(cb.or(
                        cb.equal(event.get("organizer").get("id"), user.getId()),
                        event.get("id").in(permSub)));
            }

            if (pagingData != null && pagingData.getSearchField() != null && pagingData.getSearchText() != null) {
                Path<String> searchPath = root.get(pagingData.getSearchField());
                predicates.add(cb.like(cb.lower(searchPath.as(String.class)),
                        "%" + pagingData.getSearchText().toLowerCase() + "%"));
            }

            Subquery<Long> subquery = query.subquery(Long.class);
            Root<Coupon> subRoot = subquery.from(Coupon.class);
            subquery.select(cb.greatest(subRoot.get("id").as(Long.class)));
            subquery.groupBy(subRoot.get("bucketName"));

            predicates.add(root.get("id").in(subquery));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Coupon> coupons = couponRepository.findAll(spec, pageable);

        Map<String, Long> usedMap = couponRepository.countUsedCouponsByBucketName()
                .stream()
                .collect(Collectors.toMap(
                        obj -> (String) obj[0],
                        obj -> (Long) obj[1]));

        return coupons.map(coupon -> {
            CouponDTO dto = modelMapper.map(coupon, CouponDTO.class);
            dto.setId(coupon.getUuid());
            dto.setEventId(coupon.getEvent().getUuid());
            dto.setUsedCoupon(usedMap.getOrDefault(coupon.getBucketName(), 0L));
            return dto;
        });
    }

    @Override
    public Page<CouponDTO> findByBucketName(String bucketName, PagingData pagingData) {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");

        if (pagingData != null && pagingData.getSortField() != null && pagingData.getSortDirection() != null) {
            sort = Sort.by(
                    "DESC".equalsIgnoreCase(pagingData.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                    pagingData.getSortField());
        }

        Specification<Coupon> spec = (root, query, cb) -> {
            if (Coupon.class.equals(query.getResultType())) {
                root.fetch("redeemBy", JoinType.LEFT);
            }
            return cb.equal(root.get("bucketName"), bucketName);
        };

        if (pagingData != null && pagingData.getSearchField() != null && pagingData.getSearchText() != null) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get(pagingData.getSearchField())),
                    "%" + pagingData.getSearchText().toLowerCase() + "%"));
        }

        if (pagingData == null) {
            List<CouponDTO> dtos = couponRepository.findAll(spec)
                    .stream()
                    .map(coupon -> {
                        CouponDTO dto = modelMapper.map(coupon, CouponDTO.class);
                        dto.setId(coupon.getUuid());
                        dto.setEventId(coupon.getEvent().getUuid());
                        if (coupon.getRedeemBy() != null) {
                            String first = coupon.getRedeemBy().getFirstName();
                            String last  = coupon.getRedeemBy().getLastName();
                            dto.setRedeemByName(
                                Stream.of(first, last).filter(Objects::nonNull).collect(Collectors.joining(" ")).trim()
                            );
                        }
                        return dto;
                    })
                    .toList();
            return new PageImpl<>(dtos);
        } else {
            Pageable pageable = PageRequest.of(
                    pagingData.getPage(),
                    pagingData.getSize(),
                    sort);

            return couponRepository.findAll(spec, pageable)
                    .map(coupon -> {
                        CouponDTO dto = modelMapper.map(coupon, CouponDTO.class);
                        dto.setId(coupon.getUuid());
                        dto.setEventId(coupon.getEvent().getUuid());
                        if (coupon.getRedeemBy() != null) {
                            String first = coupon.getRedeemBy().getFirstName();
                            String last  = coupon.getRedeemBy().getLastName();
                            dto.setRedeemByName(
                                Stream.of(first, last).filter(Objects::nonNull).collect(Collectors.joining(" ")).trim()
                            );
                        }
                        return dto;
                    });
        }
    }

    @Override
    public Optional<Coupon> findById(Integer id) {
        return couponRepository.findById(id);
    }

    @Override
    public Optional<Coupon> findByUuid(String uuid) {
        return couponRepository.findByUuid(uuid);
    }

    @Override
    public CouponDTO findFirstByBucketName(String bucketName) {
        Coupon coupon = couponRepository.findFirstByBucketName(bucketName)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));
        CouponDTO dto = modelMapper.map(coupon, CouponDTO.class);
        dto.setId(coupon.getUuid());
        dto.setEventId(coupon.getEvent().getUuid());
        return dto;
    }

    @Override
    public Coupon save(Coupon coupon) {
        return couponRepository.save(coupon);
    }

    @Override
    public void deleteByUuid(String uuid) {
        couponRepository.deleteByUuid(uuid);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByUuids(List<String> uuids) {
        List<Coupon> toDelete = couponRepository.findAllByUuidIn(uuids).stream()
                .filter(c -> c.getRedeemBy() == null)
                .toList();

        if (toDelete.isEmpty()) return;

        String bucketName = toDelete.get(0).getBucketName();

        couponRepository.deleteAll(toDelete);

        List<Coupon> availableCoupons = couponRepository.findByBucketName(bucketName);
        int newLimit = availableCoupons.size();
        for (Coupon coupon : availableCoupons) {
            coupon.setLimitCoupon(newLimit);
        }
        couponRepository.saveAll(availableCoupons);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteByBucketName(String bucketName) {
        couponRepository.deleteByBucketNameAndStatusAndRedeemByIsNull(
                bucketName,
                CouponStatus.NEW.getDescription());
    }

    @Override
    public List<Coupon> createCoupon(CouponDTO couponDTO) {

        List<Coupon> coupons = new ArrayList<>();
        Integer limit = couponDTO.getLimitCoupon();

        String couponCode = couponCodeGenerator.generateUniqueCouponCodeChecked();
        String bucketName = UUID.randomUUID().toString();

        Event event = eventRepository.findByUuid(couponDTO.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        if (CouponType.INTERNAL.getDescription().equals(couponDTO.getType())) {
            if (couponDTO.getOldEventId() == null) {
                throw new ValidationException("Old event id is required.");
            }

            String oldEventId = couponDTO.getOldEventId();
            Event oldEvent = eventRepository.findByUuid(oldEventId)
                    .orElseThrow(() -> new ValidationException(
                            "Event not found for UUID: " + oldEventId));
            List<String> runnerIds = participantService.getParticipantByEventId(oldEventId);
            for (String runnerId : runnerIds) {
                Coupon coupon = modelMapper.map(couponDTO, Coupon.class);
                coupon.setEvent(event);
                coupon.setOldEvent(oldEvent);
                coupon.setBucketName(bucketName);
                coupon.setRunnerIdNo(runnerId);
                coupon.setCouponCode(couponCode);
                coupon.setLimitCoupon(runnerIds.size());
                coupon.setStatus("new");
                coupons.add(coupon);
            }
        } else if (CouponType.EXTERNAL.getDescription().equals(couponDTO.getType())) {

            List<String> runnerIds = couponDTO.getRunnerIds();
            for (String runnerId : runnerIds) {
                Coupon coupon = modelMapper.map(couponDTO, Coupon.class);
                coupon.setEvent(event);
                coupon.setBucketName(bucketName);
                coupon.setRunnerIdNo(runnerId);
                coupon.setCouponCode(couponCode);
                coupon.setLimitCoupon(runnerIds.size());
                coupon.setStatus("new");
                coupons.add(coupon);
            }
        } else if (CouponType.REUSABLE.getDescription().equals(couponDTO.getType())) {

            for (Integer i = 0; i < limit; i++) {
                Coupon coupon = modelMapper.map(couponDTO, Coupon.class);
                coupon.setEvent(event);
                coupon.setBucketName(bucketName);
                coupon.setCouponCode(couponCode);
                coupon.setStatus("new");
                coupons.add(coupon);
            }
        } else if (CouponType.NON_REUSABLE.getDescription().equals(couponDTO.getType())) {
            for (Integer i = 0; i < limit; i++) {
                Coupon coupon = modelMapper.map(couponDTO, Coupon.class);
                coupon.setEvent(event);
                coupon.setBucketName(bucketName);
                coupon.setCouponCode(couponCodeGenerator.generateUniqueCouponCodeChecked());
                coupon.setStatus("new");
                coupons.add(coupon);
            }
        } else {
            throw new ValidationException("Not match coupon type.");
        }

        return couponRepository.saveAll(coupons);
    }

    @Override
    public List<Coupon> updateCoupon(CouponDTO couponDTO) {

        List<Coupon> coupons = couponRepository.findByBucketName(couponDTO.getBucketName());
        if (coupons.isEmpty()) {
            throw new ValidationException("No coupon found.");
        }

        if (!coupons.get(0).getType().equals(couponDTO.getType())) {
            throw new ValidationException("Mismatch coupon type.");
        }

        if (isExternalOrInternalType(couponDTO.getType())) {
            updateExternalOrInternalCoupons(coupons, couponDTO);
        } else if (isReusableOrNonReusableType(couponDTO.getType())) {
            updateReusableOrNonReusableCoupons(coupons, couponDTO);
        } else {
            throw new ValidationException("Not match coupon type.");
        }

        return couponRepository.saveAll(coupons);
    }

    @Override
    public List<Coupon> updateCouponStatus(CouponDTO couponDTO) {

        List<Coupon> coupons = couponRepository.findByBucketName(couponDTO.getBucketName());
        if (coupons.isEmpty()) {
            throw new ValidationException("No coupon found.");
        }

        coupons.forEach(coupon -> coupon.setStatus(couponDTO.getStatus()));

        return couponRepository.saveAll(coupons);
    }

    private boolean isExternalOrInternalType(String type) {
        return CouponType.EXTERNAL.getDescription().equals(type) ||
                CouponType.INTERNAL.getDescription().equals(type);
    }

    private boolean isReusableOrNonReusableType(String type) {
        return CouponType.REUSABLE.getDescription().equals(type) ||
                CouponType.NON_REUSABLE.getDescription().equals(type);
    }

    private void updateExternalOrInternalCoupons(List<Coupon> coupons, CouponDTO couponDTO) {
        for (Coupon coupon : coupons) {
            if (!CouponStatus.REDEEMED.getDescription().equals(coupon.getStatus())) {
                updateCommonFields(coupon, couponDTO);
            }
            coupon.setCouponName(couponDTO.getCouponName());
            coupon.setOldEventName(couponDTO.getOldEventName());
        }
    }

    private void updateReusableOrNonReusableCoupons(List<Coupon> coupons, CouponDTO couponDTO) {
        for (Coupon coupon : coupons) {
            if (!CouponStatus.REDEEMED.getDescription().equals(coupon.getStatus())) {
                updateCommonFields(coupon, couponDTO);
            }
            coupon.setCouponName(couponDTO.getCouponName());
            coupon.setLimitCoupon(couponDTO.getLimitCoupon());
        }

        Integer diffSize = couponDTO.getLimitCoupon() - coupons.size();
        if (diffSize > 0) {
            Event event = eventRepository.findByUuid(couponDTO.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
            addNewCoupons(coupons, couponDTO, diffSize, event);
        } else {
            removeExistingCoupons(coupons, diffSize);
        }
    }

    private void updateCommonFields(Coupon coupon, CouponDTO couponDTO) {
        coupon.setDeductionPercentage(couponDTO.getDeductionPercentage());
        coupon.setStartTime(couponDTO.getStartTime());
        coupon.setExpiryTime(couponDTO.getExpiryTime());
        coupon.setActive(couponDTO.getActive());
        coupon.setStatus(couponDTO.getStatus());
    }

    private void addNewCoupons(List<Coupon> coupons, CouponDTO couponDTO, Integer diffSize, Event event) {
        String type = couponDTO.getType();
        for (Integer i = 0; i < diffSize; i++) {
            Coupon coupon = modelMapper.map(couponDTO, Coupon.class);
            coupon.setBucketName(coupons.get(0).getBucketName());
            coupon.setEvent(event);

            if (CouponType.REUSABLE.getDescription().equals(type)) {
                coupon.setCouponCode(coupons.get(0).getCouponCode());
            } else if (CouponType.NON_REUSABLE.getDescription().equals(type)) {
                coupon.setCouponCode(couponCodeGenerator.generateUniqueCouponCodeChecked());
            }
            coupons.add(coupon);
        }
    }

    private void removeExistingCoupons(List<Coupon> coupons, Integer diffSize) {
        diffSize = Math.abs(diffSize);
        if (diffSize > coupons.size()) {
            throw new IllegalArgumentException("Invalid diffSize: " + diffSize);
        }

        List<Coupon> nonRedeemedCoupons = coupons.stream()
                .filter(coupon -> !CouponStatus.REDEEMED.getDescription().equals(coupon.getStatus()))
                .toList();

        if (diffSize > nonRedeemedCoupons.size()) {
            throw new IllegalArgumentException("Not enough non-redeemed coupons to remove.");
        }

        List<Coupon> toRemove = new ArrayList<>();

        for (int i = nonRedeemedCoupons.size() - 1; i >= nonRedeemedCoupons.size() - diffSize; i--) {
            toRemove.add(nonRedeemedCoupons.get(i));
        }
        coupons.removeAll(toRemove);
        couponRepository.deleteAll(toRemove);
    }

    @Override
    public List<CouponDTO> getGroupedCouponsByEventIds(List<String> eventIdsOrLinks) {
        List<String> eventIds = eventIdsOrLinks.stream()
                .map(key -> eventRepository.findByLinkOrUuid(key)
                        .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + key))
                        .getUuid())
                .distinct()
                .toList();

        List<String> types = List.of(
                CouponType.INTERNAL.getDescription(),
                CouponType.EXTERNAL.getDescription(),
                CouponType.REUSABLE.getDescription());

        String status = CouponStatus.APPROVED.getDescription();
        List<Coupon> coupons = couponRepository.findGroupedByBucketNameByEventIdsAndTypeAndStatus(
                eventIds, types, status);

        return coupons.stream().map(coupon -> {
            CouponDTO dto = modelMapper.map(coupon, CouponDTO.class);
            dto.setId(coupon.getUuid());
            dto.setEventId(coupon.getEvent().getUuid());
            return dto;
        }).toList();
    }

    @Override
    public Map<String, Object> validateCoupon(String couponCode, String eventId, List<String> idNos, String orderUuid) {
        Map<String, Object> response = new HashMap<>();
        Set<String> idNoSet = new HashSet<>(idNos);
        Map<String, Boolean> idNoValidity = initializeIdNoValidity(idNos);
        Event event = eventRepository.findByLinkOrUuid(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found for UUID: " + eventId));

        List<Coupon> coupons = couponRepository.findAllByCouponCodeAndEventIdAndStatusAndRedeemByIsNullOrOrderUuid(
                couponCode,
                event.getUuid(),
                CouponStatus.APPROVED.getDescription(),
                orderUuid);

        if (coupons == null || coupons.isEmpty()) {
            response.put("status", "error");
            response.put("message", "Coupon not found or already redeemed.");
            response.put("idNo", idNoValidity);
            return response;
        }

        OffsetDateTime now = OffsetDateTime.now();
        int index = 0;

        for (Coupon coupon : coupons) {
            if (!isCouponActive(coupon, now)) {
                continue;
            }

            String type = coupon.getType();
            if (isInternalOrExternal(type)) {
                String couponIdNo = coupon.getRunnerIdNo();
                if (couponIdNo != null && idNoSet.contains(couponIdNo)) {
                    idNoValidity.put(couponIdNo, true);
                }
            } else {
                if (index < idNos.size()) {
                    idNoValidity.put(idNos.get(index), true);
                    index++;
                }
            }
        }

        Coupon firstValidCoupon = coupons.stream()
                .filter(c -> isCouponActive(c, now))
                .findFirst()
                .orElse(null);

        if (firstValidCoupon != null) {
            response.put("deductionPercentage", firstValidCoupon.getDeductionPercentage());
            response.put("type", firstValidCoupon.getType());
        }

        response.put("status", "success");
        response.put("message", "Validated coupon successfully.");
        response.put("idNo", idNoValidity);
        return response;
    }

    private Map<String, Boolean> initializeIdNoValidity(List<String> idNos) {
        Map<String, Boolean> map = new HashMap<>();
        if (idNos != null) {
            for (String id : idNos) {
                map.put(id, false);
            }
        }
        return map;
    }

    private boolean isCouponActive(Coupon coupon, OffsetDateTime now) {
        return (coupon.getStartTime() == null || !coupon.getStartTime().isAfter(now)) &&
                (coupon.getExpiryTime() == null || !coupon.getExpiryTime().isBefore(now));
    }

    private boolean isInternalOrExternal(String type) {
        return CouponType.INTERNAL.getDescription().equalsIgnoreCase(type) ||
                CouponType.EXTERNAL.getDescription().equalsIgnoreCase(type);
    }

    public Map<String, Object> getCouponDetailsDownload(String bucketName) {
        String[] columns = { "ลำดับ", "ชื่ออีเว้นท์", "ชื่อคูปอง", "รหัสคูปอง", "เปอร์เซ็นต์ส่วนลด",
                "วันที่เริ่มต้น", "วันที่หมดอายุ", "ใช้โดย", "วันที่ใช้", "สถานะ" };

        List<Coupon> coupons = couponRepository.findByBucketName(bucketName);
        if (coupons.isEmpty()) {
            throw new ResourceNotFoundException("No coupons found for bucket: " + bucketName);
        }

        List<CouponDownloadDTO> dtos = coupons.stream()
                .map(c -> {
                    CouponDownloadDTO dto = modelMapper.map(c, CouponDownloadDTO.class);
                    dto.setEventName(c.getEvent().getName());
                    return dto;
                })
                .toList();

        List<List<Object>> sheetData = new ArrayList<>();
        for (int i = 0; i < dtos.size(); i++) {
            CouponDownloadDTO dto = dtos.get(i);
            Coupon c = coupons.get(i);

            String redeemByName = null;
            if (c.getRedeemBy() != null) {
                String first = c.getRedeemBy().getFirstName();
                String last  = c.getRedeemBy().getLastName();
                redeemByName = ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
                if (redeemByName.isEmpty()) redeemByName = null;
            }

            List<Object> row = new ArrayList<>();
            row.add(i + 1);
            row.add(dto.getEventName());
            row.add(dto.getCouponName());
            row.add(dto.getCouponCode());
            row.add(dto.getDeductionPercentage() != null ? dto.getDeductionPercentage() + "%" : null);
            row.add(formatDate(dto.getStartTime()));
            row.add(formatDate(dto.getExpiryTime()));
            row.add(redeemByName);
            row.add(formatDate(dto.getRedeemTime()));
            row.add(mapStatus(dto));
            sheetData.add(row);
        }

        Map<String, Object> sheet = new HashMap<>();
        sheet.put("sheetName", "คูปอง");
        sheet.put("columns", columns);
        sheet.put("datas", sheetData);
        sheet.put("couponName", dtos.get(0).getCouponName());

        return sheet;
    }

    private String formatDate(OffsetDateTime dateTime) {
        return dateTime != null ? dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : null;
    }

    private String mapStatus(CouponDownloadDTO dto) {
        if (dto.getRedeemTime() != null) {
            return "ใช้ไปแล้ว";
        } else if (dto.getExpiryTime() != null && dto.getExpiryTime().isBefore(OffsetDateTime.now())) {
            return "หมดอายุ";
        } else {
            return "ยังไม่ได้ใช้";
        }
    }

}
