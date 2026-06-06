package com.actionth.membership.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.ContractLog;

@Repository
public interface ContractLogRepository extends JpaRepository<ContractLog, Long> {

}
