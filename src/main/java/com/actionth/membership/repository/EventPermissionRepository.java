package com.actionth.membership.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.EventPermission;

@Repository
public interface EventPermissionRepository extends JpaRepository<EventPermission, Integer> {

    Optional<EventPermission> findByUuid(String uuid);

    @Query("SELECT ep FROM EventPermission ep WHERE ep.event.uuid = :eventUuid AND ep.active = true")
    Page<EventPermission> findActiveByEventUuid(@Param("eventUuid") String eventUuid, Pageable pageable);

    @Query("SELECT ep FROM EventPermission ep WHERE ep.event.uuid = :eventUuid AND ep.user.uuid = :userUuid AND ep.active = true")
    Optional<EventPermission> findActiveByEventUuidAndUserUuid(@Param("eventUuid") String eventUuid,
            @Param("userUuid") String userUuid);

    @Query("SELECT ep FROM EventPermission ep WHERE ep.event.uuid = :eventUuid AND ep.user.uuid = :userUuid")
    Optional<EventPermission> findByEventUuidAndUserUuid(@Param("eventUuid") String eventUuid,
            @Param("userUuid") String userUuid);
}
