package com.actionth.membership.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContractDocumentDTO {
    /** uuid of the contract being previewed; required, the server loads and checks it. */
    private String id;
    private String runNo;
    private String contractDate;
    private String detail;
    private String customerCompany;
    private String organizer;
    private String tel;
    private String address;
    private String taxNo;
    private String event;
	private String providerSignature;
	private String providerSeal;
	private String providerName;
	private String providerPosition;
	private String customerSignature;
	private String customerSeal;
	private String customerName;
	private String customerPosition;
}