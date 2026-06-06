package com.actionth.membership.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardOrganizerDTO {
    
    private String organizerId;
    private String organizerName;
    private List<DashboardEventDTO> events;

}
