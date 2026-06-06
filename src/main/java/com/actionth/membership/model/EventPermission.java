package com.actionth.membership.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.AllArgsConstructor;
import javax.persistence.*;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "eventPermission")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@EqualsAndHashCode(callSuper = true)
public class EventPermission extends StandardFields {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "eventId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference("event-eventPermissions")
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "userId", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonBackReference("user-eventPermissions")
    private User user;

    @Column(name = "role", nullable = false, length = 20)
    private String role;

    @Column(name = "canRead", nullable = false)
    private Boolean canRead;

    @Column(name = "canUpdate", nullable = false)
    private Boolean canUpdate;

    @Column(name = "canDelete", nullable = false)
    private Boolean canDelete;

    public void syncBooleanFlags() {
        if (this.role == null) {
            this.role = "viewer";
        }
        switch (this.role) {
            case "owner":
                this.canRead = true;
                this.canUpdate = true;
                this.canDelete = true;
                break;
            case "admin", "editor":
                this.canRead = true;
                this.canUpdate = true;
                this.canDelete = false;
                break;
            case "viewer":
            default:
                this.canRead = true;
                this.canUpdate = false;
                this.canDelete = false;
                break;
        }
    }

}
