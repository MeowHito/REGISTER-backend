package com.actionth.membership.service;

import com.actionth.membership.model.dto.DashboardOrganizerDTO;
import com.actionth.membership.model.dto.DashboardOverviewDTO;
import com.actionth.membership.model.dto.DashboardRegistrationDTO;
import com.actionth.membership.model.dto.EventDto;

import java.util.List;

public interface DashboardService {

        List<EventDto> getAllEvents();

        DashboardOverviewDTO getDashboardOverview(String eventUuid);

        List<DashboardOrganizerDTO> getEventsAndOrganizers();

        DashboardRegistrationDTO getDashboardRegistration(String eventUuid);

}
