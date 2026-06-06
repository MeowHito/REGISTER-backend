package com.actionth.membership.service;

import java.util.List;
import java.util.Map;

import com.actionth.membership.model.dto.EventTypeDto;
import com.actionth.membership.model.dto.TeamClubResponse;

public interface EventTypeService {

    List<Map<String, Object>> findIdAndNameByEventId(Integer id);

    EventTypeDto getEventTypeById(String id, Boolean active);

    List<EventTypeDto> getEventTypeByEventId(String eventId, Boolean active);

    TeamClubResponse getTeamClubsByEventType(String eventTypeId, String search, Integer page, Integer limit);
}
