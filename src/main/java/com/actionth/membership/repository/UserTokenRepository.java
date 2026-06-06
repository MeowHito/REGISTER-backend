package com.actionth.membership.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.UserToken;

@Repository
public interface UserTokenRepository extends JpaRepository<UserToken, Integer> {

    UserToken findByUserId(Integer userId);

    Optional<UserToken> findByUuid(String uuid);

    @Query(value = """
            SELECT COUNT(*)
            FROM userToken a
            WHERE a.uuid = :uuid
            AND a.active = true
            AND a.createdTime >= DATE_SUB(NOW(), INTERVAL 1 DAY)
            """, nativeQuery = true)
    int countByUuidAndActiveWithinOneDay(@Param("uuid") String uuid);

}
