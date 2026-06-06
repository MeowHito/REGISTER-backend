package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardEventDTO {

    private String eventId;
    private String eventName;
    private String eventDate;
    
}
