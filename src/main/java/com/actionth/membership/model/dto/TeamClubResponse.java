package com.actionth.membership.model.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeamClubResponse {

    private List<String> items;
    private int page;
    private int limit;
    private boolean hasMore;

}
