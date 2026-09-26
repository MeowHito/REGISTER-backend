package com.actionth.membership.model.dto;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** One manual edit of a participant in the back office, stored in {@code OrderDetail.manualEditLog}. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ParticipantEditLogDto {

    private OffsetDateTime time;
    private String by;
    private List<Change> changes = new ArrayList<>();

    /** {@code field} is the form field key; the front end turns it into a label. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Change {
        private String field;
        private String before;
        private String after;
    }
}
