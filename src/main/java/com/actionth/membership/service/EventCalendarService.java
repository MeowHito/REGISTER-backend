package com.actionth.membership.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.actionth.membership.model.EventCalendar;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.request.EventCalendarDTO;
import com.actionth.membership.model.request.GeneralRequest;

public interface EventCalendarService {

    List<Map<String, Object>> getAllEvents();

    Page<EventCalendarDTO> findAll(PagingData pagingData);
    
    EventCalendarDTO findByUuid(String uuid);

    void createEventCalendar(EventCalendarDTO eventCalendarDTO);

    EventCalendar updateEventCalendar(EventCalendarDTO dto);

    EventCalendar updateApproveStatus(EventCalendarDTO dto);

    /** @return the entries that were updated, so the caller can notify their submitters. */
    List<EventCalendar> updateApproveStatusBulk(List<String> uuids, Boolean isApproved, String rejectReason);

    int updateMajor(List<String> uuids, boolean isMajor);

    Page<EventCalendarDTO> getApprovedEvents(GeneralRequest generalRequest);

    long countPendingEvents();

    void deleteEvent(String uuid);

    int deleteEvents(List<String> uuids);

}
