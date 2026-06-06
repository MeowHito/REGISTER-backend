package com.actionth.membership.model;

import javax.persistence.*;

import lombok.Data;

@Entity
@Data
@Table(name = "emailLogAttachment")
public class EmailAttachmentLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String contentType;

    private String fileKey;

    private String prefix;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emailLogId")
    private EmailLog emailLog;
}
