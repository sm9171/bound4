package com.bound4.project.adapter.out.persistence;

import com.bound4.project.domain.Role;
import com.bound4.project.domain.SubscriptionPlan;
import com.bound4.project.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findByRole(Role role);

    List<User> findBySubscriptionPlan(SubscriptionPlan subscriptionPlan);

    List<User> findByRoleAndSubscriptionPlan(Role role, SubscriptionPlan subscriptionPlan);

    @Query("SELECT u FROM User u WHERE u.createdAt >= :fromDate")
    List<User> findUsersCreatedAfter(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role")
    long countByRole(@Param("role") Role role);

    @Query("SELECT COUNT(u) FROM User u WHERE u.subscriptionPlan = :plan")
    long countBySubscriptionPlan(@Param("plan") SubscriptionPlan plan);

    @Query("SELECT u FROM User u WHERE u.email LIKE %:emailPattern%")
    List<User> findByEmailContaining(@Param("emailPattern") String emailPattern);
}