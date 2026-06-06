package com.actionth.membership.service.impl;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.actionth.membership.model.EventType;
import com.actionth.membership.model.dto.AgeGroupDto;
import com.actionth.membership.model.dto.EventTypeDto;
import com.actionth.membership.model.dto.PricingDto;
import com.actionth.membership.model.dto.TeamClubResponse;
import com.actionth.membership.repository.EventTypeRepository;
import com.actionth.membership.repository.OrderDetailRepository;
import com.actionth.membership.service.EventTypeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class EventTypeServiceImpl implements EventTypeService {

	private final EventTypeRepository eventTypeRepository;
	private final OrderDetailRepository orderDetailRepository;

	@Override
	public List<Map<String, Object>> findIdAndNameByEventId(Integer id) {
		return eventTypeRepository.findIdAndNameByEventId(id);
	}

	@Override
	public EventTypeDto getEventTypeById(String id, Boolean active) {
		EventType eventType = eventTypeRepository.findByUuid(id)
				.orElse(null);

		if (eventType == null)
			return null;
		if (active != null && !eventType.getActive().equals(active))
			return null;

		return mapEventTypeToDto(eventType);
	}

	@Override
	public List<EventTypeDto> getEventTypeByEventId(String eventId, Boolean active) {
		Specification<EventType> spec = Specification
				.where((root, query, cb) -> cb.equal(root.get("event").get("uuid"), eventId));

		if (active != null) {
			spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
		}

		return eventTypeRepository.findAll(spec)
				.stream()
				.map(this::mapEventTypeToDto)
				.toList();
	}

	@Override
	@Transactional(readOnly = true)
	public TeamClubResponse getTeamClubsByEventType(String eventTypeId, String search, Integer page, Integer limit) {
		String q = (search == null || search.isBlank()) ? null : search.trim();
		int safeLimit = Math.min(Math.max(limit == null ? 30 : limit, 1), 100);
		int safePage = Math.max(page == null ? 0 : page, 0);

		Pageable pageable = PageRequest.of(safePage, safeLimit);

		Slice<String> s = orderDetailRepository.findTeamClubsByEventType(eventTypeId, q, pageable);

		List<String> items = s.getContent().stream()
				.map(this::normalizeTeamClub)
				.filter(Objects::nonNull)
				.toList();

		return new TeamClubResponse(items, safePage, safeLimit, s.hasNext());
	}

	private EventTypeDto mapEventTypeToDto(EventType eventType) {
		return EventTypeDto.builder()
				.id(eventType.getUuid())
				.name(eventType.getName())
				.eventDate(eventType.getEventDate())
				.quota(eventType.getQuota())
				.price(eventType.getPrice())
				.isNoShirt(eventType.getIsNoShirt())
				.discountNoShirt(eventType.getDiscountNoShirt())

				.pricing(eventType.getPricing().stream().map(pr -> PricingDto.builder()
						.id(pr.getUuid())
						.price(pr.getPrice())
						.quota(pr.getQuota())
						.paymentTypeId(
								pr.getPaymentType() != null ? pr
										.getPaymentType()
										.getUuid() : null)
						.paymentName(pr.getPaymentType() != null ? pr
								.getPaymentType()
								.getName() : null)
						.endDate(pr.getPaymentType() != null ? pr
								.getPaymentType()
								.getEndDate() : null)
						.build()).toList())

				.ageGroups(eventType.getAgeGroups().stream().map(ag -> AgeGroupDto.builder()
						.id(ag.getUuid())
						.gender(ag.getGender().name())
						.minAge(ag.getMinAge())
						.maxAge(ag.getMaxAge())
						.position(ag.getPosition())
						.build()).toList())
				.build();
	}

	private String normalizeTeamClub(String s) {
		if (s == null)
			return null;
		String norm = s.trim().replaceAll("\\s+", " ");
		return norm.isBlank() ? null : norm;
	}
}
