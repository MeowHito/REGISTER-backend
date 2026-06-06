package com.actionth.membership.repository;

import com.actionth.membership.model.EventType;
import com.actionth.membership.projection.UuidIdProjection;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface EventTypeRepository extends JpaRepository<EventType, Integer>, JpaSpecificationExecutor<EventType> {

    Optional<EventType> findByUuid(String uuid);

    void deleteByUuid(String uuid);

    @Query(value = """
            SELECT et.uuid, et.name
            FROM eventType et
            INNER JOIN event e ON e.id = et.eventId AND e.active = true
            WHERE e.uuid = :uuid AND et.active = true
            """, nativeQuery = true)
    List<Map<String, Object>> findEventTypeByUuid(@Param("uuid") String uuid);

    @Query("SELECT et.id AS id, et.name AS name FROM EventType et WHERE et.event.id = :eventId")
    List<Map<String, Object>> findIdAndNameByEventId(@Param("eventId") Integer eventId);

    @Query(value = "SELECT * FROM eventType WHERE active = 1", nativeQuery = true)
    List<EventType> findAllActiveEventTypes();

    @Query(value = """
            SELECT
                et.id, et.active, et.createdTime, et.updatedTime, et.uuid,
                et.capacity, et.eventDate, et.name, et.price, et.createdBy, et.updatedBy, et.eventId,
                e.location AS eventLocation,
                e.email, e.website, e.facebook, e.logoUrl, e.pictureUrl, e.contactTel, e.contactName,
                ag.minAge, ag.maxAge, et.is_no_shirt, et.discount_no_shirt
            FROM eventType et
            JOIN event e ON et.eventId = e.id
            LEFT JOIN ageGroup ag ON et.id = ag.eventTypeId
            WHERE et.active = 1  AND e.uuid = :uuid
            """, nativeQuery = true)
    List<Object[]> findActiveEventTypesWithLocationByEventUuid(@Param("uuid") String uuid);

    @Query("SELECT et FROM EventType et WHERE et.event.uuid = :eventUuid")
    List<EventType> findByEventUuid(@Param("eventUuid") String eventUuid);

    @Query("SELECT e.id FROM EventType e WHERE e.uuid = :uuid")
    Optional<Integer> findIdByUuid(@Param("uuid") String uuid);

    @Query("SELECT e.uuid AS uuid, e.id AS id FROM EventType e WHERE e.uuid IN :uuids")
    List<UuidIdProjection> findAllByUuidIn(@Param("uuids") Set<String> uuids);

}
