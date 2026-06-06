package com.actionth.membership.model.request;

import lombok.Data;

@Data
public class HelpRequestStatusRequest {
    private String uuid;
    private String status;
    private String adminNote;
}
