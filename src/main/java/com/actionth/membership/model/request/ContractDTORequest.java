package com.actionth.membership.model.request;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ContractDTORequest {

    private String id;
    private String runNo;
    private String eventId;
    private String eventName;

    private String organizerName;
    private String idNo;
    private String taxNo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime endDate;

    private String bankbook;
    private String accountNo;
    private String accountName;
    private String email;
    private String tel;
    private String address;
    private String province;
    private String amphoe;
    private String district;
    private String zipcode;
    private String prefixPath;
    private String contractPath;
    private String certificatePath;
    private String idCardPath;
    private String bankAccountPath;
    private String powerOfAttorneyPath;
    private String pp20Path;
    private String otherDocumentPath;
    private String remark;
    private String tempContractPath;

    private String detail;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime contractDate;

    private String providerSignature;
    private String providerSeal;
    private String providerName;
    private String providerPosition;
    private String customerSignature;
    private String customerSeal;
    private String customerName;
    private String customerPosition;
    private Boolean isUploadContract;

    private String thumbCustomerSignaturePath;

    private String tempCertificatePath;
    private String tempIdCardPath;
    private String tempBankAccountPath;
    private String tempPowerOfAttorneyPath;
    private String tempPp20Path;
    private String tempOtherDocumentPath;
    
    private List<MediaFileDTO> mediaFiles;
}
