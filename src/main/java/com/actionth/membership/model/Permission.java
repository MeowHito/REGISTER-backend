package com.actionth.membership.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import javax.persistence.*;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "permission")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@EqualsAndHashCode(callSuper = true)
public class Permission extends StandardFields {

    @ManyToOne
    @JoinColumn(name = "menuId", nullable = false)
    private Menu menu;

    @ManyToOne
    @JoinColumn(name = "roleId", nullable = false)
    private Role role;

    @Column(name = "canRead", nullable = false)
    private Boolean canRead;

    @Column(name = "canCreate", nullable = false)
    private Boolean canCreate;

    @Column(name = "canUpdate", nullable = false)
    private Boolean canUpdate;

    @Column(name = "canDelete", nullable = false)
    private Boolean canDelete;
}
