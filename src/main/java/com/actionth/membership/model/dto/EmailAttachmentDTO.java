package com.actionth.membership.model.dto;

import java.io.Serializable;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for email attachments
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailAttachmentDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * The filename for the attachment (e.g., "document.pdf", "image.jpg")
     */
    private String filename;
    
    /**
     * The content of the attachment as byte array
     */
    private byte[] content;
    
    /**
     * The content type/MIME type (e.g., "application/pdf", "image/jpeg")
     */
    private String contentType;
}
