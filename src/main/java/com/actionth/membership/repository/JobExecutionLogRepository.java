package com.actionth.membership.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.JobExecutionLog;

@Repository
public interface JobExecutionLogRepository extends JpaRepository<JobExecutionLog, Long> {

    @Query("SELECT j FROM JobExecutionLog j ORDER BY j.firedTime DESC")
    List<JobExecutionLog> findTop50ByOrderByFiredTimeDesc();

    @Query("SELECT j FROM JobExecutionLog j WHERE j.jobName = :jobName ORDER BY j.firedTime DESC")
    List<JobExecutionLog> findByJobNameOrderByFiredTimeDesc(String jobName);
}
