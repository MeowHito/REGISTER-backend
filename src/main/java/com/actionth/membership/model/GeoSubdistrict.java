package com.actionth.membership.model;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import javax.persistence.*;

@Entity
@Data
@Immutable
@Table(name = "geoSubdistricts")
public class GeoSubdistrict {

    @Id
    private Integer code;

    private Integer districtCode;

    private String nameEn;

    private String nameTh;

    private String postalCode;

    private Boolean active;
}
