package com.actionth.membership.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.ShirtType;
import com.actionth.membership.projection.UuidIdProjection;

@Repository
public interface ShirtTypeRepository extends JpaRepository<ShirtType, Integer> {

    @Query(value = "SELECT * FROM shirtType WHERE active = 1", nativeQuery = true)
    List<ShirtType> findAllActiveShirtTypes();

    Optional<ShirtType> findByUuid(String uuid);

     @Query("SELECT e.id FROM ShirtType e WHERE e.uuid = :uuid")
     Optional<Integer> findIdByUuid(@Param("uuid") String uuid);

     @Query("SELECT e.uuid AS uuid, e.id AS id FROM ShirtType e WHERE e.uuid IN :uuids")
    List<UuidIdProjection> findAllByUuidIn(@Param("uuids") Set<String> uuids);
}
