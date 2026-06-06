package com.actionth.membership.repository;

import com.actionth.membership.model.AppErrorLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppErrorLogRepository extends JpaRepository<AppErrorLog, Long> {

    Optional<AppErrorLog> findByLogId(String logId);

    List<AppErrorLog> findByLevelAndCreatedTimeAfterOrderByCreatedTimeDesc(
            String level, OffsetDateTime after);

    List<AppErrorLog> findByContextAndCreatedTimeAfterOrderByCreatedTimeDesc(
            String context, OffsetDateTime after);

    List<AppErrorLog> findByUserIdOrderByCreatedTimeDesc(String userId);

    List<AppErrorLog> findByHttpStatusBetweenAndCreatedTimeAfterOrderByCreatedTimeDesc(
            Integer statusMin, Integer statusMax, OffsetDateTime after);

    @Query("SELECT f FROM AppErrorLog f WHERE f.level = 'error' AND f.createdTime > :since ORDER BY f.createdTime DESC")
    List<AppErrorLog> findRecentErrors(@Param("since") OffsetDateTime since);

    @Query("SELECT f.context, COUNT(f) FROM AppErrorLog f WHERE f.level = 'error' AND f.createdTime > :since GROUP BY f.context ORDER BY COUNT(f) DESC")
    List<Object[]> countErrorsByContext(@Param("since") OffsetDateTime since);

    @Query("SELECT COUNT(f) FROM AppErrorLog f WHERE f.level = 'error' AND f.createdTime > :since")
    long countRecentErrors(@Param("since") OffsetDateTime since);
}
