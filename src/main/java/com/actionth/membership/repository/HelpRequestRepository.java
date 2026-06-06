package com.actionth.membership.repository;

import com.actionth.membership.model.HelpRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HelpRequestRepository extends JpaRepository<HelpRequest, Integer>, JpaSpecificationExecutor<HelpRequest> {

    List<HelpRequest> findByOrderUuidAndActiveTrue(String orderUuid);

    Optional<HelpRequest> findByUuidAndActiveTrue(String uuid);
}
