package com.actionth.membership.model.request;

import com.actionth.membership.model.PagingData;

import lombok.Data;

@Data
public class GeneralRequest {
    private PagingData paging;
    private Boolean active;
    private Integer createdBy;

}
