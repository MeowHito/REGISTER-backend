package com.actionth.membership.model;

import java.time.OffsetDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "jobExecutionLog")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String jobName;

    @Column(nullable = false)
    private String jobGroup;

    @Column(nullable = false)
    private String triggerName;

    @Column(nullable = false)
    private String triggerGroup;

    @Column(nullable = false)
    private String instanceName;

    @Column(nullable = false)
    private OffsetDateTime firedTime;

    @Column(nullable = false)
    private OffsetDateTime scheduledTime;

    @Column
    private OffsetDateTime completedTime;

    @Column(nullable = false)
    private String status; // STARTED, COMPLETED, FAILED

    @Column(length = 2000)
    private String errorMessage;

    @Column
    private Long durationMs;

    @Column
    private Integer priority;

    @Column(nullable = false)
    private OffsetDateTime createdAt;
}
