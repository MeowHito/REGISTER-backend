package com.actionth.membership.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.EventInvitation;

@Repository
public interface EventInvitationRepository extends JpaRepository<EventInvitation, Integer> {

    Optional<EventInvitation> findByToken(String token);

    @Query("SELECT ei FROM EventInvitation ei WHERE ei.event.uuid = :eventUuid AND ei.email = :email AND ei.status = 'pending' AND ei.active = true")
    Optional<EventInvitation> findPendingByEventUuidAndEmail(@Param("eventUuid") String eventUuid,
            @Param("email") String email);

    @Query("SELECT ei FROM EventInvitation ei WHERE ei.event.uuid = :eventUuid AND ei.status = 'pending' AND ei.active = true")
    List<EventInvitation> findPendingByEventUuid(@Param("eventUuid") String eventUuid);
}
