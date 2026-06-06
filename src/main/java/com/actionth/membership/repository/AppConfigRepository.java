package com.actionth.membership.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.AppConfig;

@Repository
public interface AppConfigRepository extends JpaRepository<AppConfig, Integer> {
        Optional<AppConfig> findFirstByName(String name);
}
