package com.actionth.membership.model.request;

import lombok.Data;

@Data
public class HelpRequestRequest {
    private String orderUuid;
    private String message;
    private String attachmentUrl;
}
