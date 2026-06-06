package com.actionth.membership.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.actionth.membership.model.EmailAttachmentLog;

@Repository
public interface EmailAttachmentLogRepository extends JpaRepository<EmailAttachmentLog, Long> {
    List<EmailAttachmentLog> findByEmailLogId(Long emailLogId);
}
