package com.actionth.membership.repository;

import com.actionth.membership.model.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    @Query("""
            SELECT n FROM Notification n
            WHERE n.recipient.id = :recipientId AND n.active = true
            ORDER BY n.id DESC
            """)
    List<Notification> findLatestByRecipient(@Param("recipientId") Integer recipientId, Pageable pageable);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.recipient.id = :recipientId AND n.active = true AND n.readAt IS NULL
            """)
    long countUnreadByRecipient(@Param("recipientId") Integer recipientId);

    @Query("""
            SELECT n FROM Notification n
            WHERE n.uuid = :uuid AND n.recipient.id = :recipientId AND n.active = true
            """)
    Optional<Notification> findOwned(@Param("uuid") String uuid, @Param("recipientId") Integer recipientId);

    @Modifying
    @Query("""
            UPDATE Notification n SET n.readAt = :readAt
            WHERE n.recipient.id = :recipientId AND n.readAt IS NULL
            """)
    int markAllRead(@Param("recipientId") Integer recipientId, @Param("readAt") OffsetDateTime readAt);
}
