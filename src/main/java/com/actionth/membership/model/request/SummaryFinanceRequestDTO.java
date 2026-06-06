package com.actionth.membership.model.request;

import lombok.Data;

@Data
public class SummaryFinanceRequestDTO {

    private String id;
    private String startDate;
    private String endDate;
    private String remark;

}
