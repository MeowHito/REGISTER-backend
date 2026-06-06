package com.actionth.membership.repository;

import com.actionth.membership.model.SystemAnnouncement;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemAnnouncementRepository
        extends JpaRepository<SystemAnnouncement, Integer>, JpaSpecificationExecutor<SystemAnnouncement> {

    Optional<SystemAnnouncement> findByUuid(String uuid);

    void deleteByUuid(String uuid);

    @Query("SELECT sa FROM SystemAnnouncement sa WHERE sa.active = true " +
            "AND (sa.startDate IS NULL OR sa.startDate <= :now) " +
            "AND (sa.endDate IS NULL OR sa.endDate >= :now) " +
            "ORDER BY sa.createdTime DESC")
    List<SystemAnnouncement> findAllCurrentlyActive(@Param("now") OffsetDateTime now);
}
