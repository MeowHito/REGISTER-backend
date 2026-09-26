package com.actionth.membership.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;

import com.actionth.membership.model.User;
import com.actionth.membership.model.dto.UserDto;
import com.actionth.membership.model.dto.UserViewDto;
import com.actionth.membership.model.request.GeneralRequest;
import com.actionth.membership.model.request.UseRoleDtoRequest;
import com.actionth.membership.model.request.UserAccountDTORequest;
import com.actionth.membership.model.request.UserProfileDTORequest;
import com.actionth.membership.model.request.UserRegisterDTORequest;
import com.actionth.membership.model.request.UserResetPasswordDTORequest;

public interface UserService {

    /* Default */

    Page<UserViewDto> findAll(GeneralRequest generalRequest);

    Map<String, Long> countByRoleType();

    User findById(Integer id);

    User findByEmail(String email);

    User findByUuid(String uuid);

    User findByIdNo(String idNo);

    UserDto createUser(UserRegisterDTORequest user);

    UserDto updateUser(UserProfileDTORequest user);

    void deleteUser(String uuid, String mode);

    /* Add-ons */

    UserDto getById(Integer id);

    UserDto getByEmail(String email);

    UserDto getByUuid(String uuid);

    UserDto getByIdNo(String idNo);

    List<UserViewDto> getUserActiveByRole(String role);

    List<UserViewDto> getUserActiveByRoleType(String roleType);
        
    String getToken(UserDto user);

    String getApproverSignatureImg();

    UserDto updatePassword(UserAccountDTORequest user);

    UserDto resetPassword(UserResetPasswordDTORequest user);

    void updateStatus(UserProfileDTORequest user);

    /** Admin-only: grant or revoke the right to approve organizer sign-ups (admins only). */
    void updateApprover(String uuid, boolean canApprove);

    void updateRole(UseRoleDtoRequest user);

    User getCurrentUserSession();
}
