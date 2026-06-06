package com.actionth.membership.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.criteria.Predicate;

import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventCalendar;
import com.actionth.membership.model.EventType;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.EventCalendarDTO;
import com.actionth.membership.model.request.GeneralRequest;
import com.actionth.membership.repository.EventCalendarRepository;
import com.actionth.membership.repository.EventRepository;
import com.actionth.membership.service.EventCalendarService;
import com.actionth.membership.utils.SearchPredicateBuilder;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class EventCalendarServiceImpl implements EventCalendarService {

    private final EventRepository eventRepository;
    private final EventCalendarRepository eventCalendarRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<Map<String, Object>> getAllEvents() {
        List<Event> events = eventRepository.findAll();
        List<Map<String, Object>> eventCardData = new ArrayList<>();

        for (Event event : events) {
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("id", event.getUuid());
            eventData.put("name", event.getName());
            eventData.put("eventDate", event.getEventDate().toString());
            eventData.put("location", event.getLocation());

            List<String> eventTypeNames = event.getEventTypes().stream()
                    .map(EventType::getName)
                    .toList();
            eventData.put("eventTypes", eventTypeNames);

            eventCardData.add(eventData);
        }
        return eventCardData;
    }

    @Override
    public Page<EventCalendarDTO> findAll(PagingData pagingData) {
        Sort sort = Sort.by(Sort.Direction.DESC, "id");
        if (pagingData.getSortField() != null && pagingData.getSortDirection() != null) {
            sort = Sort.by(
                    "DESC".equalsIgnoreCase(pagingData.getSortDirection()) ? Sort.Direction.DESC : Sort.Direction.ASC,
                    pagingData.getSortField());
        }

        Pageable pageable = PageRequest.of(pagingData.getPage(), pagingData.getSize(), sort);

        Specification<EventCalendar> spec = (root, query, criteriaBuilder) -> {
            query.distinct(true);
            return null;
        };

        if (pagingData.getSearchField() != null && pagingData.getSearchText() != null) {
            spec = spec.and((root, query, criteriaBuilder) -> criteriaBuilder
                    .like(root.get(pagingData.getSearchField()), "%" + pagingData.getSearchText() + "%"));
        }
        Page<EventCalendar> eventCalendars = eventCalendarRepository.findAll(spec, pageable);

        return eventCalendars.map(eventCalendar -> {
            EventCalendarDTO dto = modelMapper.map(eventCalendar, EventCalendarDTO.class);
            dto.setEventId(eventCalendar.getUuid());
            return dto;
        });
    }

    @Override
    public EventCalendarDTO findByUuid(String uuid) {
        EventCalendar event = eventCalendarRepository.findByUuid(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("EventCalendar not found with uuid: " + uuid));
        EventCalendarDTO dto = modelMapper.map(event, EventCalendarDTO.class);
        dto.setEventId(event.getUuid());
        return dto;
    }

    @Override
    public void createEventCalendar(EventCalendarDTO eventCalendarDTO) {
        EventCalendar event = modelMapper.map(eventCalendarDTO, EventCalendar.class);
        event.setIsApproved(null);
        eventCalendarRepository.save(event);
    }

    @Override
    public EventCalendar updateEventCalendar(EventCalendarDTO dto) {
        EventCalendar event = eventCalendarRepository.findByUuid(dto.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("EventCalendar not found"));
        event.setEventName(dto.getEventName());
        event.setEventDate(dto.getEventDate());
        event.setLocation(dto.getLocation());
        event.setExtraDetail(dto.getExtraDetail());
        event.setEventType(dto.getEventType());
        event.setSubmitterName(dto.getSubmitterName());
        event.setEmail(dto.getEmail());
        event.setPhone(dto.getPhone());
        return eventCalendarRepository.save(event);
    }

    @Override
    public EventCalendar updateApproveStatus(EventCalendarDTO dto) {
        EventCalendar event = eventCalendarRepository.findByUuid(dto.getEventId())
                .orElseThrow(() -> new ResourceNotFoundException("EventCalendar not found"));
        event.setIsApproved(dto.getIsApproved());
        if (dto.getIsApproved() != null && !dto.getIsApproved()) {
            event.setRejectReason(dto.getRejectReason());
        } else {
            event.setRejectReason(null);
        }
        return eventCalendarRepository.save(event);
    }

    @Override
    public Page<EventCalendarDTO> getApprovedEvents(GeneralRequest generalRequest) {
        Specification<EventCalendar> activeSpec = (root, query, cb) -> cb.and(
                cb.equal(root.get("active"), true),
                cb.equal(root.get("isApproved"), true));

        PagingData paging = generalRequest.getPaging();

        Specification<EventCalendar> searchSpec = (root, query, cb) -> {
            List<Predicate> predicates = SearchPredicateBuilder.build(cb, root, paging, null);
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Specification<EventCalendar> combinedSpec = Specification.<EventCalendar>where(activeSpec).and(searchSpec);

        if (paging == null) {
            List<EventCalendarDTO> dtos = eventCalendarRepository.findAll(combinedSpec)
                    .stream()
                    .map(event -> {
                        EventCalendarDTO dto = modelMapper.map(event, EventCalendarDTO.class);
                        dto.setEventId(event.getUuid());
                        return dto;
                    })
                    .toList();
            return new PageImpl<>(dtos);
        } else {
            Pageable pageable = PageRequest.of(
                    paging.getPage(),
                    paging.getSize(),
                    paging.getSortField() != null
                            ? Sort.by(Sort.Direction.fromString(paging.getSortDirection()), paging.getSortField())
                            : Sort.unsorted());

            return eventCalendarRepository.findAll(combinedSpec, pageable)
                    .map(event -> {
                        EventCalendarDTO dto = modelMapper.map(event, EventCalendarDTO.class);
                        dto.setEventId(event.getUuid());
                        return dto;
                    });
        }
    }

    @Override
    public long countPendingEvents() {
        return eventCalendarRepository.countByIsApprovedIsNull();
    }

    @Override
    public void deleteEvent(String uuid) {
        EventCalendar event = eventCalendarRepository.findByUuid(uuid)
                .orElseThrow(() -> new RuntimeException("Event not found"));
        eventCalendarRepository.delete(event);
    }

}
