package com.actionth.membership.model.request;

import java.util.List;

import lombok.Data;

@Data
public class ZipDTORequest {
    private String name;
    private List<ImageDTO> images;

    @Data
    public static class ImageDTO {
        private String url;
        private String filename;
    }

}
