package com.actionth.membership.service.impl;

import com.actionth.membership.exception.ResourceNotFoundException;
import com.actionth.membership.exception.ValidationException;
import com.actionth.membership.model.PagingData;
import com.actionth.membership.model.Role;
import com.actionth.membership.model.User;
import com.actionth.membership.model.dto.MenuViewDto;
import com.actionth.membership.model.dto.PermissionViewDto;
import com.actionth.membership.model.dto.RoleViewDto;
import com.actionth.membership.model.dto.UserDto;
import com.actionth.membership.model.dto.UserViewDto;
import com.actionth.membership.model.request.GeneralRequest;
import com.actionth.membership.model.request.UseRoleDtoRequest;
import com.actionth.membership.model.request.UserAccountDTORequest;
import com.actionth.membership.model.request.UserProfileDTORequest;
import com.actionth.membership.model.request.UserRegisterDTORequest;
import com.actionth.membership.model.request.UserResetPasswordDTORequest;
import com.actionth.membership.repository.RoleRepository;
import com.actionth.membership.repository.UserRepository;
import com.actionth.membership.service.AWSService;
import com.actionth.membership.service.UserService;
import com.actionth.membership.utils.ContextUtils;
import com.google.api.client.util.Strings;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.criteria.Predicate;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class UserServiceImpl implements UserService {

    @Value("${jwt.secret-key}")
    private String key;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AWSService awsService;
    private final ModelMapper modelMapper;
    private final RoleRepository roleRepository;
    private final ContextUtils contextUtils;

    @Override
    public Page<UserViewDto> findAll(GeneralRequest generalRequest) {
        PagingData paging = generalRequest.getPaging();

        Specification<User> searchSpec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (generalRequest.getActive() != null) {
                predicates.add(cb.equal(root.get("active"), generalRequest.getActive()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        if (paging == null || paging.getPage() < 1) {
            List<UserViewDto> dtos = userRepository.findAll(
                    searchSpec,
                    Sort.by(Sort.Direction.DESC, "id")).stream()
                    .map(this::mapViewUserToDto)
                    .toList();
            return new PageImpl<>(dtos);
        } else {
            Pageable pageable = PageRequest.of(
                    paging.getPage(),
                    paging.getSize(),
                    paging.getSortField() != null
                            ? Sort.by(Sort.Direction.fromString(paging.getSortDirection()), paging.getSortField())
                            : Sort.by(Sort.Direction.DESC, "id"));

            return userRepository.findAll(searchSpec, pageable)
                    .map(this::mapViewUserToDto);
        }
    }

    @Override
    public User findById(Integer id) {
        Optional<User> user = userRepository.findById(id);
        if (user.isPresent()) {
            return user.get();
        } else {
            throw new ResourceNotFoundException("User not found with id: " + id);
        }
    }

    @Override
    public User findByEmail(String email) {
        Optional<User> user = userRepository.findByEmail(email);
        if (user.isPresent()) {
            return user.get();
        } else {
            throw new ResourceNotFoundException("User not found with email: " + email);
        }
    }

    @Override
    public User findByUuid(String uuid) {
        Optional<User> user = userRepository.findByUuid(uuid);
        if (user.isPresent()) {
            return user.get();
        } else {
            throw new ResourceNotFoundException("User not found with uuid: " + uuid);
        }
    }

    @Override
    public User findByIdNo(String idNo) {
        Optional<User> user = userRepository.findByIdNo(idNo);
        if (user.isPresent()) {
            return user.get();
        } else {
            throw new ResourceNotFoundException("User not found with idNo: " + idNo);
        }
    }

    @Override
    public UserDto createUser(UserRegisterDTORequest userDto) {
        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ValidationException("Email is already taken");
        }
        User user = modelMapper.map(userDto, User.class);

        if (!Strings.isNullOrEmpty(userDto.getPassword())) {
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
        }

        Role role = new Role();
        if ("guest".equalsIgnoreCase(userDto.getRole())) {
            role = roleRepository.findByRole("guest")
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: guest"));
        } else if ("organizer".equalsIgnoreCase(userDto.getRole())) {
            role = roleRepository.findByRole("organizer")
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: organizer"));
        } else if ("admin".equalsIgnoreCase(userDto.getRole())) {
            role = roleRepository.findByRole("admin")
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: admin"));
        }

        user.setRole(role);

        User saved = userRepository.save(user);
        return mapUserToDto(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserDto updateUser(UserProfileDTORequest userDto) {
        User user = userRepository.findByUuid(userDto.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        user.setFirstName(userDto.getFirstName());
        user.setLastName(userDto.getLastName());
        user.setFirstNameEn(userDto.getFirstNameEn());
        user.setLastNameEn(userDto.getLastNameEn());
        user.setCompanyName(userDto.getCompanyName());
        user.setGender(userDto.getGender());
        user.setBirthDate(userDto.getBirthDate());
        user.setPhone(userDto.getPhone());
        user.setNationality(userDto.getNationality());
        // Only allow setting idNo if not already set, unless current user is admin
        User currentUser = getCurrentUserSession();
        boolean isAdmin = currentUser != null && "admin".equalsIgnoreCase(currentUser.getRole().getRoleType());
        if (isAdmin || user.getIdNo() == null || user.getIdNo().isBlank()) {
            user.setIdNo(userDto.getIdNo());
        }
        user.setHealthIssues(userDto.getHealthIssues());
        user.setBloodType(userDto.getBloodType());
        user.setEmergencyContact(userDto.getEmergencyContact());
        user.setEmergencyRelation(userDto.getEmergencyRelation());
        user.setEmergencyPhone(userDto.getEmergencyPhone());
        user.setPrefixPath(userDto.getPrefixPath());
        user.setPictureUrl(userDto.getPictureUrl());
        user.setSignatureUrl(userDto.getSignatureUrl());
        user.setAddress(userDto.getAddress());
        user.setProvince(userDto.getProvince());
        user.setAmphoe(userDto.getAmphoe());
        user.setDistrict(userDto.getDistrict());
        user.setZipcode(userDto.getZipcode());
        user.setShippingAddress(userDto.getShippingAddress());
        user.setShippingProvince(userDto.getShippingProvince());
        user.setShippingAmphoe(userDto.getShippingAmphoe());
        user.setShippingDistrict(userDto.getShippingDistrict());
        user.setShippingZipcode(userDto.getShippingZipcode());

        if (Boolean.TRUE.equals(userDto.getTranferApprover()) &&
                userDto.getApproverId() != null) {
            User tranferUser = userRepository.findByUuid(userDto.getApproverId())
                    .orElseThrow(() -> new ResourceNotFoundException("Tranfer User not found"));
            tranferUser.setIsApprover(true);
            user.setIsApprover(false);
            userRepository.save(tranferUser);
        }

        User saved = userRepository.save(user);
        return mapUserToDto(saved);
    }

    @Override
    public void deleteUser(String uuid, String mode) {
        if ("hard".equals(mode)) {
            User entity = userRepository.findByUuid(uuid)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            userRepository.delete(entity);
        } else if ("soft".equals(mode)) {
            User entity = userRepository.findByUuid(uuid)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            entity.setActive(false);
            userRepository.save(entity);
        }
    }

    @Override
    public UserDto getById(Integer id) {
        User user = this.findById(id);
        return mapUserToDto(user);
    }

    @Override
    public UserDto getByEmail(String email) {
        User user = this.findByEmail(email);
        return mapUserToDto(user);
    }

    @Override
    public UserDto getByUuid(String uuid) {
        User currentUser = getCurrentUserSession();
        if (currentUser != null && !"admin".equalsIgnoreCase(currentUser.getRole().getRoleType())
                && !uuid.equals(currentUser.getUuid())) {
            throw new ValidationException("ท่านไม่มีสิทธิ์ในการเข้าถึงข้อมูล");
        }
        User user = this.findByUuid(uuid);
        return mapUserToDto(user);
    }

    @Override
    public UserDto getByIdNo(String idNo) {
        User user = this.findByIdNo(idNo);
        return mapUserToDto(user);
    }

    @Override
    public List<UserViewDto> getUserActiveByRole(String role) {
        List<User> users = userRepository.findAllActiveUsersByRole(role);
        return users.stream().map(this::mapViewUserToDto).toList();
    }

    @Override
    public List<UserViewDto> getUserActiveByRoleType(String roleType) {
        List<User> users = userRepository.findAllActiveUsersByRoleType(roleType);
        return users.stream().map(this::mapViewUserToDto).toList();
    }

    @Override
    public String getToken(UserDto userDto) {
        User user = this.findByUuid(userDto.getId());
        return Jwts.builder()
                .setSubject(user.getId().toString())
                .claim("authorities", "ROLE_" + userDto.getRole().getRoleType().toUpperCase())
                .setIssuedAt(new Date())
                .setExpiration(Date.from(Instant.now().plus(14, ChronoUnit.DAYS)))
                .signWith(Keys.hmacShaKeyFor(key.getBytes()))
                .compact();
    }

    @Override
    public String getApproverSignatureImg() {
        return userRepository.findFirstByIsApprover(true)
                .map(User::getSignatureUrl)
                .orElse(null);
    }

    @Override
    public UserDto updatePassword(UserAccountDTORequest userDto) {
        User user = getCurrentUserSession();

        if (user == null) {
            throw new ValidationException("ไม่พบผู้ใช้งาน");
        }

        if (!passwordEncoder.matches(userDto.getOpw(), user.getPassword())) {
            throw new ValidationException("รหัสผ่านเดิมไม่ถูกต้อง");
        }

        user.setPassword(passwordEncoder.encode(userDto.getNpw()));

        User saved = userRepository.save(user);

        return mapUserToDto(saved);
    }

    @Override
    public UserDto resetPassword(UserResetPasswordDTORequest userDto) {
        User currentUser = getCurrentUserSession();
        if (currentUser == null || !"admin".equalsIgnoreCase(currentUser.getRole().getRoleType())) {
            throw new ValidationException("ขออภัย ท่านไม่มีสิทธิในการรีเซตรหัสผ่าน");
        }

        User user = this.findByUuid(userDto.getId());

        user.setPassword(passwordEncoder.encode(userDto.getNpw()));

        User saved = userRepository.save(user);

        return mapUserToDto(saved);
    }

    @Override
    public void updateStatus(UserProfileDTORequest userDto) {
        User user = this.findByUuid(userDto.getId());

        user.setActive(userDto.getActive());
        userRepository.save(user);
    }

    @Override
    public void updateRole(UseRoleDtoRequest userDto) {
        User user = this.findByUuid(userDto.getUserId());
        Role role = roleRepository.findByUuid(userDto.getRoleId())
                .orElseThrow(() -> new IllegalArgumentException("Role not found: " + userDto.getRoleId()));
        user.setRole(role);
        userRepository.save(user);
    }

    private UserDto mapUserToDto(User user) {
        UserDto userMapper = modelMapper.map(user, UserDto.class);
        userMapper.setId(user.getUuid());

        if (user.getRole() != null) {
            RoleViewDto roleDto = modelMapper.map(user.getRole(), RoleViewDto.class);

            List<PermissionViewDto> permissionDtos = user.getRole().getPermissions().stream()
                    .filter(p -> Boolean.TRUE.equals(p.getCanRead()))
                    .map(permission -> {
                        PermissionViewDto dto = modelMapper.map(permission, PermissionViewDto.class);

                        if (permission.getMenu() != null) {
                            MenuViewDto menuDto = modelMapper.map(permission.getMenu(), MenuViewDto.class);
                            dto.setMenu(menuDto);
                        }

                        return dto;
                    })
                    .toList();

            roleDto.setPermissions(permissionDtos);
            userMapper.setRole(roleDto);
        }

        String prefixPath = user.getPrefixPath();
        if (prefixPath != null && !prefixPath.isEmpty()) {
            try {
                if (user.getPictureUrl() != null) {
                    String publicUrl = awsService.getPublicUrl(prefixPath, user.getPictureUrl());
                    userMapper.setThumbPictureUrl(publicUrl);
                }
                if (user.getSignatureUrl() != null) {
                    String signaturePublicUrl = awsService.getPublicUrl(prefixPath, user.getSignatureUrl());
                    userMapper.setThumbSignatureUrl(signaturePublicUrl);
                }
            } catch (SQLException e) {
                log.error("Unable to generate public URL: {}", e.getMessage());
            }
        }

        return userMapper;
    }

    private UserViewDto mapViewUserToDto(User user) {
        UserViewDto userMapper = modelMapper.map(user, UserViewDto.class);
        userMapper.setId(user.getUuid());
        userMapper.setRole(null);
        userMapper.setRoleType(null);
        userMapper.setActive(null);
        User currentUser = getCurrentUserSession();

        if (currentUser != null && "admin".equalsIgnoreCase(currentUser.getRole().getRoleType())) {
                userMapper.setRole(user.getRole().getRole());
                userMapper.setRoleType(user.getRole().getRoleType());
                userMapper.setActive(user.getActive());
                String prefixPath = user.getPrefixPath();
                String thumbPictureUrl = user.getPictureUrl();
                if (prefixPath != null && !prefixPath.isEmpty()) {
                    try {
                        String publicUrl = awsService.getPublicUrl(prefixPath, thumbPictureUrl);
                        userMapper.setThumbPictureUrl(publicUrl);
                    } catch (SQLException e) {
                        log.error("Unable to generate public URL: {}", e.getMessage());
                    }
                }
            }
        
        return userMapper;
    }

    @Override
    public User getCurrentUserSession() {
        Integer currentUser = contextUtils.getCurrentUserIdOrNull();
        if (currentUser == null) {
            return null;
        }
        return this.findById(contextUtils.getCurrentUserIdOrNull());
    }
}
