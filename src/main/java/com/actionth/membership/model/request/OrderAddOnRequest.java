package com.actionth.membership.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OrderAddOnRequest {
    /** uuid of the EventAddOn being bought. */
    private String addOnId;

    /**
     * Index into {@link OrderRequest#getOrderDetails()} identifying which
     * runner this add-on is for. Null for a per-order add-on. Sent as an index
     * because the runners have no uuid until the order is saved.
     */
    private Integer applicantIndex;

    private Integer qty;
    private String note;
}
