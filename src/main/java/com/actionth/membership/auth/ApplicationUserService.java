package com.actionth.membership.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.model.User;
import com.actionth.membership.service.UserService;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ApplicationUserService implements UserDetailsService {

    @Autowired
    private UserService userService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        try {
            User user = userService.findByEmail(email);
            if (user == null || !user.getActive()) {
                throw new UsernameNotFoundException("User not found with email: " + email);
            }
            return new ApplicationUser(
                user.getId().toString(),
                user.getPassword(),
                AuthorityUtils.createAuthorityList("ROLE_" + user.getRole().getRoleType().toUpperCase()),
                true,
                true,
                true,
                true
            );
        } catch (EmptyResultDataAccessException e) {
            throw new UsernameNotFoundException("User not found with email: " + email);
        } catch (DataAccessException e) {
            log.error("Error accessing database:", e);
            throw new BusinessException("Error accessing database", e);
        }
    }

}
