package com.actionth.membership.model.dto;

import java.time.OffsetDateTime;

import org.springframework.format.annotation.DateTimeFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserViewDto {
    private String id;
    private String email;
    private String firstName;
    private String lastName;
    private String firstNameEn;
    private String lastNameEn;
    private String companyName;
    private String gender;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime birthDate;

    private String tel;
    private String prefixPath;
    private String pictureUrl;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String role;
   
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String roleType;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String thumbPictureUrl;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Boolean active;
}
