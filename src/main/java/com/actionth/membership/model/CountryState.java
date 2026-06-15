package com.actionth.membership.model;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import javax.persistence.*;

@Entity
@Data
@Immutable
@Table(name = "countryState")
public class CountryState {
    @Id
    private Integer id;
    private String uuid;
    private String countryEn;
    private String countryLocal;
    private String stateEn;
    private String stateLocal;
    private String stateType;
}