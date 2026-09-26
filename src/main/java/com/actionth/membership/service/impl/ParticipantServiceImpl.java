package com.actionth.membership.service.impl;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.persistence.criteria.JoinType;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.exception.ParticipantOverQuotaException;
import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.EventType;
import com.actionth.membership.model.EventSelectionField;
import com.actionth.membership.model.OrderAddOn;
import com.actionth.membership.model.OrderDetail;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.Pricing;
import com.actionth.membership.model.ShirtSize;
import com.actionth.membership.model.ShirtType;
import com.actionth.membership.model.User;
import com.actionth.membership.model.dto.EventSelectionFieldDto;
import com.actionth.membership.model.dto.EventSelectionOptionDto;
import com.actionth.membership.model.dto.ParticipantDTO;
import com.actionth.membership.model.dto.ParticipantDetailDto;
import com.actionth.membership.model.dto.ParticipantDownloadDTO;
import com.actionth.membership.model.dto.ParticipantEditLogDto;
import com.actionth.membership.model.dto.ParticipantViewDto;
import com.actionth.membership.model.dto.SelectionAnswerDto;
import com.actionth.membership.model.dto.SelectionAnswerDto.SelectionValueDto;
import com.actionth.membership.model.dto.ShirtSizeDto;
import com.actionth.membership.model.request.ParticipantDTORequest;
import com.actionth.membership.model.request.ParticipantUploadDTORequest;
import com.actionth.membership.model.request.ParticipantUploadDTORequest.ParticipantUploadDTO;
import com.actionth.membership.repository.EventPermissionRepository;
import com.actionth.membership.repository.EventTypeRepository;
import com.actionth.membership.repository.OrderDetailRepository;
import com.actionth.membership.repository.ShirtSizeRepository;
import com.actionth.membership.repository.ShirtTypeRepository;
import com.actionth.membership.repository.UserRepository;
import com.actionth.membership.service.EventTypeService;
import com.actionth.membership.service.ParticipantService;
import com.actionth.membership.utils.AddOnUtils;
import com.actionth.membership.utils.AgeGroupUtils;
import com.actionth.membership.utils.ContextUtils;
import com.actionth.membership.utils.ExportDateTimeUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantServiceImpl implements ParticipantService {

    private final OrderDetailRepository orderDetailRepository;
    private final EventPermissionRepository eventPermissionRepository;
    private final EventTypeService eventTypeService;
    private final ShirtSizeRepository shirtSizeRepository;
    private final ShirtTypeRepository shirtTypeRepository;
    private final EventTypeRepository eventTypeRepository;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final ContextUtils contextUtils;
    private final ModelMapper modelMapper;

    @Override
    public List<String> getParticipantByEventId(String eventId) {
        return orderDetailRepository.getParticipantByEventId(eventId);
    }

    @Override
    public Page<ParticipantDTO> findAll(String eventId, PagingData pagingData) {
        Sort.Direction direction = "DESC".equalsIgnoreCase(pagingData.getSortDirection())
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        String sortField = pagingData.getSortField();

        Sort sort;
        if (sortField == null || sortField.isBlank()) {
            sort = Sort.by(Sort.Direction.DESC, "id");
        } else if ("orderNo".equals(sortField)) {
            sort = Sort.by(direction, "order.orderNo");
        } else if ("registerDate".equals(sortField)) {
            sort = Sort.by(direction, "createdTime");
        } else {
            sort = Sort.by(direction, sortField);
        }

        Pageable pageable = PageRequest.of(pagingData.getPage(), pagingData.getSize(), sort);

        Specification<OrderDetail> spec = (root, query, cb) -> {
            if (OrderDetail.class.equals(query.getResultType())) {
                root.fetch("order", JoinType.LEFT);
                root.fetch("eventType", JoinType.LEFT);
                root.fetch("shirtSize", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.equal(root.get("active"), true);
        };

        spec = spec.and((root, query, cb) -> cb.equal(root.get("eventType").get("uuid"), eventId)); // order.paymentStatus
        spec = spec.and((root, query, cb) -> cb.equal(root.get("order").get("paymentStatus"), "SUCCESS"));

        if (pagingData.getSearchField() != null && pagingData.getSearchText() != null
                && !pagingData.getSearchField().isBlank()
                && !pagingData.getSearchText().isBlank()) {

            String field = pagingData.getSearchField();
            String text = "%" + pagingData.getSearchText().trim().toLowerCase() + "%";

            spec = spec.and((root, query, cb) -> switch (field) {
                case "firstName" -> cb.like(cb.lower(root.get("firstName")), text);
                case "lastName" -> cb.like(cb.lower(root.get("lastName")), text);
                case "gender" -> cb.like(cb.lower(root.get("gender")), text);
                case "nationality" -> cb.like(cb.lower(root.get("nationality")), text);
                case "orderNo" -> cb.like(cb.lower(root.get("order").get("orderNo")), text);
                case "bibNo" -> cb.like(cb.lower(root.get("bibNo")), text);
                case "teamClub" -> cb.like(cb.lower(root.get("teamClub")), text);
                case "shirtSizeName" -> cb.like(cb.lower(root.get("shirtSize").get("name")), text);
                default -> cb.conjunction();
            });
        }

        return orderDetailRepository.findAll(spec, pageable).map(this::toDTO);
    }

    @Override
    public List<ParticipantViewDto> checkParticipantName(String eventId, String name) {
        return orderDetailRepository.findByEventAndName(eventId, name).stream().map(this::toViewsDto)
                .toList();
    }

    private ParticipantViewDto toViewsDto(OrderDetail participantDTO) {
        return modelMapper.map(participantDTO, ParticipantViewDto.class);
    }

    private static final int SEARCH_LIMIT = 100;

    @Transactional(readOnly = true)
    @Override
    public Page<ParticipantViewDto> checkParticipant(String eventId, String name, int page, int size) {
        String eventKey = trimToNull(eventId);
        String q = trimToNull(name);

        if (eventKey == null || q == null)
            return Page.empty();

        boolean looksLikeExact = q.matches("^\\d+$") || q.matches("^[0-9A-Za-z\\-]{3,}$")
                || q.matches("^[0-9a-fA-F\\-]{36}$");
        if (!looksLikeExact && q.length() < 2)
            return Page.empty();

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), SEARCH_LIMIT);

        Sort sort = Sort.by(
                Sort.Order.asc("bibNo").nullsLast(),
                Sort.Order.desc("createdTime").nullsLast(),
                Sort.Order.asc("uuid"));

        Pageable pageable = PageRequest.of(safePage, safeSize, sort);

        String normalized = q.replaceAll("\\s+", " ").trim();
        String[] parts = normalized.split("\\s+");

        if (parts.length >= 2) {
            String p1 = parts[0];
            String p2 = String.join(" ", java.util.Arrays.copyOfRange(parts, 1, parts.length));

            return orderDetailRepository
                    .searchParticipantsFullName(eventKey, p1, p2, normalized, normalized, pageable)
                    .map(this::toViewDto);
        }

        Page<OrderDetail> rows = orderDetailRepository.searchParticipants(eventKey, normalized, normalized, pageable);
        return rows.map(this::toViewDto);
    }

    @Transactional(readOnly = true)
    @Override
    public ParticipantDetailDto getParticipantDetail(String participantId) {
        OrderDetail od = orderDetailRepository.findByUuid(participantId)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));

        // Check if logged-in user can see unmasked data
        boolean canViewFull = false;
        Integer currentUserId = contextUtils.resolveUserIdFromCookie();
        log.info("[getParticipantDetail] participantId={}, currentUserId={}", participantId, currentUserId);
        if (currentUserId != null) {
            canViewFull = userRepository.findById(currentUserId)
                    .map(user -> {
                        // 1. Admin can see all
                        if (user.getRole() != null && "admin".equalsIgnoreCase(user.getRole().getRoleType())) {
                            log.info("[getParticipantDetail] admin access");
                            return true;
                        }
                        // 2. Event organizer (owner)
                        if (od.getEventType() != null && od.getEventType().getEvent() != null
                                && od.getEventType().getEvent().getOrganizer() != null
                                && currentUserId.equals(od.getEventType().getEvent().getOrganizer().getId())) {
                            log.info("[getParticipantDetail] organizer access");
                            return true;
                        }
                        // 3. Collaborator with canRead permission on this event
                        if (od.getEventType() != null && od.getEventType().getEvent() != null) {
                            String eventUuid = od.getEventType().getEvent().getUuid();
                            boolean hasPermission = eventPermissionRepository
                                    .findActiveByEventUuidAndUserUuid(eventUuid, user.getUuid())
                                    .map(ep -> Boolean.TRUE.equals(ep.getCanRead()))
                                    .orElse(false);
                            if (hasPermission) {
                                log.info("[getParticipantDetail] collaborator access");
                                return true;
                            }
                        }
                        // 4. Same idNo (own registration)
                        if (od.getIdNo() != null && od.getIdNo().equals(user.getIdNo())) {
                            log.info("[getParticipantDetail] owner (same idNo) access");
                            return true;
                        }
                        return false;
                    })
                    .orElse(false);
        }

        ParticipantDetailDto dto = new ParticipantDetailDto();
        dto.setParticipantId(od.getUuid());
        dto.setBibNo(od.getBibNo());

        // Order
        if (od.getOrder() != null) {
            dto.setOrderNo(od.getOrder().getOrderNo());
        }
        dto.setRegisterDate(od.getCreatedTime());

        // Personal
        dto.setFirstName(od.getFirstName());
        dto.setLastName(od.getLastName());
        dto.setFirstNameEn(od.getFirstNameEn());
        dto.setLastNameEn(od.getLastNameEn());
        dto.setGender(od.getGender());
        dto.setBirthDate(od.getBirthDate());
        dto.setAge(od.getAge());
        dto.setNationality(od.getNationality());
        dto.setIdNo(canViewFull ? od.getIdNo() : maskGeneric(od.getIdNo()));
        dto.setPictureUrl(od.getPictureUrl());
        dto.setPrefixPath(od.getPrefixPath());

        // Contact (masked for public view, unmasked for authorized users)
        dto.setEmail(canViewFull ? od.getEmail() : maskEmail(od.getEmail()));
        dto.setPhone(canViewFull ? od.getPhone() : maskPhone(od.getPhone()));
        dto.setAddress(od.getAddress());
        dto.setProvince(od.getProvince());
        dto.setAmphoe(od.getAmphoe());
        dto.setDistrict(od.getDistrict());
        dto.setZipcode(od.getZipcode());

        // Health & Emergency
        dto.setBloodType(od.getBloodType());
        dto.setHealthIssues(od.getHealthIssues());
        dto.setEmergencyContact(od.getEmergencyContact());
        dto.setEmergencyRelation(od.getEmergencyRelation());
        dto.setEmergencyPhone(canViewFull ? od.getEmergencyPhone() : maskPhone(od.getEmergencyPhone()));

        // Event
        dto.setEventTypeName(od.getEventType() != null ? od.getEventType().getName() : null);
        dto.setAgeGroupName(AgeGroupUtils.resolveAgeGroupCode(od));
        dto.setTeamClub(od.getTeamClub());

        // Shirt & Delivery
        dto.setReceiveShirt(od.getReceiveShirt());
        dto.setShirtTypeName(od.getShirtType() != null ? od.getShirtType().getName() : null);
        dto.setShirtSizeName(od.getShirtSize() != null ? od.getShirtSize().getName() : null);
        dto.setDeliveryMethod(od.getDeliveryMethod());
        dto.setShippingAddress(od.getShippingAddress());
        dto.setShippingProvince(od.getShippingProvince());
        dto.setShippingAmphoe(od.getShippingAmphoe());
        dto.setShippingDistrict(od.getShippingDistrict());
        dto.setShippingZipcode(od.getShippingZipcode());

        // Dynamic Q&A
        dto.setSelectionAnswers(od.getSelectionAnswers());

        dto.setMasked(!canViewFull);

        return dto;
    }

    private static String maskGeneric(String value) {
        if (value == null || value.isBlank()) return null;
        String v = value.trim();
        if (v.length() <= 4) return "***";
        return v.substring(0, 2) + "*".repeat(v.length() - 4) + v.substring(v.length() - 2);
    }

    private static String maskEmail(String email) {
        if (email == null || email.isBlank()) return null;
        int at = email.indexOf('@');
        if (at <= 0) return "***";
        String local = email.substring(0, at);
        String domain = email.substring(at);
        if (local.length() <= 4) return local.charAt(0) + "***" + domain;
        return local.substring(0, 2) + "*".repeat(local.length() - 4) + local.substring(local.length() - 2) + domain;
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String d = phone.replaceAll("\\D", "");
        if (d.length() <= 4) return "***";
        return d.substring(0, 2) + "*".repeat(d.length() - 4) + d.substring(d.length() - 2);
    }

    private static String trimToNull(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private ParticipantViewDto toViewDto(OrderDetail od) {
        ParticipantViewDto dto = new ParticipantViewDto();

        dto.setParticipantId(od.getUuid());
        dto.setBibNo(od.getBibNo());
        dto.setTeamClub(od.getTeamClub());
        dto.setFirstName(od.getFirstName());
        dto.setLastName(od.getLastName());
        dto.setFirstNameEn(od.getFirstNameEn());
        dto.setLastNameEn(od.getLastNameEn());
        dto.setGender(od.getGender());
        dto.setBirthDate(od.getBirthDate());
        dto.setAgeGroupName(AgeGroupUtils.resolveAgeGroupCode(od));
        dto.setNationality(od.getNationality());
        dto.setDeliveryMethod(od.getDeliveryMethod());

        if (od.getOrder() != null) {
            dto.setOrderNo(od.getOrder().getOrderNo());
        }

        if (od.getEventType() != null) {
            dto.setEventType(od.getEventType().getName());
        }

        if (od.getShirtSize() != null) {
            dto.setShirtSize(od.getShirtSize().getName());
        }

        if (od.getCreatedTime() != null) {
            dto.setRegisterDate(od.getCreatedTime());
        }

        return dto;
    }

    private EventSelectionFieldDto toSelectionFieldDto(EventSelectionField field) {
        return EventSelectionFieldDto.builder()
                .id(field.getUuid())
                .title(field.getTitle())
                .titleEn(field.getTitleEn())
                .type(field.getType())
                .required(field.isRequired())
                .options(field.getOptions().stream()
                        .map(opt -> EventSelectionOptionDto.builder()
                                .id(opt.getUuid())
                                .value(opt.getValue())
                                .valueEn(opt.getValueEn())
                                .inputType(opt.getInputType())
                                .position(opt.getPosition())
                                .build())
                        .toList())
                .build();
    }

    private ParticipantDTO toDTO(OrderDetail participant) {
        ParticipantDTO dto = modelMapper.map(participant, ParticipantDTO.class);
        dto.setId(participant.getUuid());
        dto.setRegisterDate(participant.getCreatedTime());
        dto.setTeamClub(participant.getTeamClub());

        if (participant.getShirtSize() != null) {
            dto.setShirtSizeId(participant.getShirtSize().getUuid());
            dto.setShirtSizeName(participant.getShirtSize().getName());
        }
        if (participant.getShirtType() != null) {
            dto.setShirtTypeId(participant.getShirtType().getUuid());
            dto.setShirtTypeName(participant.getShirtType().getName());
        }

        if (participant.getEventType() != null) {
            dto.setEventTypeId(participant.getEventType().getUuid());
            dto.setEventTypeName(participant.getEventType().getName());
        }

        if (participant.getOrder() != null) {
            dto.setOrderNo(participant.getOrder().getOrderNo());
        }

        dto.setEmail(participant.getEmail());
        dto.setPhone(participant.getPhone());
        dto.setProvince(participant.getProvince());
        dto.setAddress(participant.getAddress());
        dto.setAmphoe(participant.getAmphoe());
        dto.setDistrict(participant.getDistrict());
        dto.setZipcode(participant.getZipcode());
        dto.setDeliveryMethod(participant.getDeliveryMethod());
        dto.setShippingAddress(participant.getShippingAddress());
        dto.setShippingProvince(participant.getShippingProvince());
        dto.setShippingAmphoe(participant.getShippingAmphoe());
        dto.setShippingDistrict(participant.getShippingDistrict());
        dto.setShippingZipcode(participant.getShippingZipcode());
        dto.setBloodType(participant.getBloodType());
        dto.setHealthIssues(participant.getHealthIssues());
        dto.setEmergencyContact(participant.getEmergencyContact());
        dto.setEmergencyRelation(participant.getEmergencyRelation());
        dto.setEmergencyPhone(participant.getEmergencyPhone());
        dto.setManualEditedTime(participant.getManualEditedTime());
        dto.setManualEditedBy(participant.getManualEditedBy());
        dto.setAddOns(AddOnUtils.forParticipant(participant).stream()
                .map(AddOnUtils::toDto)
                .toList());

        return dto;
    }

    @Transactional(readOnly = true)
    @Override
    public ParticipantDTO findParticipantByUuid(String uuid) {
        OrderDetail participant = orderDetailRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        ParticipantDTO dto = toDTO(participant);
        dto.setManualEdits(readEditLog(participant.getManualEditLog()));

        // Populate selectionAnswers and combined selectionFields for the detail view
        dto.setSelectionAnswers(participant.getSelectionAnswers());
        if (participant.getEventType() != null) {
            List<EventSelectionFieldDto> allFields = new ArrayList<>();
            if (participant.getEventType().getEvent() != null) {
                participant.getEventType().getEvent().getSelectionFields().stream()
                        .map(this::toSelectionFieldDto)
                        .forEach(allFields::add);
            }
            participant.getEventType().getSelectionFields().stream()
                    .map(this::toSelectionFieldDto)
                    .forEach(allFields::add);
            dto.setSelectionFields(allFields);
        }

        return dto;
    }

    @Override
    public List<Map<String, Object>> getAllParticipantDownload(Integer eventId) {
        String[] baseColumns = { "id", "หมายเลขออเดอร์", "ชื่อ", "นามสกุล", "ชื่อ (ภาษาอังกฤษ)", "นามสกุล (ภาษาอังกฤษ)",
                "บัตรประชาชน", "เพศ", "วันเกิด", "สัญชาติ",
                "อีเมล", "เบอร์โทรศัพท์", "ที่อยู่", "จังหวัด", "อำเภอ", "ตำบล", "ไปรษณีย์",
                "หมู่เลือด", "ปัญหาสุขภาพ", "ผู้ติดต่อฉุกเฉิน", "เบอร์โทรศัพท์ผู้ติดต่อฉุกเฉิน",
                "ไซส์เสื้อ", "กลุ่มอายุ", "สมัครวันที่", "ชื่อทีม", "bib", "ข้อตกลงการรับสมัครและกติกา",
                "รับเสื้อและอุปกรณ์", "ที่อยู่จัดส่ง", "รับ Bib และอุปกรณ์แข่งขัน", "ผู้มารับ", "เบอร์ติดต่อ",
                "ผู้รับแทน" };

        List<Map<String, Object>> results = eventTypeService.findIdAndNameByEventId(eventId);
        List<Map<String, Object>> formatData = new ArrayList<>();
        for (Map<String, Object> result : results) {
            List<OrderDetail> participants = orderDetailRepository
                    .getParticipantByEventTypeId((Integer) result.get("id"));

            LinkedHashSet<String> questionTitles = new LinkedHashSet<>();
            for (OrderDetail participant : participants) {
                if (participant.getSelectionAnswers() != null) {
                    for (var answer : participant.getSelectionAnswers()) {
                        if (answer.getQuestion() != null && answer.getQuestion().getValue() != null
                                && !answer.getQuestion().getValue().isEmpty()) {
                            questionTitles.add(answer.getQuestion().getValue());
                        }
                    }
                }
            }
            List<String> questionList = new ArrayList<>(questionTitles);

            // One column per add-on (quantity), plus a note column when the
            // organizer gave that add-on a note field — same idea as questions.
            LinkedHashMap<String, String> addOnColumns = new LinkedHashMap<>();
            LinkedHashMap<String, String> addOnNoteColumns = new LinkedHashMap<>();
            for (OrderDetail participant : participants) {
                for (OrderAddOn oa : AddOnUtils.forParticipant(participant)) {
                    String key = addOnKey(oa);
                    addOnColumns.putIfAbsent(key, "สินค้าเสริม: " + Objects.toString(oa.getName(), "-"));
                    String noteLabel = oa.getAddOn() != null ? oa.getAddOn().getNoteLabel() : null;
                    if ((noteLabel != null && !noteLabel.isBlank())
                            || (oa.getNote() != null && !oa.getNote().isBlank())) {
                        addOnNoteColumns.putIfAbsent(key, "สินค้าเสริม: " + Objects.toString(oa.getName(), "-")
                                + " - " + (noteLabel != null && !noteLabel.isBlank() ? noteLabel : "หมายเหตุ"));
                    }
                }
            }
            List<String> addOnKeys = new ArrayList<>(addOnColumns.keySet());

            List<String> allColumns = new ArrayList<>(Arrays.asList(baseColumns));
            allColumns.addAll(questionList);
            for (String key : addOnKeys) {
                allColumns.add(addOnColumns.get(key));
                if (addOnNoteColumns.containsKey(key)) {
                    allColumns.add(addOnNoteColumns.get(key));
                }
            }

            Map<String, Object> allParticipant = new HashMap<>();
            allParticipant.put("sheetName", result.get("name"));
            allParticipant.put("columns", allColumns.toArray(new String[0]));
            allParticipant.put("preHeader", List.of("ชื่ออีเว้นท์", result.get("name")));

            List<List<String>> sheetData = new ArrayList<>();
            for (OrderDetail participant : participants) {
                ParticipantDownloadDTO dto = toDownloadDTO(participant);

                List<String> addressParts = Arrays.asList(
                        dto.getShippingAddress(),
                        dto.getShippingDistrict(),
                        dto.getShippingAmphoe(),
                        dto.getShippingProvince(),
                        dto.getShippingZipcode());

                List<String> row = new ArrayList<>();
                row.add(dto.getId());
                row.add(dto.getOrderNo());
                row.add(dto.getFirstName());
                row.add(dto.getLastName());
                row.add(dto.getFirstNameEn());
                row.add(dto.getLastNameEn());
                row.add(dto.getIdNo());
                row.add(dto.getGender());
                row.add(dto.getBirthDate() != null ? dto.getBirthDate().toString() : "");
                row.add(dto.getNationality());
                row.add(dto.getEmail());
                row.add(dto.getPhone());
                row.add(dto.getAddress());
                row.add(dto.getProvince());
                row.add(dto.getAmphoe());
                row.add(dto.getDistrict());
                row.add(dto.getZipcode());
                row.add(dto.getBloodType());
                row.add(dto.getHealthIssues());
                row.add(dto.getEmergencyContact());
                row.add(dto.getEmergencyPhone());
                row.add(dto.getShirtSize() != null ? dto.getShirtSize().getName() : "");
                row.add(AgeGroupUtils.resolveAgeGroup(participant));
                row.add(dto.getRegisterDate() != null ? dto.getRegisterDate().toString() : "");
                row.add(dto.getTeamClub());
                row.add(dto.getBibNo());
                row.add(Boolean.TRUE.equals(dto.getRules()) ? "ยอมรับ" : "ไม่ยอมรับ");
                row.add(dto.getDeliveryMethod());
                row.add(addressParts.stream()
                        .filter(Objects::nonNull)
                        .collect(Collectors.joining(", ")));
                row.add("");
                row.add("");
                row.add("");
                row.add("");

                Map<String, List<String>> answerMap = buildAnswerMap(participant);

                for (String q : questionList) {
                    List<String> values = answerMap.get(q);
                    row.add(values != null ? String.join(", ", values) : "");
                }

                Map<String, List<OrderAddOn>> addOnMap = AddOnUtils.forParticipant(participant).stream()
                        .collect(Collectors.groupingBy(this::addOnKey, LinkedHashMap::new, Collectors.toList()));
                for (String key : addOnKeys) {
                    List<OrderAddOn> bought = addOnMap.getOrDefault(key, List.of());
                    int qty = bought.stream().mapToInt(oa -> oa.getQty() != null ? oa.getQty() : 0).sum();
                    row.add(bought.isEmpty() ? "" : String.valueOf(qty));
                    if (addOnNoteColumns.containsKey(key)) {
                        row.add(bought.stream()
                                .map(OrderAddOn::getNote)
                                .filter(n -> n != null && !n.isBlank())
                                .collect(Collectors.joining(", ")));
                    }
                }

                sheetData.add(row);
            }

            allParticipant.put("datas", sheetData);
            formatData.add(allParticipant);
        }
        return formatData;
    }

    /** Groups by the organizer's add-on; falls back to the snapshotted name if it was deleted. */
    private String addOnKey(OrderAddOn oa) {
        return oa.getAddOn() != null ? oa.getAddOn().getUuid() : "name:" + Objects.toString(oa.getName(), "");
    }

    private Map<String, List<String>> buildAnswerMap(OrderDetail participant) {
        Map<String, List<String>> answerMap = new LinkedHashMap<>();
        if (participant.getSelectionAnswers() == null) {
            return answerMap;
        }
        for (var answer : participant.getSelectionAnswers()) {
            String question = answer.getQuestion() != null ? answer.getQuestion().getValue() : "";
            String value = extractAnswerValue(answer.getValue());
            if (!question.isEmpty() && !value.isEmpty()) {
                answerMap.computeIfAbsent(question, k -> new ArrayList<>()).add(value);
            }
        }
        return answerMap;
    }

    private ParticipantDownloadDTO toDownloadDTO(OrderDetail participant) {
        ParticipantDownloadDTO dto = modelMapper.map(participant, ParticipantDownloadDTO.class);
        dto.setId(participant.getUuid());
        dto.setRegisterDate(participant.getCreatedTime());

        if (participant.getShirtSize() != null) {
            dto.setShirtSize(modelMapper.map(participant.getShirtSize(), ShirtSizeDto.class));
        }

        if (participant.getOrder() != null) {
            dto.setOrderNo(participant.getOrder().getOrderNo());
        }

        return dto;
    }

    private String extractAnswerValue(Object rawValue) {
        if (rawValue instanceof List<?> list) {
            return list.stream()
                    .map(this::extractSingleValue)
                    .filter(v -> !v.isEmpty())
                    .collect(Collectors.joining(", "));
        }
        return extractSingleValue(rawValue);
    }

    private String extractSingleValue(Object rawValue) {
        String value = "";
        String freeText = null;
        boolean isFreeText = false;

        if (rawValue instanceof SelectionValueDto dto) {
            value = dto.getValue() != null ? dto.getValue() : "";
            isFreeText = "FREE_TEXT".equals(dto.getInputType());
            freeText = dto.getFreeTextValue();
        } else if (rawValue instanceof Map<?, ?> map) {
            Object valueObj = map.get("value");
            value = valueObj != null ? valueObj.toString() : "";
            isFreeText = "FREE_TEXT".equals(map.get("inputType"));
            Object freeTextObj = map.get("freeTextValue");
            freeText = freeTextObj != null ? freeTextObj.toString() : null;
        } else if (rawValue != null) {
            return rawValue.toString();
        }

        if (isFreeText && freeText != null && !freeText.isEmpty()) {
            return value.isEmpty() ? freeText : value + "; " + freeText;
        }

        return value;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateParticipant(ParticipantDTORequest req) {

        OrderDetail participant = orderDetailRepository.findByUuid(req.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        Integer eventId = participant.getOrder() != null && participant.getOrder().getEvent() != null
                ? participant.getOrder().getEvent().getId()
                : null;

        List<ParticipantEditLogDto.Change> changes = new ArrayList<>();

        // Distance: must belong to the same event. A full quota only warns — the caller resends
        // with confirmOverQuota once the user accepts. The paid price snapshot stays as it is.
        if (!isBlank(req.getEventTypeId()) && (participant.getEventType() == null
                || !req.getEventTypeId().equals(participant.getEventType().getUuid()))) {
            EventType target = eventTypeRepository.findByUuid(req.getEventTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event type not found"));
            if (target.getEvent() == null || !Objects.equals(target.getEvent().getId(), eventId)) {
                throw new BusinessException("ประเภทการแข่งขันที่เลือกไม่ได้อยู่ในอีเวนต์นี้");
            }
            Pricing targetPricing = matchingPricing(participant.getPricing(), target);
            if (!Boolean.TRUE.equals(req.getConfirmOverQuota())) {
                String full = overQuotaMessage(target, targetPricing);
                if (full != null) {
                    throw new ParticipantOverQuotaException(full);
                }
            }
            track(changes, "eventType",
                    participant.getEventType() != null ? participant.getEventType().getName() : null, target.getName());
            participant.setEventType(target);
            participant.setPricing(targetPricing);
        }

        // Shirt: the type must be one of this event's, the size one of that type's.
        ShirtType shirtType = participant.getShirtType();
        if (!isBlank(req.getShirtTypeId()) && (shirtType == null || !req.getShirtTypeId().equals(shirtType.getUuid()))) {
            ShirtType target = shirtTypeRepository.findByUuid(req.getShirtTypeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Shirt type not found"));
            if (target.getEvent() == null || !Objects.equals(target.getEvent().getId(), eventId)) {
                throw new BusinessException("แบบเสื้อที่เลือกไม่ได้อยู่ในอีเวนต์นี้");
            }
            track(changes, "shirtType", shirtType != null ? shirtType.getName() : null, target.getName());
            participant.setShirtType(target);
            shirtType = target;
        }
        if (!isBlank(req.getShirtSizeId())) {
            Optional<ShirtSize> shirtSize = shirtSizeRepository.findByUuid(req.getShirtSizeId());
            if (shirtSize.isPresent()) {
                ShirtSize size = shirtSize.get();
                if (shirtType != null && size.getShirtType() != null
                        && !Objects.equals(size.getShirtType().getId(), shirtType.getId())) {
                    throw new BusinessException("ไซส์เสื้อที่เลือกไม่ตรงกับแบบเสื้อ");
                }
                ShirtSize old = participant.getShirtSize();
                if (old == null || !Objects.equals(old.getId(), size.getId())) {
                    track(changes, "shirtSize", old != null ? old.getName() : null, size.getName());
                    participant.setShirtSize(size);
                }
            }
        }

        track(changes, "bibNo", participant.getBibNo(), req.getBibNo());
        participant.setBibNo(req.getBibNo());
        track(changes, "teamClub", participant.getTeamClub(), req.getTeamClub());
        participant.setTeamClub(req.getTeamClub());
        track(changes, "firstName", participant.getFirstName(), req.getFirstName());
        participant.setFirstName(req.getFirstName());
        track(changes, "lastName", participant.getLastName(), req.getLastName());
        participant.setLastName(req.getLastName());
        track(changes, "firstNameEn", participant.getFirstNameEn(), req.getFirstNameEn());
        participant.setFirstNameEn(req.getFirstNameEn());
        track(changes, "lastNameEn", participant.getLastNameEn(), req.getLastNameEn());
        participant.setLastNameEn(req.getLastNameEn());
        track(changes, "idNo", participant.getIdNo(), req.getIdNo());
        participant.setIdNo(req.getIdNo());
        track(changes, "gender", participant.getGender(), req.getGender());
        participant.setGender(req.getGender());

        // Compare the calendar day in Thai time: the form sends local midnight as UTC (…T17:00Z).
        String oldBirth = participant.getBirthDate() != null
                ? participant.getBirthDate().atZoneSameInstant(ExportDateTimeUtils.BANGKOK_ZONE).toLocalDate().toString()
                : null;
        String newBirth = req.getBirthDate() != null
                ? req.getBirthDate().atZoneSameInstant(ExportDateTimeUtils.BANGKOK_ZONE).toLocalDate().toString()
                : null;
        if (!Objects.equals(oldBirth, newBirth)) {
            track(changes, "birthDate", oldBirth, newBirth);
            participant.setBirthDate(req.getBirthDate());
            // Same rule as at registration: age in the year the runner signed up.
            participant.setAge(req.getBirthDate() == null ? null
                    : AgeGroupUtils.calculateAgeAtRefDate(req.getBirthDate(), participant.getCreatedTime()));
        }

        track(changes, "nationality", participant.getNationality(), req.getNationality());
        participant.setNationality(req.getNationality());
        track(changes, "email", participant.getEmail(), req.getEmail());
        participant.setEmail(req.getEmail());
        track(changes, "phone", participant.getPhone(), req.getPhone());
        participant.setPhone(req.getPhone());
        track(changes, "address", participant.getAddress(), req.getAddress());
        participant.setAddress(req.getAddress());
        track(changes, "province", participant.getProvince(), req.getProvince());
        participant.setProvince(req.getProvince());
        track(changes, "amphoe", participant.getAmphoe(), req.getAmphoe());
        participant.setAmphoe(req.getAmphoe());
        track(changes, "district", participant.getDistrict(), req.getDistrict());
        participant.setDistrict(req.getDistrict());
        track(changes, "zipcode", participant.getZipcode(), req.getZipcode());
        participant.setZipcode(req.getZipcode());
        track(changes, "shippingAddress", participant.getShippingAddress(), req.getShippingAddress());
        participant.setShippingAddress(req.getShippingAddress());
        track(changes, "shippingProvince", participant.getShippingProvince(), req.getShippingProvince());
        participant.setShippingProvince(req.getShippingProvince());
        track(changes, "shippingAmphoe", participant.getShippingAmphoe(), req.getShippingAmphoe());
        participant.setShippingAmphoe(req.getShippingAmphoe());
        track(changes, "shippingDistrict", participant.getShippingDistrict(), req.getShippingDistrict());
        participant.setShippingDistrict(req.getShippingDistrict());
        track(changes, "shippingZipcode", participant.getShippingZipcode(), req.getShippingZipcode());
        participant.setShippingZipcode(req.getShippingZipcode());
        track(changes, "bloodType", participant.getBloodType(), req.getBloodType());
        participant.setBloodType(req.getBloodType());
        track(changes, "healthIssues", participant.getHealthIssues(), req.getHealthIssues());
        participant.setHealthIssues(req.getHealthIssues());
        track(changes, "emergencyContact", participant.getEmergencyContact(), req.getEmergencyContact());
        participant.setEmergencyContact(req.getEmergencyContact());
        track(changes, "emergencyRelation", participant.getEmergencyRelation(), req.getEmergencyRelation());
        participant.setEmergencyRelation(req.getEmergencyRelation());
        track(changes, "emergencyPhone", participant.getEmergencyPhone(), req.getEmergencyPhone());
        participant.setEmergencyPhone(req.getEmergencyPhone());

        if (req.getSelectionAnswers() != null) {
            String before = answersText(participant.getSelectionAnswers());
            String after = answersText(req.getSelectionAnswers());
            track(changes, "selectionAnswers", before, after);
            participant.setSelectionAnswers(req.getSelectionAnswers());
        }

        if (!changes.isEmpty()) {
            recordManualEdit(participant, changes);
        }

        orderDetailRepository.save(participant);
    }

    /** The target distance's pricing for the same phase (Early Bird → Early Bird), else none. */
    private Pricing matchingPricing(Pricing current, EventType target) {
        if (current == null || current.getPaymentType() == null || target.getPricing() == null) {
            return null;
        }
        Integer phaseId = current.getPaymentType().getId();
        return target.getPricing().stream()
                .filter(p -> p.getPaymentType() != null && Objects.equals(p.getPaymentType().getId(), phaseId))
                .findFirst()
                .orElse(null);
    }

    /** Thai warning when one more runner would exceed the distance or its pricing phase, else null. */
    private String overQuotaMessage(EventType target, Pricing pricing) {
        if (target.getQuota() != null) {
            long used = orderDetailRepository.countRegisteredByEventTypeId(target.getId());
            if (used >= target.getQuota()) {
                return String.format("ประเภท %s โควตาเต็มแล้ว (%d/%d)", target.getName(), used, target.getQuota());
            }
        }
        if (pricing != null && pricing.getQuota() != null) {
            long used = orderDetailRepository.countByPricingIdAndActiveOrder(pricing.getId());
            if (used >= pricing.getQuota()) {
                String phase = pricing.getPaymentType() != null ? pricing.getPaymentType().getName() : "";
                return String.format("ประเภท %s รอบ %s โควตาเต็มแล้ว (%d/%d)", target.getName(), phase, used,
                        pricing.getQuota());
            }
        }
        return null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    /** Records a change when the value really differs; blank and null count as the same. */
    private static void track(List<ParticipantEditLogDto.Change> changes, String field, String before, String after) {
        String b = trimToNull(before);
        String a = trimToNull(after);
        if (!Objects.equals(b, a)) {
            changes.add(new ParticipantEditLogDto.Change(field, b, a));
        }
    }

    private String answersText(List<SelectionAnswerDto> answers) {
        if (answers == null) {
            return null;
        }
        return answers.stream()
                .map(a -> Objects.toString(a.getQuestion() != null ? a.getQuestion().getValue() : null, "")
                        + ": " + extractAnswerValue(a.getValue()))
                .collect(Collectors.joining("; "));
    }

    private void recordManualEdit(OrderDetail participant, List<ParticipantEditLogDto.Change> changes) {
        OffsetDateTime now = OffsetDateTime.now();
        String by = currentEditorName();

        List<ParticipantEditLogDto> history = readEditLog(participant.getManualEditLog());
        history.add(new ParticipantEditLogDto(now, by, changes));
        try {
            participant.setManualEditLog(objectMapper.writeValueAsString(history));
        } catch (JsonProcessingException e) {
            throw new BusinessException("บันทึกประวัติการแก้ไขไม่สำเร็จ", e);
        }
        participant.setManualEditedTime(now);
        participant.setManualEditedBy(by);
    }

    private String currentEditorName() {
        Integer userId = contextUtils.getCurrentUserIdOrNull();
        User user = userId != null ? userRepository.findById(userId).orElse(null) : null;
        if (user == null) {
            return "-";
        }
        String name = (Objects.toString(user.getFirstName(), "") + " " + Objects.toString(user.getLastName(), "")).trim();
        if (name.isEmpty()) {
            name = Objects.toString(user.getEmail(), "-");
        }
        String role = user.getRole() != null ? user.getRole().getRole() : null;
        return role != null && !role.isBlank() ? name + " (" + role + ")" : name;
    }

    private List<ParticipantEditLogDto> readEditLog(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(objectMapper.readValue(json, new TypeReference<List<ParticipantEditLogDto>>() {
            }));
        } catch (JsonProcessingException e) {
            log.warn("Unreadable manualEditLog, starting over: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadParticipants(List<ParticipantUploadDTORequest> uploadDTORequests) {
        for (ParticipantUploadDTORequest request : uploadDTORequests) {
            List<OrderDetail> participantsToSave = new ArrayList<>();

            for (ParticipantUploadDTO dto : request.getData()) {
                if (dto.getIdNo() == null || dto.getId() == null)
                    continue;

                Optional<OrderDetail> optionalParticipant = orderDetailRepository.findByUuid(dto.getId());

                if (optionalParticipant.isPresent()) {
                    OrderDetail participant = optionalParticipant.get();
                    participant.setFirstName(dto.getFirstName());
                    participant.setLastName(dto.getLastName());
                    participant.setFirstNameEn(dto.getFirstNameEn());
                    participant.setLastNameEn(dto.getLastNameEn());
                    participant.setIdNo(dto.getIdNo());
                    participant.setGender(dto.getGender());
                    participant.setBirthDate(dto.getBirthDate());
                    participant.setNationality(dto.getNationality());
                    participant.setPhone(dto.getPhone());
                    participant.setEmail(dto.getEmail());
                    participant.setAddress(dto.getAddress());
                    participant.setProvince(dto.getProvince());
                    participant.setAmphoe(dto.getAmphoe());
                    participant.setDistrict(dto.getDistrict());
                    participant.setZipcode(dto.getZipcode());
                    participant.setBloodType(dto.getBloodType());
                    participant.setHealthIssues(dto.getHealthIssues());
                    participant.setEmergencyContact(dto.getEmergencyContact());
                    participant.setEmergencyPhone(dto.getEmergencyPhone());
                    participant.setShippingAddress(dto.getShippingAddress());
                    participant.setShippingProvince(dto.getShippingProvince());
                    participant.setShippingAmphoe(dto.getShippingAmphoe());
                    participant.setShippingDistrict(dto.getShippingDistrict());
                    participant.setShippingZipcode(dto.getShippingZipcode());
                    participant.setTeamClub(dto.getTeamClub());
                    participant.setBibNo(dto.getBibNo());

                    Optional<ShirtSize> shirtSize = shirtSizeRepository.findByUuid(dto.getShirtSizeId());

                    if (shirtSize.isPresent()) {
                        participant.setShirtSize(shirtSize.get());
                    }

                    participantsToSave.add(participant);
                }
            }

            orderDetailRepository.saveAll(participantsToSave);
        }
    }

}
