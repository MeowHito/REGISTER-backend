package com.actionth.membership.model.request;

import java.io.Serializable;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BibDocumentEmailRequest implements Serializable {

    private static final long serialVersionUID = 1L;
    
    private String to;
    private List<BibDocumentMessageDTO> bibList;
}
