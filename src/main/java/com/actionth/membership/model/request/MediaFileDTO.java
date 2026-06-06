package com.actionth.membership.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class MediaFileDTO {

    private String id;
    private String prefixPath;
    private String path;
    private String thumbUrl;
}
