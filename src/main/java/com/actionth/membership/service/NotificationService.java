package com.actionth.membership.service;

import com.actionth.membership.constant.NotificationType;
import com.actionth.membership.constant.PaymentStatus;
import com.actionth.membership.event.NotificationEvent;
import com.actionth.membership.event.OrderStatusChangedEvent;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.Notification;
import com.actionth.membership.model.Orders;
import com.actionth.membership.model.User;
import com.actionth.membership.model.dto.NotificationDto;
import com.actionth.membership.model.dto.NotificationListDto;
import com.actionth.membership.repository.NotificationRepository;
import com.actionth.membership.repository.OrderRepository;
import com.actionth.membership.repository.UserRepository;
import com.actionth.membership.utils.ContextUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Back-office bell notifications.
 *
 * Callers only publish; rows are written after the caller's transaction
 * commits, in a transaction of their own, and every failure is swallowed and
 * logged — a notification must never roll back or break a payment, a signup
 * or an invite.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int LIST_SIZE = 20;

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher publisher;
    private final ContextUtils contextUtils;
    private final PlatformTransactionManager transactionManager;

    // ---------------------------------------------------------------- publish

    /** Something an admin has to act on; the person who caused it is never notified. */
    public void notifyAdmins(String type, String title, String message, String link) {
        publisher.publishEvent(new NotificationEvent(Set.of(), true, contextUtils.getCurrentUserIdOrNull(),
                type, title, message, link));
    }

    public void notifyUser(Integer userId, String type, String title, String message, String link) {
        if (userId == null) {
            return;
        }
        publisher.publishEvent(new NotificationEvent(Set.of(userId), false, contextUtils.getCurrentUserIdOrNull(),
                type, title, message, link));
    }

    /**
     * Organizer sign-up waiting for approval: goes to the admins who may approve; when nobody
     * holds that right yet, every admin is told so the sign-up is not lost.
     */
    public void organizerPending(User organizer) {
        String name = (Objects.toString(organizer.getFirstName(), "") + " "
                + Objects.toString(organizer.getLastName(), "")).trim();
        String title = "มีผู้จัดงานสมัครใหม่รออนุมัติ";
        String message = (name.isEmpty() ? "" : name + " · ") + Objects.toString(organizer.getEmail(), "");
        String link = "/operations?tab=pendingOrganizers";
        Set<Integer> approvers = userRepository.findOrganizerApprovers().stream()
                .map(User::getId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (approvers.isEmpty()) {
            notifyAdmins(NotificationType.ORGANIZER_PENDING, title, message, link);
            return;
        }
        publisher.publishEvent(new NotificationEvent(approvers, false, contextUtils.getCurrentUserIdOrNull(),
                NotificationType.ORGANIZER_PENDING, title, message, link));
    }

    // ---------------------------------------------------------------- deliver

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotification(NotificationEvent event) {
        runIsolated("type=" + event.type(), () -> save(event));
    }

    private void save(NotificationEvent event) {
        Set<Integer> recipients = new LinkedHashSet<>(event.userIds());
        if (event.toAdmins()) {
            userRepository.findAllActiveUsersByRoleType("admin").forEach(u -> recipients.add(u.getId()));
        }
        if (event.excludeUserId() != null) {
            recipients.remove(event.excludeUserId());
        }
        for (Integer userId : recipients) {
            userRepository.findById(userId).ifPresent(user -> notificationRepository.save(Notification.builder()
                    .recipient(user)
                    .type(event.type())
                    .title(truncate(event.title(), 255))
                    .message(truncate(event.message(), 500))
                    .link(truncate(event.link(), 500))
                    .build()));
        }
    }

    /**
     * Runs after the caller committed, in a brand-new transaction; nothing it
     * throws — including a failed commit — ever reaches the caller.
     */
    private void runIsolated(String what, Runnable work) {
        try {
            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            tx.executeWithoutResult(status -> work.run());
        } catch (Exception e) {
            log.error("[Notification] Failed to deliver {}: {}", what, e.getMessage(), e);
        }
    }

    /** Paid → the event's organizer; sent to review → admins and the organizer. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderStatusChanged(OrderStatusChangedEvent change) {
        boolean paid = PaymentStatus.SUCCESS.toString().equalsIgnoreCase(change.newStatus());
        boolean review = PaymentStatus.REVIEW.toString().equalsIgnoreCase(change.newStatus());
        if (!paid && !review) {
            return;
        }
        runIsolated("order " + change.orderId() + " -> " + change.newStatus(),
                () -> saveOrderNotifications(change.orderId(), paid));
    }

    private void saveOrderNotifications(Integer orderId, boolean paid) {
        Orders order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }
        Event event = order.getEvent();
        String eventName = event != null && event.getName() != null ? event.getName() : "-";
        User organizer = event != null ? event.getOrganizer() : null;
        int applicants = order.getOrderDetails() != null ? order.getOrderDetails().size() : 0;

        if (paid) {
            if (organizer != null) {
                save(new NotificationEvent(Set.of(organizer.getId()), false, null,
                        NotificationType.ORDER_PAID,
                        "มีผู้สมัครชำระเงินใหม่",
                        eventName + " · ออเดอร์ " + order.getOrderNo() + " · " + applicants + " คน",
                        "/eventList"));
            }
            return;
        }

        String reason = order.getReviewReason() != null ? " (" + order.getReviewReason() + ")" : "";
        String message = eventName + " · ออเดอร์ " + order.getOrderNo() + reason;
        save(new NotificationEvent(Set.of(), true, null, NotificationType.ORDER_REVIEW,
                "ออเดอร์รอตรวจสอบการชำระเงิน", message, "/operations?tab=paymentMismatch"));
        if (organizer != null && !isAdmin(organizer)) {
            save(new NotificationEvent(Set.of(organizer.getId()), false, null, NotificationType.ORDER_REVIEW,
                    "ออเดอร์รอตรวจสอบการชำระเงิน", message, "/eventList"));
        }
    }

    // ---------------------------------------------------------------- read (current user only)

    @Transactional(readOnly = true)
    public NotificationListDto listMine() {
        Integer userId = requireUserId();
        List<NotificationDto> items = notificationRepository
                .findLatestByRecipient(userId, PageRequest.of(0, LIST_SIZE)).stream()
                .map(this::toDto)
                .toList();
        return NotificationListDto.builder()
                .items(items)
                .unreadCount(notificationRepository.countUnreadByRecipient(userId))
                .build();
    }

    @Transactional
    public void markRead(String uuid) {
        Integer userId = requireUserId();
        notificationRepository.findOwned(uuid, userId).ifPresent(n -> {
            if (n.getReadAt() == null) {
                n.setReadAt(OffsetDateTime.now());
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.markAllRead(requireUserId(), OffsetDateTime.now());
    }

    private Integer requireUserId() {
        Integer userId = contextUtils.getCurrentUserIdOrNull();
        if (userId == null) {
            throw new AccessDeniedException("Unauthenticated");
        }
        return userId;
    }

    private NotificationDto toDto(Notification n) {
        return NotificationDto.builder()
                .id(n.getUuid())
                .type(n.getType())
                .title(n.getTitle())
                .message(n.getMessage())
                .link(n.getLink())
                .read(n.getReadAt() != null)
                .createdTime(n.getCreatedTime())
                .build();
    }

    private static boolean isAdmin(User user) {
        return user.getRole() != null && "admin".equalsIgnoreCase(user.getRole().getRoleType());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

}
