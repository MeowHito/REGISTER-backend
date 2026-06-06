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
@Table(name = "role")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@EqualsAndHashCode(callSuper = true)
public class Role extends StandardFields {

    @OneToMany(mappedBy = "role", cascade = CascadeType.ALL)
    @Builder.Default
    private List<Permission> permissions = new ArrayList<>();

    private String roleType;

    @Column(unique = true)
    private String role;
}
