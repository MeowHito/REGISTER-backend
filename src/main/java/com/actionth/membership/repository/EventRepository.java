package com.actionth.membership.repository;

import com.actionth.membership.model.CountryState;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.projection.EventOrganizerProjection;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventRepository extends JpaRepository<Event, Integer>, JpaSpecificationExecutor<Event> {
    // Additional query methods can be defined here

    Optional<Event> findByUuid(String uuid);

    @Query("SELECT e FROM Event e WHERE e.link = :uuid OR e.uuid = :uuid")
    Optional<Event> findByLinkOrUuid(@Param("uuid") String uuid);

    List<Event> findByActiveTrueAndIsDraftFalseAndEventDateBetween(OffsetDateTime start, OffsetDateTime end);

    void deleteByUuid(String uuid);

    @Query("SELECT e.id AS id, e.name AS name FROM Event e WHERE e.uuid = :uuid")
    Map<String, Object> findIdAndNameByUuid(@Param("uuid") String uuid);

    @Query("""
            SELECT e.uuid AS uuid, e.name AS name, e.eventDate AS eventDate
            FROM Event e JOIN e.organizer o WHERE o.uuid = :uuid
            ORDER BY e.createdTime DESC
            """)
    List<EventOrganizerProjection> findEventByOrganizer(@Param("uuid") String uuid);

    @Query("""
            SELECT DISTINCT e.uuid AS uuid, e.name AS name, e.eventDate AS eventDate
            FROM Event e
            LEFT JOIN e.organizer o
            LEFT JOIN e.eventPermissions ep
            WHERE (o.uuid = :userUuid OR (ep.user.uuid = :userUuid AND ep.active = true))
            ORDER BY e.createdTime DESC
            """)
    List<EventOrganizerProjection> findEventByPermission(@Param("userUuid") String userUuid);

    @Query("SELECT c FROM CountryState c WHERE c.uuid = :uuid")
    Optional<CountryState> findCountryStateByUuid(@Param("uuid") String uuid);
}
