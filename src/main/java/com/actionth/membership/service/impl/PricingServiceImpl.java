package com.actionth.membership.service.impl;

import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.stereotype.Service;

import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.EventType;
import com.actionth.membership.model.Pricing;
import com.actionth.membership.model.dto.AvailablePricingResponse;
import com.actionth.membership.projection.PricingAvailabilityProjection;
import com.actionth.membership.repository.EventTypeRepository;
import com.actionth.membership.repository.OrderDetailRepository;
import com.actionth.membership.repository.PricingRepository;
import com.actionth.membership.service.PricingService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class PricingServiceImpl implements PricingService {

	private final PricingRepository pricingRepository;
	private final OrderDetailRepository orderDetailRepository;
	private final EventTypeRepository eventTypeRepository;

	@Override
	public Boolean checkQuota(String pricingId) {
		Pricing pricing = pricingRepository.findByUuid(pricingId)
            .orElseThrow(() -> new ResourceNotFoundException("Pricing not found"));

		Long usedQuota = orderDetailRepository.countByPricingIdAndPaymentStatus(pricingId);

		return usedQuota < pricing.getQuota();
	}

	@Override
	public AvailablePricingResponse getAvailablePricingByEventType(String eventTypeId) {
		EventType eventType = eventTypeRepository.findByUuid(eventTypeId)
				.orElseThrow(() -> new ResourceNotFoundException("EventType not found"));

		List<PricingAvailabilityProjection> availablePricingList = pricingRepository.findAvailablePricingWithQuota(eventType.getId());
		
		if (!availablePricingList.isEmpty()) {
			PricingAvailabilityProjection availablePricing = availablePricingList.get(0);
			
			Pricing pricingEntity = pricingRepository.findByUuid(availablePricing.getPricingUuid())
					.orElseThrow(() -> new ResourceNotFoundException("Pricing not found"));
			
			OffsetDateTime startDate = calculateStartDate(eventType, pricingEntity);
			Integer availableQuota = availablePricing.getQuota() - availablePricing.getUsedQuota().intValue();
			
			return AvailablePricingResponse.builder()
					.id(availablePricing.getPricingUuid())
					.price(availablePricing.getPrice())
					.paymentName(availablePricing.getPaymentName())
					.startDate(startDate)
					.endDate(pricingEntity.getPaymentType() != null 
							? pricingEntity.getPaymentType().getEndDate() 
							: null)
					.isSpecialPrice(true)
					.availableQuota(availableQuota)
					.build();
		}
		
		OffsetDateTime now = OffsetDateTime.now();
		OffsetDateTime startDate = eventType.getEvent() != null 
				? eventType.getEvent().getStartRegistrationDate() 
				: null;
		OffsetDateTime endDate = eventType.getEvent() != null 
				? eventType.getEvent().getEndRegistrationDate() 
				: null;
		
		if (startDate != null && now.isBefore(startDate)) {
			throw new ResourceNotFoundException("Registration has not started yet");
		}
		
		if (endDate != null && now.isAfter(endDate)) {
			throw new ResourceNotFoundException("Registration has ended");
		}
		
		Long sumActivePricingQuota = pricingRepository.sumActivePricingQuotaByEventTypeId(eventType.getId());
		Long expiredOrStandardUsed = orderDetailRepository.countByEventTypeIdWithExpiredOrNullPricing(eventType.getId());
		Integer availableQuota = eventType.getQuota() != null 
				? eventType.getQuota() - sumActivePricingQuota.intValue() - expiredOrStandardUsed.intValue() 
				: null;
		
		if (availableQuota != null && availableQuota <= 0) {
			throw new ResourceNotFoundException("No available quota for this event type");
		}
		
		return AvailablePricingResponse.builder()
				.id(eventType.getUuid())
				.price(eventType.getPrice())
				.paymentName("Standard")
				.startDate(startDate)
				.endDate(endDate)
				.isSpecialPrice(false)
				.availableQuota(availableQuota)
				.build();
	}
	
	private OffsetDateTime calculateStartDate(EventType eventType, Pricing currentPricing) {
		List<Pricing> sortedPricing = eventType.getPricing().stream()
				.filter(p -> p.getPaymentType() != null && p.getPaymentType().getEndDate() != null)
				.sorted(Comparator.comparing(p -> p.getPaymentType().getEndDate()))
				.toList();
		
		for (int i = 0; i < sortedPricing.size(); i++) {
			if (sortedPricing.get(i).getId().equals(currentPricing.getId())) {
				if (i > 0) {
					return sortedPricing.get(i - 1).getPaymentType().getEndDate().plusDays(1);
				} else {
					return eventType.getEvent() != null 
							? eventType.getEvent().getStartRegistrationDate() 
							: null;
				}
			}
		}
		
		return eventType.getEvent() != null 
				? eventType.getEvent().getStartRegistrationDate() 
				: null;
	}

}
