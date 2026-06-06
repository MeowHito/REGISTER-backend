package com.actionth.membership.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagingData {

    int page;
    int size;
    String sortField;
    String sortDirection;

    String searchField;
    String searchText;

    List<Search> search;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Search {
        private String searchField;
        private String searchText;
        private String searchType; // "STRING", "DATE", "EQUAL", "LIKE", etc.
    }
}
