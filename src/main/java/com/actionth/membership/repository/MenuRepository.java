package com.actionth.membership.repository;

import com.actionth.membership.model.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface MenuRepository extends JpaRepository<Menu, Integer>, JpaSpecificationExecutor<Menu> {

    Optional<Menu> findByUuid(String uuid);

    void deleteByUuid(String uuid);
}
