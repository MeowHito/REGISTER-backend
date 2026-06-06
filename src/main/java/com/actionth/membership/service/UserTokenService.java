package com.actionth.membership.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.exception.ValidationException;
import com.actionth.membership.model.User;
import com.actionth.membership.model.UserToken;
import com.actionth.membership.model.request.UserTokenDTORequest;
import com.actionth.membership.repository.UserRepository;
import com.actionth.membership.repository.UserTokenRepository;

@Service
public class UserTokenService {

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean existsByUuidAndActiveWithinOneDay(String uuid) {
        return userTokenRepository.countByUuidAndActiveWithinOneDay(uuid) > 0;
    }

    public UserToken createUserToken(UserToken userToken) {
        return userTokenRepository.save(userToken);
    }

    public UserToken getUserTokenByUserId(Integer userId) {
        // ค้นหา UserToken ตาม userId
        return userTokenRepository.findByUserId(userId);
    }

    @Transactional(rollbackFor = Exception.class)
    public UserToken updateUserToken(UserTokenDTORequest userTokenDTO) {

        if (userTokenRepository.countByUuidAndActiveWithinOneDay(userTokenDTO.getUuid()) <= 0) {
            throw new ValidationException("The token is invalid or expired.");
        }

        UserToken userToken = userTokenRepository.findByUuid(userTokenDTO.getUuid())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        userToken.setActive(false);

        String newPassword = userTokenDTO.getNpw();
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new ValidationException("New password cannot be empty.");
        }

        User user = userToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        
        userRepository.save(user);
        userTokenRepository.save(userToken);
        return userToken;
    }

    public void deleteUserToken(Integer id) {
        // ลบ UserToken โดยใช้ id
        userTokenRepository.deleteById(id);
    }

}
