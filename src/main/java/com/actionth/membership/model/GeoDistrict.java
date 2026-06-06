package com.actionth.membership.model;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import javax.persistence.*;

@Entity
@Data
@Immutable
@Table(name = "geoDistricts")
public class GeoDistrict {

    @Id
    private Integer code;

    private Integer provinceCode;

    private String nameEn;

    private String nameTh;

    private Boolean active;
}
