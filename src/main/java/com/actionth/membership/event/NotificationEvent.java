package com.actionth.membership.event;

import java.util.Set;

/**
 * A notification to deliver once the surrounding transaction commits.
 *
 * @param userIds       explicit recipients (may be empty)
 * @param toAdmins      also deliver to every active admin
 * @param excludeUserId never notify this user — the person who caused it
 */
public record NotificationEvent(
        Set<Integer> userIds,
        boolean toAdmins,
        Integer excludeUserId,
        String type,
        String title,
        String message,
        String link) {
}
