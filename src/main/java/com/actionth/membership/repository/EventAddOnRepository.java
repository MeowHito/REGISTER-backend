package com.actionth.membership.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.EventAddOn;

@Repository
public interface EventAddOnRepository extends JpaRepository<EventAddOn, Integer> {

    Optional<EventAddOn> findByUuid(String uuid);
}
