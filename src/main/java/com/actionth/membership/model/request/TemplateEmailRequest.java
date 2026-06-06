package com.actionth.membership.model.request;

import java.io.Serializable;
import java.util.Map;

import lombok.Data;

@Data
public class TemplateEmailRequest implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String to;
    private String cc;
    private String subject;
    private String orderId;
    private String templateName;
    private Map<String, Object> variables;
}
