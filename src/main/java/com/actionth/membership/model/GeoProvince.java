package com.actionth.membership.model;

import lombok.Data;
import org.hibernate.annotations.Immutable;
import javax.persistence.*;

@Entity
@Data
@Immutable
@Table(name = "geoProvinces")
public class GeoProvince {

    @Id
    private Integer code;

    private String nameEn;

    private String nameTh;

    private Boolean active;
}
