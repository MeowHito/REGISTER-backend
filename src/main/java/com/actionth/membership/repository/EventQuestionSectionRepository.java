package com.actionth.membership.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.EventQuestionSection;

@Repository
public interface EventQuestionSectionRepository extends JpaRepository<EventQuestionSection, Integer> {

    Optional<EventQuestionSection> findByUuid(String uuid);

    Optional<EventQuestionSection> findByShareToken(String shareToken);
}
