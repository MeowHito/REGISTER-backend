package com.actionth.membership.model.projection;

import java.time.OffsetDateTime;

public interface EventOrganizerProjection {
    String getUuid();
    String getName();
    OffsetDateTime getEventDate();
}
