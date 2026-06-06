package com.actionth.membership.model;

import com.actionth.membership.model.listener.ContractListener;
import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import javax.persistence.*;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "contract")
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
@EntityListeners(ContractListener.class) // เพิ่ม Listener
public class Contract extends StandardFields {

  @Column(nullable = false, unique = true)
  private String runNo;

  @JsonBackReference("contract-event")
  @ManyToOne
  @JoinColumn(name = "eventId")
  private Event event;

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

  @Column(columnDefinition = "MEDIUMTEXT")
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
}
