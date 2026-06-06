package com.actionth.membership.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "menu")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@EqualsAndHashCode(callSuper = true)
public class Menu extends StandardFields {

    private String title;
    private String path;
    private String icon;
    private Boolean isDisplay;
    private Boolean disabled;
    private Boolean isNoti;
    private String badgeKey;
    private Integer position;

    @OneToMany(mappedBy = "menu", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Permission> permissions = new ArrayList<>();
}
