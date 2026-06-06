package com.actionth.membership.model.request;

import java.io.Serializable;
import java.util.List;

import com.actionth.membership.model.dto.EmailAttachmentDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SimpleEmailRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String to;
    private String cc;
    private String subject;
    private String body;
    private String orderId;
    private List<EmailAttachmentDTO> attachments;
    private String emailLogId;
}
