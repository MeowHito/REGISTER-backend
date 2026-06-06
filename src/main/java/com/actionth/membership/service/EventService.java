package com.actionth.membership.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.actionth.membership.model.dto.EventDto;
import com.actionth.membership.model.dto.EventTypeAvailabilityResponse;
import com.actionth.membership.model.dto.EventViewDto;
import com.actionth.membership.model.request.GeneralRequest;

public interface EventService {

    Page<EventViewDto> findAll(GeneralRequest generalRequest);

    EventDto getEventByUuid(String uuid);

    EventDto getEventByLinkOrUuid(String uuid);
    
    List<OffsetDateTime> getAllEventDatesInMonth(OffsetDateTime date);

    EventDto createEvent(EventDto dto);

    EventDto updateEvent(EventDto dto);

    void updateStatus(EventDto dto);

    void deleteEvent(String uuid, String mode);

    Map<String, Object> findIdAndNameByUuid(String id);
    
    List<EventDto> findEventByOrganizer(String id);
    
    List<EventDto> findEventByPermission(String id);
    
    List<EventTypeAvailabilityResponse> getEventTypesAvailability(String eventId);

}
