package com.actionth.membership.model;

import javax.persistence.*;
import org.springframework.format.annotation.DateTimeFormat;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.OffsetDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@SuperBuilder
@EntityListeners(AuditingEntityListener.class)
@MappedSuperclass
@NoArgsConstructor
@AllArgsConstructor
public class StandardFields {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  protected Integer id;

  @Column(nullable = false, unique = true, length = 36)
  protected String uuid;

  @Builder.Default
  protected Boolean active = true;

  @CreatedDate
  @Column(updatable = false)
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  protected OffsetDateTime createdTime;

  @LastModifiedDate
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  protected OffsetDateTime updatedTime;

  @CreatedBy
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @ManyToOne
  @JoinColumn(name = "createdBy")
  protected User createdBy;

  @LastModifiedBy
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @ManyToOne
  @JoinColumn(name = "updatedBy")
  protected User updatedBy;

  @PrePersist
  protected void onPrePersist() {
    if (this.uuid == null) {
      this.uuid = UUID.randomUUID().toString();
    }
  }

}