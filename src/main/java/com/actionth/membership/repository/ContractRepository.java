package com.actionth.membership.repository;

import com.actionth.membership.model.Contract;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ContractRepository extends JpaRepository<Contract, Integer>, JpaSpecificationExecutor<Contract> {
    Optional<Contract> findByUuid(String uuid);

    @Query(value = "SELECT c.runNo FROM contract c WHERE c.runNo LIKE CONCAT('QT', :year, '%') ORDER BY c.runNo DESC LIMIT 1", nativeQuery = true)
    String findLastRunNo(@Param("year") String year);

}
