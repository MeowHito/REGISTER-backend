package com.actionth.membership.repository;

import com.actionth.membership.model.Announcement;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnnouncementRepository
                extends JpaRepository<Announcement, Integer>, JpaSpecificationExecutor<Announcement> {
        Optional<Announcement> findByUuid(String uuid);

        void deleteByUuid(String uuid);

        @Query(value = """
                        SELECT COUNT(*)
                        FROM announcement a
                        JOIN event e ON e.id = a.eventId
                        WHERE e.uuid = :uuid
                        AND a.active = true
                        """, nativeQuery = true)
        int countByEventUuid(@Param("uuid") String uuid);

        @Query(value = """
                        SELECT COUNT(*)
                        FROM announcement a
                        JOIN event e ON e.id = a.eventId
                        WHERE e.uuid = :eventUuid
                        AND a.uuid != :announcementUuid
                        AND a.active = true
                        """, nativeQuery = true)
        int countByEventUuidExcludingUuid(
                        @Param("eventUuid") String eventUuid,
                        @Param("announcementUuid") String announcementUuid);

        @Query("SELECT COUNT(a) FROM Announcement a WHERE a.isRead IS NULL OR a.isRead = false")
        long countUnread();

        @Query(value = """
                        SELECT COUNT(*)
                        FROM announcement a
                        JOIN event e ON e.id = a.eventId
                        WHERE (a.isRead IS NULL OR a.isRead = false)
                        AND (
                            e.organizerId = :userId
                            OR EXISTS (
                                SELECT 1 FROM eventPermission ep
                                WHERE ep.eventId = e.id
                                AND ep.userId = :userId
                                AND ep.active = true
                            )
                        )
                        """, nativeQuery = true)
        long countUnreadByUser(@Param("userId") Integer userId);

}
