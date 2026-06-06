package com.actionth.membership.service;

import org.springframework.data.domain.Page;

import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.dto.EventPermissionDto;
import com.actionth.membership.model.dto.InviteResponseDto;
import com.actionth.membership.model.request.InviteRequest;
import com.actionth.membership.model.request.UpdatePermissionRequest;

public interface EventPermissionService {

    Page<EventPermissionDto> getEventPermissionsByEvent(String eventId, PagingData pagingData);

    void updatePermissions(String eventId, UpdatePermissionRequest request);

    InviteResponseDto inviteMembers(String eventId, InviteRequest request);

    String acceptInvitation(String token);

}
