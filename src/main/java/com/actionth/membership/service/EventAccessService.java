package com.actionth.membership.service;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventPermission;
import com.actionth.membership.model.OrderDetail;
import com.actionth.membership.repository.EventRepository;
import com.actionth.membership.repository.OrderDetailRepository;
import com.actionth.membership.utils.ContextUtils;

import lombok.RequiredArgsConstructor;

/**
 * Per-event authorisation, finer than roles. Admin passes straight through; otherwise the caller
 * must be the event's organizer or hold an active {@link EventPermission} with the matching flag.
 * Deleting is never implied by ownership — it needs {@code canDelete}.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventAccessService {

    public enum Access { READ, UPDATE, DELETE }

    private final EventRepository eventRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final ContextUtils contextUtils;

    public void assertCan(Event event, Access access) {
        if (isCurrentUserAdmin()) {
            return;
        }

        Integer userId = contextUtils.getCurrentUserIdOrNull();
        if (userId == null) {
            throw new AccessDeniedException("Unauthenticated");
        }

        EventPermission permission = event.getEventPermissions() == null ? null
                : event.getEventPermissions().stream()
                        .filter(p -> p.getUser() != null && userId.equals(p.getUser().getId())
                                && !Boolean.FALSE.equals(p.getActive()))
                        .findFirst()
                        .orElse(null);
        boolean isOwner = event.getOrganizer() != null && userId.equals(event.getOrganizer().getId());

        boolean allowed = switch (access) {
            case READ -> isOwner || (permission != null && Boolean.TRUE.equals(permission.getCanRead()));
            case UPDATE -> isOwner || (permission != null && Boolean.TRUE.equals(permission.getCanUpdate()));
            case DELETE -> permission != null && Boolean.TRUE.equals(permission.getCanDelete());
        };

        if (!allowed) {
            throw new AccessDeniedException("No permission to " + access.name().toLowerCase() + " this event");
        }
    }

    public void assertCanByEventUuid(String eventUuid, Access access) {
        Event event = eventRepository.findByUuid(eventUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        assertCan(event, access);
    }

    public void assertCanByParticipantUuid(String participantUuid, Access access) {
        OrderDetail participant = orderDetailRepository.findByUuid(participantUuid)
                .orElseThrow(() -> new ResourceNotFoundException("Participant not found"));
        Event event = participant.getOrder() != null ? participant.getOrder().getEvent() : null;
        if (event == null && participant.getEventType() != null) {
            event = participant.getEventType().getEvent();
        }
        if (event == null) {
            // Unowned data is admin-only rather than open to everyone.
            if (isCurrentUserAdmin()) {
                return;
            }
            throw new AccessDeniedException("No permission to " + access.name().toLowerCase() + " this participant");
        }
        assertCan(event, access);
    }

    public boolean isCurrentUserAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
