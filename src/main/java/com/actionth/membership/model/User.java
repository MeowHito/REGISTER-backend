package com.actionth.membership.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;

import javax.persistence.*;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "user")
@JsonIgnoreProperties(value = { "hibernateLazyInitializer", "handler" }, ignoreUnknown = true)
@EqualsAndHashCode(callSuper = true)
public class User extends StandardFields {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = true)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @ManyToOne
    @JoinColumn(name = "roleId")
    private Role role;

    private String firstName;
    private String lastName;
    private String firstNameEn;
    private String lastNameEn;
    private String companyName;
    private String gender;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime birthDate;
    private String phone;
    private String nationality;
    private String idNo;
    private String healthIssues;
    private String bloodType;

    private String emergencyContact;
    private String emergencyRelation;
    private String emergencyPhone;

    private String prefixPath;
    private String pictureUrl;
    private String signatureUrl;

    private String address;
    private String province;
    private String amphoe;
    private String district;
    private String zipcode;

    private String shippingAddress;
    private String shippingProvince;
    private String shippingAmphoe;
    private String shippingDistrict;
    private String shippingZipcode;

    private Boolean isApprover;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @LazyCollection(LazyCollectionOption.FALSE)
    @JsonManagedReference("user-eventPermissions")
    @Builder.Default
    private List<EventPermission> eventPermissions = new ArrayList<>();
}
