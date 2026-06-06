package com.actionth.membership.service.impl;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.persistence.EntityNotFoundException;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.actionth.membership.exception.EventModificationException;
import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.AgeGroup;
import com.actionth.membership.model.CountryState;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventCalendar;
import com.actionth.membership.model.EventCondition;
import com.actionth.membership.model.EventDetail;
import com.actionth.membership.model.EventPermission;
import com.actionth.membership.model.EventSelectionField;
import com.actionth.membership.model.EventSelectionOption;
import com.actionth.membership.model.EventType;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.PaymentType;
import com.actionth.membership.model.Pricing;
import com.actionth.membership.model.ShirtSize;
import com.actionth.membership.model.ShirtType;
import com.actionth.membership.model.StandardFields;
import com.actionth.membership.model.User;
import com.actionth.membership.model.dto.AgeGroupDto;
import com.actionth.membership.model.dto.CountryStateDto;
import com.actionth.membership.model.dto.EventConditionDto;
import com.actionth.membership.model.dto.EventDetailDto;
import com.actionth.membership.model.dto.EventDto;
import com.actionth.membership.model.dto.EventPermissionSummaryDto;
import com.actionth.membership.model.dto.EventSelectionFieldDto;
import com.actionth.membership.model.dto.EventSelectionOptionDto;
import com.actionth.membership.model.dto.EventTypeAvailabilityResponse;
import com.actionth.membership.model.dto.EventTypeDto;
import com.actionth.membership.model.dto.EventViewDto;
import com.actionth.membership.model.dto.PaymentTypeDto;
import com.actionth.membership.model.dto.PricingDto;
import com.actionth.membership.model.dto.ShirtSizeDto;
import com.actionth.membership.model.dto.ShirtTypeDto;
import com.actionth.membership.model.projection.EventOrganizerProjection;
import com.actionth.membership.projection.PricingAvailabilityProjection;
import com.actionth.membership.model.request.GeneralRequest;
import com.actionth.membership.repository.EventCalendarRepository;
import com.actionth.membership.repository.EventRepository;
import com.actionth.membership.repository.OrderDetailRepository;
import com.actionth.membership.repository.PricingRepository;
import com.actionth.membership.repository.UserRepository;
import com.actionth.membership.service.EventService;
import com.actionth.membership.utils.ContextUtils;
import com.actionth.membership.utils.SearchPredicateBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EventServiceImpl implements EventService {

	private final EventRepository eventRepository;
	private final EventCalendarRepository eventCalendarRepository;
	private final UserRepository userRepository;
	private final OrderDetailRepository orderDetailRepository;
	private final PricingRepository pricingRepository;
	private final ModelMapper modelMapper;
	private final ContextUtils contextUtils;

	@Override
	public EventDto getEventByUuid(String uuid) {
		Event event = eventRepository.findByUuid(uuid)
				.orElseThrow(() -> new EntityNotFoundException("Event not found with uuid: " + uuid));

		return mapEventToDto(event);
	}

	@Override
	public EventDto getEventByLinkOrUuid(String uuid) {
		Event event = eventRepository.findByLinkOrUuid(uuid)
				.orElseThrow(() -> new EntityNotFoundException("Event not found with uuid: " + uuid));

		return mapEventToDto(event);
	}

	@Override
	public Page<EventViewDto> findAll(GeneralRequest generalRequest) {
		PagingData paging = generalRequest.getPaging();
		Integer createdBy = generalRequest.getCreatedBy() != null
				? generalRequest.getCreatedBy()
				: null;

		Specification<Event> searchSpec = (root, query, cb) -> {
			query.distinct(true);
			List<Predicate> predicates = new ArrayList<>();

			if (generalRequest.getActive() != null) {
				predicates.add(cb.equal(root.get("active"), generalRequest.getActive()));
			}
			if (generalRequest.getCreatedBy() != null) {
				Join<Event, EventPermission> ep = root.join("eventPermissions", JoinType.LEFT);
				ep.on(cb.or(
						cb.isTrue(ep.get("canRead")),
						cb.isTrue(ep.get("canUpdate")),
						cb.isTrue(ep.get("canDelete"))));
				predicates.add(cb.equal(ep.get("user").get("id"), generalRequest.getCreatedBy()));
			}

			predicates.addAll(SearchPredicateBuilder.build(cb, root, paging, s -> {
				if ("province".equalsIgnoreCase(s.getSearchField())) {
					Join<Event, CountryState> countryState = root.join("province", JoinType.LEFT);
					return cb.equal(countryState.get("uuid"), s.getSearchText());
				}
				return null;
			}));

			return cb.and(predicates.toArray(new Predicate[0]));
		};

		if (paging == null || paging.getPage() < 1) {
			List<EventViewDto> dtos = eventRepository.findAll(
					searchSpec,
					Sort.by(Sort.Direction.DESC, "id")).stream()
					.map(event -> mapViewEventToDto(event, createdBy))
					.toList();
			return new PageImpl<>(dtos);
		} else {
			Pageable pageable = PageRequest.of(
					paging.getPage(),
					paging.getSize(),
					paging.getSortField() != null
							? Sort.by(Sort.Direction.fromString(paging.getSortDirection()), paging.getSortField())
							: Sort.by(Sort.Direction.DESC, "id"));

			return eventRepository.findAll(searchSpec, pageable)
					.map(event -> mapViewEventToDto(event, createdBy));
		}
	}

	@Override
	public List<OffsetDateTime> getAllEventDatesInMonth(OffsetDateTime date) {
		OffsetDateTime startODT = date.with(TemporalAdjusters.firstDayOfMonth())
				.withHour(0).withMinute(0).withSecond(0).withNano(0);
		OffsetDateTime endODT = date.with(TemporalAdjusters.lastDayOfMonth())
				.withHour(23).withMinute(59).withSecond(59).withNano(999999999);

		List<OffsetDateTime> internalDates = eventCalendarRepository
				.findByActiveTrueAndIsApprovedTrueAndEventDateBetween(startODT, endODT)
				.stream()
				.map(EventCalendar::getEventDate)
				.toList();

		List<OffsetDateTime> externalDates = eventRepository
				.findByActiveTrueAndIsDraftFalseAndEventDateBetween(startODT, endODT)
				.stream()
				.map(Event::getEventDate)
				.toList();

		return Stream.concat(internalDates.stream(), externalDates.stream())
				.distinct()
				.sorted()
				.toList();
	}

	@Override
	public EventDto createEvent(EventDto dto) {
		Event entity = new Event();
		applyDtoToEntity(dto, entity, false);
		entity.setIsDraft(true);
		Event saved = eventRepository.save(entity);
		return mapEventToDto(saved);
	}

	@Override
	public EventDto updateEvent(EventDto dto) {
		Event entity = eventRepository.findByUuid(dto.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Event not found"));

		validateEventModification(entity, dto);

		applyDtoToEntity(dto, entity, true);
		Event updated = eventRepository.save(entity);
		return mapEventToDto(updated);
	}

	private void validateEventModification(Event entity, EventDto dto) {
		boolean hasParticipants = orderDetailRepository.existsByEventIdAndActiveOrder(entity.getId());
		if (!hasParticipants) {
			return;
		}

		Map<String, EventType> existingEventTypes = entity.getEventTypes().stream()
				.filter(et -> et.getUuid() != null)
				.collect(Collectors.toMap(EventType::getUuid, et -> et));

		for (EventTypeDto etDto : dto.getEventTypes()) {
			if (etDto.getId() != null && existingEventTypes.containsKey(etDto.getId())) {
				EventType existingEt = existingEventTypes.get(etDto.getId());
				Long participantCount = orderDetailRepository.countRegisteredByEventTypeId(existingEt.getId());
				
				if (participantCount > 0) {
					if (existingEt.getPrice() != null && etDto.getPrice() != null 
							&& existingEt.getPrice().compareTo(etDto.getPrice()) != 0) {
						throw new EventModificationException(
								"ไม่สามารถแก้ไขราคาได้ เนื่องจากมีผู้สมัครรายการนี้แล้ว",
								"eventType.price",
								existingEt.getName());
					}

					if (etDto.getQuota() != null && etDto.getQuota() < participantCount) {
						throw new EventModificationException(
								"ไม่สามารถลดโควต้าต่ำกว่าจำนวนผู้สมัครได้ (ผู้สมัครปัจจุบัน: " + participantCount + ")",
								"eventType.quota",
								existingEt.getName());
					}
				}

				validatePricingModification(existingEt, etDto);
			}
		}

		List<String> dtoEventTypeIds = dto.getEventTypes().stream()
				.map(EventTypeDto::getId)
				.filter(Objects::nonNull)
				.toList();

		for (EventType existingEt : entity.getEventTypes()) {
			if (existingEt.getUuid() != null && !dtoEventTypeIds.contains(existingEt.getUuid())) {
				Long participantCount = orderDetailRepository.countRegisteredByEventTypeId(existingEt.getId());
				if (participantCount > 0) {
					throw new EventModificationException(
							"ไม่สามารถลบประเภทการแข่งขันได้ เนื่องจากมีผู้สมัครรายการนี้แล้ว",
							"eventType",
							existingEt.getName());
				}
			}
		}

		List<String> dtoShirtTypeIds = dto.getShirtTypes().stream()
				.map(ShirtTypeDto::getId)
				.filter(Objects::nonNull)
				.toList();

		for (ShirtType existingSt : entity.getShirtTypes()) {
			if (existingSt.getUuid() != null && !dtoShirtTypeIds.contains(existingSt.getUuid())) {
				boolean hasSelection = orderDetailRepository.existsByShirtTypeIdAndActiveOrder(existingSt.getId());
				if (hasSelection) {
					throw new EventModificationException(
							"ไม่สามารถลบประเภทเสื้อได้ เนื่องจากมีผู้สมัครเลือกประเภทนี้แล้ว",
							"shirtType",
							existingSt.getName());
				}
			}

			if (existingSt.getUuid() != null && dtoShirtTypeIds.contains(existingSt.getUuid())) {
				ShirtTypeDto matchingDto = dto.getShirtTypes().stream()
						.filter(st -> existingSt.getUuid().equals(st.getId()))
						.findFirst()
						.orElse(null);

				if (matchingDto != null) {
					List<String> dtoShirtSizeIds = matchingDto.getShirtSizes().stream()
							.map(ShirtSizeDto::getId)
							.filter(Objects::nonNull)
							.toList();

					for (ShirtSize existingSz : existingSt.getShirtSizes()) {
						if (existingSz.getUuid() != null && !dtoShirtSizeIds.contains(existingSz.getUuid())) {
							boolean hasSelection = orderDetailRepository.existsByShirtSizeIdAndActiveOrder(existingSz.getId());
							if (hasSelection) {
								throw new EventModificationException(
										"ไม่สามารถลบไซส์เสื้อได้ เนื่องจากมีผู้สมัครเลือกไซส์นี้แล้ว",
										"shirtSize",
										existingSz.getName());
							}
						}
					}
				}
			}
		}
	}

	private void validatePricingModification(EventType existingEt, EventTypeDto etDto) {
		Map<String, Pricing> existingPricingMap = existingEt.getPricing().stream()
				.filter(p -> p.getUuid() != null)
				.collect(Collectors.toMap(Pricing::getUuid, p -> p));

		// Check for removed Pricing that has active orders
		List<String> dtoPricingIds = etDto.getPricing().stream()
				.map(PricingDto::getId)
				.filter(Objects::nonNull)
				.toList();

		for (Pricing existingPricing : existingEt.getPricing()) {
			if (existingPricing.getUuid() != null && !dtoPricingIds.contains(existingPricing.getUuid())) {
				boolean hasOrders = orderDetailRepository.existsByPricingIdAndActiveOrder(existingPricing.getId());
				if (hasOrders) {
					String pricingName = existingPricing.getPaymentType() != null 
							? existingPricing.getPaymentType().getName() 
							: "Pricing";
					throw new EventModificationException(
							"ไม่สามารถลบช่วงราคาได้ เนื่องจากมีผู้สมัครใช้ราคานี้แล้ว",
							"pricing",
							pricingName);
				}
			}
		}

		// Check for price/quota modifications on Pricing with active orders
		for (PricingDto pDto : etDto.getPricing()) {
			if (pDto.getId() != null && existingPricingMap.containsKey(pDto.getId())) {
				Pricing existingPricing = existingPricingMap.get(pDto.getId());
				Long usedCount = orderDetailRepository.countByPricingIdAndActiveOrder(existingPricing.getId());

				if (usedCount > 0) {
					String pricingName = existingPricing.getPaymentType() != null 
							? existingPricing.getPaymentType().getName() 
							: "Pricing";

					// Check price change
					if (existingPricing.getPrice() != null && pDto.getPrice() != null
							&& existingPricing.getPrice().compareTo(pDto.getPrice()) != 0) {
						throw new EventModificationException(
								"ไม่สามารถแก้ไขราคาช่วงได้ เนื่องจากมีผู้สมัครใช้ราคานี้แล้ว",
								"pricing.price",
								pricingName);
					}

					// Check quota reduction below used count
					if (pDto.getQuota() != null && pDto.getQuota() < usedCount) {
						throw new EventModificationException(
								"ไม่สามารถลดโควต้าช่วงราคาต่ำกว่าจำนวนผู้สมัครได้ (ผู้สมัครปัจจุบัน: " + usedCount + ")",
								"pricing.quota",
								pricingName);
					}
				}
			}
		}
	}

	@Override
	public void updateStatus(EventDto dto) {
		Event entity = eventRepository.findByUuid(dto.getId())
				.orElseThrow(() -> new ResourceNotFoundException("Event not found"));

		entity.setIsDraft(dto.getIsDraft());
		eventRepository.save(entity);
	}

	@Override
	public void deleteEvent(String uuid, String mode) {
		if ("hard".equals(mode)) {
			Event entity = eventRepository.findByUuid(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("Event not found"));
			eventRepository.delete(entity);
		} else if ("soft".equals(mode)) {
			Event entity = eventRepository.findByUuid(uuid)
					.orElseThrow(() -> new ResourceNotFoundException("Event not found"));
			entity.setActive(false);
			eventRepository.save(entity);
		}
	}

	@Override
	public List<EventDto> findEventByOrganizer(String id) {
		List<EventOrganizerProjection> results = eventRepository.findEventByOrganizer(id);
		return results.stream()
				.map(result -> {
					EventDto dto = new EventDto();
					dto.setId(result.getUuid());
					dto.setName(result.getName());
					dto.setEventDate(result.getEventDate());
					return dto;
				})
				.toList();
	}

	@Override
	public List<EventDto> findEventByPermission(String id) {
		List<EventOrganizerProjection> results = eventRepository.findEventByPermission(id);
		return results.stream()
				.map(result -> {
					EventDto dto = new EventDto();
					dto.setId(result.getUuid());
					dto.setName(result.getName());
					dto.setEventDate(result.getEventDate());
					return dto;
				})
				.toList();
	}

	@Override
	public Map<String, Object> findIdAndNameByUuid(String id) {
		return eventRepository.findIdAndNameByUuid(id);
	}

	private void applyDtoToEntity(EventDto dto, Event entity, boolean isUpdate) {
		entity.setName(dto.getName());
		entity.setEventDate(dto.getEventDate());
		entity.setOrganizerName(dto.getOrganizerName());
		entity.setLocation(dto.getLocation());
		entity.setDescription(dto.getDescription());
		entity.setLogoUrl(dto.getLogoUrl());
		entity.setPictureUrl(dto.getPictureUrl());
		entity.setPrefixPath(dto.getPrefixPath());
		entity.setType(dto.getType());
		entity.setLink(dto.getLink());
		entity.setStartRegistrationDate(dto.getStartRegistrationDate());
		entity.setEndRegistrationDate(dto.getEndRegistrationDate());
		entity.setShippingFee(dto.getShippingFee());
		entity.setGeneralInfoTitle(dto.getGeneralInfoTitle());
		entity.setEventTypeTitle(dto.getEventTypeTitle());
		entity.setEventPrimaryColor(dto.getEventPrimaryColor());
		entity.setEventSecondaryColor(dto.getEventSecondaryColor());
		entity.setEventFontColor(dto.getEventFontColor());
		entity.setIsDraft(dto.getIsDraft());
		entity.setShowChecklist(dto.getShowChecklist());

		if (dto.getProvinceId() != null) {
			CountryState countryState = eventRepository.findCountryStateByUuid(dto.getProvinceId())
					.orElseThrow(() -> new ResourceNotFoundException(
							"Province not found: " + dto.getProvinceId()));
			entity.setProvince(countryState);
		} else {
			entity.setProvince(null);
		}

		// Organizer (nullable)
		if (dto.getOrganizerId() != null) {
			User organizer = userRepository.findByUuid(dto.getOrganizerId())
					.orElseThrow(() -> new ResourceNotFoundException(
							"Organizer not found: " + dto.getOrganizerId()));
			entity.setOrganizer(organizer);
		} else if (contextUtils.getCurrentUserIdOrNull() != null) {
			User organizer = userRepository.findById(contextUtils.getCurrentUserIdOrNull())
					.orElseThrow(() -> new ResourceNotFoundException(
							"Organizer not found: " + dto.getOrganizerId()));
			entity.setOrganizer(organizer);
		} else {
			entity.setOrganizer(null);
		}

		// === EventConditions ===
		mergeChild(entity.getEventConditions(), dto.getEventConditions(), (dtoC, existingMap) -> {
			EventCondition c = existingMap.getOrDefault(dtoC.getId(), new EventCondition());
			c.setDescription(dtoC.getDescription());
			c.setEvent(entity);
			return c;
		});

		// === EventDetails ===
		mergeChild(entity.getEventDetails(), dto.getEventDetails(), (dtoD, existingMap) -> {
			EventDetail d = existingMap.getOrDefault(dtoD.getId(), new EventDetail());
			d.setTitle(dtoD.getTitle());
			d.setDetail(dtoD.getDetail());
			d.setType(dtoD.getType());
			d.setPosition(dtoD.getPosition());
			d.setEvent(entity);
			return d;
		});

		// === PaymentTypes ===
		Map<String, PaymentType> paymentMap = entity.getPaymentTypes().stream()
				.filter(pt -> pt.getUuid() != null)
				.collect(Collectors.toMap(PaymentType::getUuid, pt -> pt));
		List<PaymentType> newPayments = new ArrayList<>();
		for (PaymentTypeDto pt : dto.getPaymentTypes()) {
			PaymentType p = isUpdate && pt.getId() != null && paymentMap.containsKey(pt.getId())
					? paymentMap.get(pt.getId())
					: new PaymentType();

			p.setTempUuid(pt.getId());
			p.setName(pt.getName());
			p.setEndDate(pt.getEndDate());
			p.setEvent(entity);
			newPayments.add(p);
		}
		entity.getPaymentTypes().clear();
		entity.getPaymentTypes().addAll(newPayments);

		// === ShirtTypes & ShirtSizes ===
		mergeChild(entity.getShirtTypes(), dto.getShirtTypes(), (dtoSt, shirtTypeMap) -> {
			ShirtType st = isUpdate && dtoSt.getId() != null
					&& shirtTypeMap.containsKey(dtoSt.getId())
							? shirtTypeMap.get(dtoSt.getId())
							: new ShirtType();

			st.setName(dtoSt.getName());
			st.setDescription(dtoSt.getDescription());
			st.setEvent(entity);

			mergeChild(st.getShirtSizes(), dtoSt.getShirtSizes(), (dtoSz, szMap) -> {
				ShirtSize sz = isUpdate && dtoSz.getId() != null
						&& szMap.containsKey(dtoSz.getId())
								? szMap.get(dtoSz.getId())
								: new ShirtSize();
				sz.setName(dtoSz.getName());
				sz.setChestSize(dtoSz.getChestSize());
				sz.setLengthSize(dtoSz.getLengthSize());
				sz.setShirtType(st);
				return sz;
			});

			return st;
		});

		// === EventTypes ===
		Map<String, EventType> eventTypeMap = entity.getEventTypes().stream()
				.filter(et -> et.getUuid() != null)
				.collect(Collectors.toMap(EventType::getUuid, et -> et));
		List<EventType> newEventTypes = new ArrayList<>();

		for (EventTypeDto et : dto.getEventTypes()) {
			EventType eventType = isUpdate && et.getId() != null && eventTypeMap.containsKey(et.getId())
					? eventTypeMap.get(et.getId())
					: new EventType();

			eventType.setName(et.getName());
			eventType.setEventDate(et.getEventDate());
			eventType.setQuota(et.getQuota());
			eventType.setPrice(et.getPrice());
			eventType.setIsNoShirt(et.getIsNoShirt());
			eventType.setDiscountNoShirt(et.getDiscountNoShirt());
			eventType.setIsTeam(et.getIsTeam());
			eventType.setEvent(entity);

			// === Pricing ===
			mergeChild(eventType.getPricing(), et.getPricing(), (dtoP, existingMap) -> {
				Pricing p = isUpdate && dtoP.getId() != null && existingMap.containsKey(dtoP.getId())
						? existingMap.get(dtoP.getId())
						: new Pricing();

				p.setPrice(dtoP.getPrice());
				p.setQuota(dtoP.getQuota());
				PaymentType matched = newPayments.stream()
						.filter(x -> Objects.equals(x.getTempUuid(), dtoP.getPaymentTypeId()))
						.findFirst()
						.orElseThrow(() -> new RuntimeException(
								"PaymentType not found: " + dtoP.getPaymentTypeId()));
				p.setPaymentType(matched);
				p.setEventType(eventType);
				return p;
			});

			// === AgeGroups ===
			mergeChild(eventType.getAgeGroups(), et.getAgeGroups(), (dtoA, existingMap) -> {
				AgeGroup ag = isUpdate && dtoA.getId() != null && existingMap.containsKey(dtoA.getId())
						? existingMap.get(dtoA.getId())
						: new AgeGroup();

				ag.setGender(AgeGroup.Gender.valueOf(dtoA.getGender().toLowerCase()));
				ag.setMinAge(dtoA.getMinAge());
				ag.setMaxAge(dtoA.getMaxAge());
				ag.setPosition(dtoA.getPosition());
				ag.setEventType(eventType);
				return ag;
			});

			// === EventSelection Field ===
			mergeChild(eventType.getSelectionFields(), et.getSelectionFields(), (dtoF, existingMap) -> {
				EventSelectionField f = isUpdate && dtoF.getId() != null && existingMap.containsKey(dtoF.getId())
						? existingMap.get(dtoF.getId())
						: new EventSelectionField();

				f.setTitle(dtoF.getTitle());
				f.setTitleEn(dtoF.getTitleEn());
				f.setType(dtoF.getType());
				f.setRequired(dtoF.isRequired());
				f.setEventType(eventType);

				mergeChild(f.getOptions(), dtoF.getOptions(), (dtoO, existingOptMap) -> {
					EventSelectionOption opt = isUpdate && dtoO.getId() != null && existingOptMap.containsKey(dtoO.getId())
							? existingOptMap.get(dtoO.getId())
							: new EventSelectionOption();

					opt.setValue(dtoO.getValue());
					opt.setValueEn(dtoO.getValueEn());
					opt.setInputType(dtoO.getInputType());
					opt.setPosition(dtoO.getPosition());
					opt.setSelectionField(f);
					return opt;
				});

				return f;
			});

			newEventTypes.add(eventType);
		}

		entity.getEventTypes().clear();
		entity.getEventTypes().addAll(newEventTypes);

		// === EventSelection Field ===
		mergeChild(entity.getSelectionFields(), dto.getSelectionFields(), (dtoF, existingMap) -> {
			EventSelectionField f = isUpdate && dtoF.getId() != null && existingMap.containsKey(dtoF.getId())
					? existingMap.get(dtoF.getId())
					: new EventSelectionField();

			f.setTitle(dtoF.getTitle());
			f.setTitleEn(dtoF.getTitleEn());
			f.setType(dtoF.getType());
			f.setRequired(dtoF.isRequired());
			f.setEvent(entity);

			// === Options ===
			mergeChild(f.getOptions(), dtoF.getOptions(), (dtoO, existingOptMap) -> {
				EventSelectionOption opt = isUpdate && dtoO.getId() != null && existingOptMap.containsKey(dtoO.getId())
						? existingOptMap.get(dtoO.getId())
						: new EventSelectionOption();

				opt.setValue(dtoO.getValue());
				opt.setValueEn(dtoO.getValueEn());
				opt.setInputType(dtoO.getInputType());
				opt.setPosition(dtoO.getPosition());
				opt.setSelectionField(f);

				return opt;
			});

			return f;
		});

		// === Event Permission (only on create) ===
		if (!isUpdate) {
			entity.getEventPermissions().clear();
			EventPermission e = new EventPermission();
			e.setEvent(entity);
			e.setUser(entity.getOrganizer());
			e.setRole("owner");
			e.syncBooleanFlags();
			entity.getEventPermissions().add(e);
		}
	}

	private EventViewDto mapViewEventToDto(Event event, Integer createdBy) {
		EventViewDto eventMapper = modelMapper.map(event, EventViewDto.class);

		ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Bangkok"));

		ZonedDateTime start = event.getStartRegistrationDate().atZoneSameInstant(ZoneId.of("Asia/Bangkok"));
		ZonedDateTime end = event.getEndRegistrationDate().atZoneSameInstant(ZoneId.of("Asia/Bangkok"));

		if (now.isBefore(start)) {
			eventMapper.setEventStatus("soon");
		} else if (!now.isAfter(end)) {
			eventMapper.setEventStatus("openRegistration");
		} else {
			eventMapper.setEventStatus("closedRegistration");
		}

		String ownerUuid = event.getOrganizer().getUuid();
		eventMapper.setOrganizerId(ownerUuid);
		if (createdBy != null) {
			EventPermission myPermission = event.getEventPermissions().stream()
					.filter(p -> p.getUser() != null && Objects.equals(p.getUser().getId(), createdBy))
					.findFirst()
					.orElse(null);
			if (myPermission != null) {
				String role;
				if (myPermission.getUser().getUuid().equals(ownerUuid)) {
					role = "owner";
				} else if (myPermission.getRole() != null && !myPermission.getRole().isEmpty()) {
					role = myPermission.getRole();
				} else if (Boolean.TRUE.equals(myPermission.getCanUpdate())) {
					role = "editor";
				} else {
					role = "viewer";
				}
				eventMapper.setPermission(EventPermissionSummaryDto.builder()
						.role(role)
						.canRead(Boolean.TRUE.equals(myPermission.getCanRead()))
						.canUpdate(Boolean.TRUE.equals(myPermission.getCanUpdate()))
						.canDelete(Boolean.TRUE.equals(myPermission.getCanDelete()))
						.build());
			}
		} else {
			eventMapper.setPermission(EventPermissionSummaryDto.builder()
					.role("admin")
					.canRead(true)
					.canUpdate(true)
					.canDelete(false)
					.build());
		}

		return eventMapper;
	}

	private EventDto mapEventToDto(Event event) {
		return EventDto.builder()
				.id(event.getUuid())
				.name(event.getName())
				.eventDate(event.getEventDate())
				.organizerName(event.getOrganizerName())
				.organizerId(event.getOrganizer() != null ? event.getOrganizer().getUuid() : null)
				.location(event.getLocation())
				.description(event.getDescription())
				.logoUrl(event.getLogoUrl())
				.pictureUrl(event.getPictureUrl())
				.prefixPath(event.getPrefixPath())
				.province(event.getProvince() != null ? modelMapper.map(event.getProvince(), CountryStateDto.class): null)
				.provinceId(event.getProvince() != null ? event.getProvince().getUuid() : null)
				.type(event.getType())
				.link(event.getLink())
				.startRegistrationDate(event.getStartRegistrationDate())
				.endRegistrationDate(event.getEndRegistrationDate())
				.shippingFee(event.getShippingFee())
				.generalInfoTitle(event.getGeneralInfoTitle())
				.eventTypeTitle(event.getEventTypeTitle())
				.eventPrimaryColor(event.getEventPrimaryColor())
				.eventSecondaryColor(event.getEventSecondaryColor())
				.eventFontColor(event.getEventFontColor())
				.isDraft(event.getIsDraft())
				.showChecklist(event.getShowChecklist())
				.eventConditions(event.getEventConditions().stream()
						.map(ec -> EventConditionDto.builder()
								.id(ec.getUuid())
								.description(ec.getDescription())
								.build())
						.toList())

				.eventDetails(event.getEventDetails().stream().map(ed -> EventDetailDto.builder()
						.id(ed.getUuid())
						.title(ed.getTitle())
						.detail(ed.getDetail())
						.type(ed.getType())
						.position(ed.getPosition())
						.build()).toList())

				.selectionFields(event.getSelectionFields().stream()
						.map(field -> EventSelectionFieldDto.builder()
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
								.build())
						.toList())

				.paymentTypes(event.getPaymentTypes().stream().map(pt -> PaymentTypeDto.builder()
						.id(pt.getUuid())
						.name(pt.getName())
						.endDate(pt.getEndDate())
						.build()).toList())

				.eventTypes(event.getEventTypes().stream().map(et -> EventTypeDto.builder()
						.id(et.getUuid())
						.name(et.getName())
						.eventDate(et.getEventDate())
						.quota(et.getQuota())
						.price(et.getPrice())
						.isNoShirt(et.getIsNoShirt())
						.discountNoShirt(et.getDiscountNoShirt())
						.isTeam(et.getIsTeam())

						.pricing(mapPricingWithCalculatedStartDate(et))

						.isQuotaFull(computeEventTypeQuotaFull(et))

						.ageGroups(et.getAgeGroups().stream().map(ag -> AgeGroupDto.builder()
								.id(ag.getUuid())
								.gender(ag.getGender().name())
								.minAge(ag.getMinAge())
								.maxAge(ag.getMaxAge())
								.position(ag.getPosition())
								.build()).toList())

						.selectionFields(et.getSelectionFields().stream().map(f -> EventSelectionFieldDto.builder()
								.id(f.getUuid())
								.title(f.getTitle())
								.titleEn(f.getTitleEn())
								.type(f.getType())
								.required(f.isRequired())
								.options(f.getOptions().stream().map(o -> EventSelectionOptionDto.builder()
										.id(o.getUuid())
										.value(o.getValue())
										.valueEn(o.getValueEn())
										.inputType(o.getInputType())
										.position(o.getPosition())
										.build()).toList())
								.build()).toList())
						.build()).toList())
				.shirtTypes(event.getShirtTypes().stream().map(st -> ShirtTypeDto.builder()
						.id(st.getUuid())
						.name(st.getName())
						.description(st.getDescription())
						.shirtSizes(st.getShirtSizes().stream()
								.map(sz -> ShirtSizeDto.builder()
										.id(sz.getUuid())
										.name(sz.getName())
										.chestSize(sz.getChestSize())
										.lengthSize(sz.getLengthSize())
										.build())
								.toList())
						.build()).toList())
				.build();
	}

	private List<PricingDto> mapPricingWithCalculatedStartDate(EventType eventType) {
		List<Pricing> pricingList = eventType.getPricing();
		OffsetDateTime startDate = eventType.getEvent() != null ? eventType.getEvent().getStartRegistrationDate() : null;

		if (pricingList == null || pricingList.isEmpty())
			return List.of();

		List<Pricing> sortedList = pricingList.stream()
				.sorted(Comparator.comparing(p -> Optional.ofNullable(p.getPaymentType())
						.map(PaymentType::getEndDate)
						.orElse(OffsetDateTime.MIN)))
				.toList();

		Map<String, Long> usedQuotaMap = new java.util.HashMap<>();
		List<PricingAvailabilityProjection> allPricings = pricingRepository.findAllPricingsWithQuota(eventType.getId());
		for (PricingAvailabilityProjection p : allPricings) {
			usedQuotaMap.put(p.getPricingUuid(), p.getUsedQuota());
		}

		List<PricingDto> result = new ArrayList<>();
		OffsetDateTime prevEndDate = null;

		for (Pricing pricing : sortedList) {
			OffsetDateTime endDate = Optional.ofNullable(pricing.getPaymentType())
					.map(PaymentType::getEndDate)
					.orElse(null);

			Long usedCount = usedQuotaMap.getOrDefault(pricing.getUuid(), 0L);
			boolean isFull = pricing.getQuota() != null && usedCount >= pricing.getQuota();

			PricingDto dto = PricingDto.builder()
					.id(pricing.getUuid())
					.price(pricing.getPrice())
					.quota(pricing.getQuota())
					.paymentTypeId(Optional.ofNullable(pricing.getPaymentType()).map(PaymentType::getUuid).orElse(null))
					.paymentName(Optional.ofNullable(pricing.getPaymentType()).map(PaymentType::getName).orElse(null))
					.startDate(prevEndDate != null ? prevEndDate.plusDays(1) : startDate)
					.endDate(endDate)
					.isQuotaFull(isFull)
					.build();

			result.add(dto);
			prevEndDate = endDate;
		}

		return result;
	}

	private Boolean computeEventTypeQuotaFull(EventType eventType) {
		Integer quota = eventType.getQuota();
		if (quota == null) return false;
		Long registeredCount = orderDetailRepository.countRegisteredByEventTypeId(eventType.getId());
		return registeredCount >= quota;
	}

	private <T, D> void mergeChild(List<T> entityList, List<D> dtoList, BiFunction<D, Map<String, T>, T> mapFunc) {
		Map<String, T> existingMap = entityList.stream()
				.filter(e -> e instanceof StandardFields standardFields && standardFields.getUuid() != null)
				.collect(Collectors.toMap(e -> ((StandardFields) e).getUuid(), e -> e));

		List<T> result = dtoList.stream()
				.map(dto -> mapFunc.apply(dto, existingMap))
				.toList();

		entityList.clear();
		entityList.addAll(result);
	}

	@Override
	public List<EventTypeAvailabilityResponse> getEventTypesAvailability(String eventId) {
		Event event = eventRepository.findByLinkOrUuid(eventId)
				.orElseThrow(() -> new ResourceNotFoundException("Event not found"));

		OffsetDateTime now = OffsetDateTime.now();
		OffsetDateTime startDate = event.getStartRegistrationDate();
		OffsetDateTime endDate = event.getEndRegistrationDate();

		if (startDate != null && now.isBefore(startDate)) {
			throw new ResourceNotFoundException("Registration has not started yet");
		}

		if (endDate != null && now.isAfter(endDate)) {
			throw new ResourceNotFoundException("Registration is closed");
		}

		List<EventType> eventTypes = event.getEventTypes();

		return eventTypes.stream().map(eventType -> {
			Integer eventTypeQuota = eventType.getQuota();

			List<PricingAvailabilityProjection> availablePricingList = pricingRepository.findAvailablePricingWithQuota(eventType.getId());

			Long registeredCount = orderDetailRepository.countRegisteredByEventTypeId(eventType.getId());

			java.math.BigDecimal currentPrice;
			String pricingId;
			String paymentName;
			Integer totalQuota;
			Integer availableQuota;
			boolean isSpecialPrice;

			if (!availablePricingList.isEmpty()) {
				PricingAvailabilityProjection pricing = availablePricingList.get(0);
				currentPrice = pricing.getPrice();
				pricingId = pricing.getPricingUuid();
				paymentName = pricing.getPaymentName();
				totalQuota = pricing.getQuota();
				availableQuota = pricing.getQuota() - pricing.getUsedQuota().intValue();
				isSpecialPrice = true;
			} else {
				currentPrice = eventType.getPrice();
				pricingId = null;
				paymentName = "Standard";
				Long sumActivePricingQuota = pricingRepository.sumActivePricingQuotaByEventTypeId(eventType.getId());
				Long expiredOrStandardUsed = orderDetailRepository.countByEventTypeIdWithExpiredOrNullPricing(eventType.getId());
				totalQuota = eventTypeQuota != null 
						? eventTypeQuota - sumActivePricingQuota.intValue()
						: null;
				availableQuota = totalQuota != null 
						? totalQuota - expiredOrStandardUsed.intValue() 
						: null;
				isSpecialPrice = false;
			}

			return EventTypeAvailabilityResponse.builder()
					.eventTypeId(eventType.getUuid())
					.pricingId(pricingId)
					.name(eventType.getName())
					.eventTypeQuota(eventTypeQuota)
					.totalQuota(totalQuota)
					.availableQuota(availableQuota != null ? Math.max(availableQuota, 0) : null)
					.registeredCount(registeredCount.intValue())
					.isAvailable(availableQuota != null && availableQuota > 0)
					.currentPrice(currentPrice)
					.paymentName(paymentName)
					.isSpecialPrice(isSpecialPrice)
					.build();
		}).toList();
	}

}
