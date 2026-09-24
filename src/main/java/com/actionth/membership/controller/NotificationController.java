package com.actionth.membership.controller;

import com.actionth.membership.model.dto.NotificationListDto;
import com.actionth.membership.response.Response;
import com.actionth.membership.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in user's own bell notifications — never anyone else's. */
@RestController
@RequestMapping("/api/notification")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public Response<NotificationListDto> listMine() {
        return new Response<>(notificationService.listMine(), "Notifications retrieved successfully", true);
    }

    @PutMapping("/{id}/read")
    public Response<Void> markRead(@PathVariable String id) {
        notificationService.markRead(id);
        return new Response<>(null, "Notification marked as read", true);
    }

    @PutMapping("/readAll")
    public Response<Void> markAllRead() {
        notificationService.markAllRead();
        return new Response<>(null, "All notifications marked as read", true);
    }
}
