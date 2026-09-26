package com.actionth.membership.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.EventCalendar;
import java.time.OffsetDateTime;

@Repository
public interface EventCalendarRepository
        extends JpaRepository<EventCalendar, Integer>, JpaSpecificationExecutor<EventCalendar> {

    Optional<EventCalendar> findByUuid(String uuid);

    List<EventCalendar> findAll();

    List<EventCalendar> findByIsApprovedTrue();

    List<EventCalendar> findByActiveTrueAndIsApprovedTrueAndEventDateBetween(OffsetDateTime start, OffsetDateTime end);

    long countByIsApprovedIsNull();

    void deleteByUuid(String uuid);

    Optional<EventCalendar> findBySourceAndSourceId(String source, String sourceId);

    long countBySourceAndIsApprovedIsNull(String source);

    @Modifying
    @Transactional
    @Query("DELETE FROM EventCalendar e WHERE e.source = :source")
    int deleteBySource(@Param("source") String source);

}
