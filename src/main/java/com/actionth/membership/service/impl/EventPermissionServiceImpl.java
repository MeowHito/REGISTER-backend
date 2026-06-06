package com.actionth.membership.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.exception.EmailMismatchException;
import com.actionth.membership.model.Event;
import com.actionth.membership.model.EventInvitation;
import com.actionth.membership.model.EventPermission;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.User;
import com.actionth.membership.model.dto.EventPermissionDto;
import com.actionth.membership.model.dto.InviteResponseDto;
import com.actionth.membership.model.dto.InviteResultDto;
import com.actionth.membership.model.request.InviteRequest;
import com.actionth.membership.model.request.UpdatePermissionRequest;
import com.actionth.membership.repository.EventInvitationRepository;
import com.actionth.membership.repository.EventPermissionRepository;
import com.actionth.membership.repository.EventRepository;
import com.actionth.membership.repository.UserRepository;
import com.actionth.membership.service.EmailService;
import com.actionth.membership.service.EventPermissionService;
import com.actionth.membership.service.UserService;

import javax.transaction.Transactional;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class EventPermissionServiceImpl implements EventPermissionService {

    private static final List<String> VALID_ROLES = List.of("admin", "editor", "viewer");
    private static final int INVITE_EXPIRY_DAYS = 7;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EventPermissionRepository eventPermissionRepository;
    private final EventInvitationRepository eventInvitationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final UserService userService;

    @Override
    public Page<EventPermissionDto> getEventPermissionsByEvent(String eventUuid, PagingData pagingData) {

        Sort sort = Optional.ofNullable(pagingData.getSortField())
                .map(field -> Sort.by(
                        "DESC".equalsIgnoreCase(pagingData.getSortDirection())
                                ? Sort.Direction.DESC
                                : Sort.Direction.ASC,
                        field))
                .orElse(Sort.by(Sort.Direction.DESC, "id"));

        Pageable pageable = PageRequest.of(pagingData.getPage(), pagingData.getSize(), sort);

        Event event = eventRepository.findByUuid(eventUuid)
                .orElseThrow(() -> new BusinessException("Event not found: " + eventUuid));
        String ownerUuid = event.getOrganizer() != null ? event.getOrganizer().getUuid() : null;

        Page<EventPermission> permissions = eventPermissionRepository.findActiveByEventUuid(eventUuid, pageable);

        return permissions.map(ep -> toPermissionDto(ep, ownerUuid));
    }

    private EventPermissionDto toPermissionDto(EventPermission ep, String ownerUuid) {
        User u = ep.getUser();

        EventPermissionDto dto = new EventPermissionDto();
        dto.setId(ep.getUuid());
        dto.setEventId(ep.getEvent().getUuid());
        dto.setUserId(u.getUuid());
        dto.setFirstName(u.getFirstName());
        dto.setLastName(u.getLastName());
        dto.setEmail(u.getEmail());
        dto.setCompanyName(u.getCompanyName());
        dto.setCanRead(Boolean.TRUE.equals(ep.getCanRead()));
        dto.setCanUpdate(Boolean.TRUE.equals(ep.getCanUpdate()));
        dto.setCanDelete(Boolean.TRUE.equals(ep.getCanDelete()));

        if (u.getUuid().equals(ownerUuid)) {
            dto.setRole("owner");
        } else if (ep.getRole() != null && !ep.getRole().isEmpty()) {
            dto.setRole(ep.getRole());
        } else if (Boolean.TRUE.equals(ep.getCanUpdate())) {
            dto.setRole("editor");
        } else {
            dto.setRole("viewer");
        }

        return dto;
    }

    @Override
    @Transactional
    public void updatePermissions(String eventId, UpdatePermissionRequest request) {
        Event event = eventRepository.findByUuid(eventId)
                .orElseThrow(() -> new BusinessException("Event not found: " + eventId));
        String ownerUuid = event.getOrganizer() != null ? event.getOrganizer().getUuid() : null;

        User currentUser = userService.getCurrentUserSession();
        boolean isAdmin = currentUser != null && currentUser.getRole() != null
                && "admin".equalsIgnoreCase(currentUser.getRole().getRoleType());

        String callerRole = resolveCallerRole(eventId, ownerUuid);

        if (!isAdmin && !"owner".equals(callerRole) && !"admin".equals(callerRole)) {
            throw new BusinessException("Only owner or admin can update permissions");
        }

        List<UpdatePermissionRequest.PermissionItem> permissions = request.getPermissions();
        if (permissions != null) {
            for (UpdatePermissionRequest.PermissionItem p : permissions) {
                if (p.getUserId() != null && p.getUserId().equals(ownerUuid)) {
                    throw new BusinessException("Cannot change owner's role");
                }
                if (p.getRole() == null || !VALID_ROLES.contains(p.getRole())) {
                    throw new BusinessException("Invalid role: " + p.getRole());
                }
                if ("admin".equals(p.getRole()) && !"owner".equals(callerRole) && !isAdmin) {
                    throw new BusinessException("Only owner can assign admin role");
                }
                if ("admin".equals(callerRole)) {
                    eventPermissionRepository.findActiveByEventUuidAndUserUuid(eventId, p.getUserId())
                            .ifPresent(target -> {
                                if ("admin".equals(target.getRole())) {
                                    throw new BusinessException("Admin cannot modify other admins");
                                }
                            });
                }
            }
        }

        List<String> removedUserIds = request.getRemovedUserIds();
        if (removedUserIds != null && ownerUuid != null && removedUserIds.contains(ownerUuid)) {
            throw new BusinessException("Cannot remove event owner");
        }
        if ("admin".equals(callerRole) && removedUserIds != null) {
            for (String userId : removedUserIds) {
                eventPermissionRepository.findActiveByEventUuidAndUserUuid(eventId, userId)
                        .ifPresent(target -> {
                            if ("admin".equals(target.getRole())) {
                                throw new BusinessException("Admin cannot remove other admins");
                            }
                        });
            }
        }

        if (permissions != null) {
            for (UpdatePermissionRequest.PermissionItem p : permissions) {
                Optional<EventPermission> existing = eventPermissionRepository
                        .findByEventUuidAndUserUuid(eventId, p.getUserId());

                EventPermission ep;
                if (existing.isPresent()) {
                    ep = existing.get();
                    ep.setActive(true);
                } else {
                    User user = userRepository.findByUuid(p.getUserId())
                            .orElseThrow(() -> new BusinessException("User not found: " + p.getUserId()));
                    ep = new EventPermission();
                    ep.setEvent(event);
                    ep.setUser(user);
                    ep.setActive(true);
                }

                ep.setRole(p.getRole());
                ep.syncBooleanFlags();
                eventPermissionRepository.save(ep);
            }
        }

        if (removedUserIds != null) {
            for (String userId : removedUserIds) {
                eventPermissionRepository.findActiveByEventUuidAndUserUuid(eventId, userId)
                        .ifPresent(ep -> {
                            ep.setActive(false);
                            eventPermissionRepository.save(ep);
                        });
            }
        }
    }

    @Override
    @Transactional
    public InviteResponseDto inviteMembers(String eventId, InviteRequest request) {
        Event event = eventRepository.findByUuid(eventId)
                .orElseThrow(() -> new BusinessException("Event not found: " + eventId));

        List<InviteResultDto> sent = new ArrayList<>();
        List<InviteResultDto> failed = new ArrayList<>();

        if (request.getInvitees() == null || request.getInvitees().isEmpty()) {
            return InviteResponseDto.builder().sent(sent).failed(failed).build();
        }

        String inviterName = resolveInviterName();

        String ownerUuid = event.getOrganizer() != null ? event.getOrganizer().getUuid() : null;

        User currentUser = userService.getCurrentUserSession();
        boolean isAdmin = currentUser != null && currentUser.getRole() != null
                && "admin".equalsIgnoreCase(currentUser.getRole().getRoleType());

        String callerRole = resolveCallerRole(eventId, ownerUuid);

        if (!isAdmin && !"owner".equals(callerRole) && !"admin".equals(callerRole)) {
            throw new BusinessException("Only owner or admin can invite members");
        }

        for (InviteRequest.InviteItem invite : request.getInvitees()) {
            if (invite.getRole() == null || !VALID_ROLES.contains(invite.getRole())) {
                failed.add(InviteResultDto.builder()
                        .email(invite.getEmail()).status("invalid_role").build());
                continue;
            }

            if ("admin".equals(invite.getRole()) && !"owner".equals(callerRole) && !isAdmin) {
                failed.add(InviteResultDto.builder()
                        .email(invite.getEmail()).status("forbidden_admin_invite").build());
                continue;
            }

            Optional<User> userOpt = userRepository.findByEmail(invite.getEmail());
            if (userOpt.isPresent()) {
                Optional<EventPermission> existingPerm = eventPermissionRepository
                        .findActiveByEventUuidAndUserUuid(eventId, userOpt.get().getUuid());
                if (existingPerm.isPresent()) {
                    failed.add(InviteResultDto.builder()
                            .email(invite.getEmail()).status("already_member").build());
                    continue;
                }
            }

            Optional<EventInvitation> existingInvite = eventInvitationRepository
                    .findPendingByEventUuidAndEmail(eventId, invite.getEmail());
            if (existingInvite.isPresent()) {
                EventInvitation existing = existingInvite.get();
                if (!existing.isExpired()) {
                    failed.add(InviteResultDto.builder()
                            .email(invite.getEmail()).status("already_invited").build());
                    continue;
                }
                existing.setStatus("expired");
                existing.setActive(false);
                eventInvitationRepository.save(existing);
            }

            String token = generateToken();
            EventInvitation invitation = EventInvitation.builder()
                    .token(token)
                    .event(event)
                    .email(invite.getEmail())
                    .role(invite.getRole())
                    .status("pending")
                    .expiresAt(OffsetDateTime.now().plusDays(INVITE_EXPIRY_DAYS))
                    .invitedByName(inviterName)
                    .active(true)
                    .build();
            eventInvitationRepository.save(invitation);

            try {
                emailService.sendInviteEmail(
                        invite.getEmail(), inviterName, event.getName(),
                        invite.getRole(), token);
            } catch (Exception e) {
                log.error("Failed to send invite email to {}: {}", invite.getEmail(), e.getMessage());
            }

            sent.add(InviteResultDto.builder()
                    .email(invite.getEmail()).status("invited").build());
        }

        return InviteResponseDto.builder().sent(sent).failed(failed).build();
    }

    @Override
    @Transactional
    public String acceptInvitation(String token) {
        EventInvitation invitation = eventInvitationRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("Invalid invitation token"));

        if (!"pending".equals(invitation.getStatus())) {
            throw new BusinessException("Invitation has already been " + invitation.getStatus());
        }

        if (invitation.isExpired()) {
            invitation.setStatus("expired");
            invitation.setActive(false);
            eventInvitationRepository.save(invitation);
            throw new BusinessException("Invitation has expired");
        }

        User currentUser = userService.getCurrentUserSession();
        if (currentUser == null) {
            throw new BusinessException("Authentication required");
        }
        if (!invitation.getEmail().equalsIgnoreCase(currentUser.getEmail())) {
            throw new EmailMismatchException(invitation.getEmail());
        }

        User user = currentUser;

        Event event = invitation.getEvent();
        String eventUuid = event.getUuid();

        Optional<EventPermission> existingOpt = eventPermissionRepository
                .findByEventUuidAndUserUuid(eventUuid, user.getUuid());

        if (existingOpt.isPresent()) {
            EventPermission existing = existingOpt.get();
            if (Boolean.TRUE.equals(existing.getActive())) {
                invitation.setStatus("accepted");
                invitation.setAcceptedByUser(user);
                invitation.setAcceptedAt(OffsetDateTime.now());
                eventInvitationRepository.save(invitation);
                return eventUuid;
            }
            existing.setActive(true);
            existing.setRole(invitation.getRole());
            existing.syncBooleanFlags();
            eventPermissionRepository.save(existing);
        } else {
            EventPermission ep = new EventPermission();
            ep.setEvent(event);
            ep.setUser(user);
            ep.setRole(invitation.getRole());
            ep.setActive(true);
            ep.syncBooleanFlags();
            eventPermissionRepository.save(ep);
        }

        invitation.setStatus("accepted");
        invitation.setAcceptedByUser(user);
        invitation.setAcceptedAt(OffsetDateTime.now());
        eventInvitationRepository.save(invitation);

        return eventUuid;
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(64);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String resolveCallerRole(String eventUuid, String ownerUuid) {
        try {
            User currentUser = userService.getCurrentUserSession();
            if (currentUser == null) return null;
            if (currentUser.getUuid().equals(ownerUuid)) return "owner";
            return eventPermissionRepository
                    .findActiveByEventUuidAndUserUuid(eventUuid, currentUser.getUuid())
                    .map(EventPermission::getRole)
                    .orElse(null);
        } catch (Exception e) {
            log.warn("Could not resolve caller role: {}", e.getMessage());
            return null;
        }
    }

    private String resolveInviterName() {
        try {
            User currentUser = userService.getCurrentUserSession();
            if (currentUser != null) {
                String name = currentUser.getFirstName();
                if (currentUser.getLastName() != null) {
                    name += " " + currentUser.getLastName();
                }
                return name;
            }
        } catch (Exception e) {
            log.warn("Could not resolve inviter name: {}", e.getMessage());
        }
        return "ผู้ดูแลระบบ";
    }

}

