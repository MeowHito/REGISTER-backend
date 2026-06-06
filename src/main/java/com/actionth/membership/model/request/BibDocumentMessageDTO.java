package com.actionth.membership.model.request;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BibDocumentMessageDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String participantUuid;
    private String logo; // ใช้ URL string แทน File/Image
}
