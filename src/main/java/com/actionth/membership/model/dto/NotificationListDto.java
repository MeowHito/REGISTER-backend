package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** What the bell dropdown needs in one call: the latest items and the badge count. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationListDto {
    @Builder.Default
    private List<NotificationDto> items = new ArrayList<>();
    private long unreadCount;
}
