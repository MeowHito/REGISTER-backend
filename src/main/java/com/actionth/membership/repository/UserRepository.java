package com.actionth.membership.repository;

import com.actionth.membership.model.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends JpaRepository<User, Integer>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByUuid(String uuid);

    Optional<User> findByIdNo(String idNo);

    boolean existsByEmail(String email);

    Optional<User> findFirstByIsApprover(Boolean isApprover);

    @Query("SELECT u FROM User u WHERE u.active = true AND u.role.role = :role")
    List<User> findAllActiveUsersByRole(@Param("role") String role);

    @Query("SELECT u FROM User u WHERE u.active = true AND u.role.roleType = :roleType")
    List<User> findAllActiveUsersByRoleType(@Param("roleType") String roleType);

    @Query(value = "SELECT u.id, u.uuid, u.active, u.createdTime, u.updatedTime, " +
            "u.createdBy, u.updatedBy, u.email, u.password, u.firstName, u.lastName, " +
            "u.firstNameEn, u.lastNameEn, u.role, u.idNo, u.gender, u.birthDate, u.tel, " +
            "u.address, u.province, u.amphoe, u.district, u.zipcode, u.nationality, " +
            "u.bloodGroup, u.healthIssues, u.emergencyContact, u.emergencyContactTel, " +
            "u.prefixPath, u.pictureUrl, u.isApprover, u.signatureUrl " +
            "FROM user u WHERE u.idNo = :idNo", nativeQuery = true)
    Optional<User> findUserByIdNo(@Param("idNo") String idNo);

}
