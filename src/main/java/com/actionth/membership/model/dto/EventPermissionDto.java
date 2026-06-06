package com.actionth.membership.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventPermissionDto {

    private String id;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String eventId;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String userId;
    
    private String firstName;
    private String lastName;
    private String email;
    private String companyName;
    private String role;

    private Boolean canRead;
    private Boolean canUpdate;
    private Boolean canDelete;

}

