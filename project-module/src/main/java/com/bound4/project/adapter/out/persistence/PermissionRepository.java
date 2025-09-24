package com.bound4.project.adapter.out.persistence;

import com.bound4.project.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {

    Optional<Permission> findByResourceAndActionAndRoleAndSubscriptionPlan(
        Resource resource, Action action, Role role, SubscriptionPlan subscriptionPlan);

    List<Permission> findByRole(Role role);

    List<Permission> findBySubscriptionPlan(SubscriptionPlan subscriptionPlan);

    List<Permission> findByRoleAndSubscriptionPlan(Role role, SubscriptionPlan subscriptionPlan);

    List<Permission> findByResource(Resource resource);

    List<Permission> findByAction(Action action);

    List<Permission> findByResourceAndAction(Resource resource, Action action);

    @Query("SELECT p FROM Permission p WHERE p.role = :role AND p.subscriptionPlan = :plan AND p.allowed = true")
    List<Permission> findAllowedPermissions(@Param("role") Role role, 
                                          @Param("plan") SubscriptionPlan plan);

    @Query("SELECT p FROM Permission p WHERE p.role = :role AND p.subscriptionPlan = :plan AND p.allowed = false")
    List<Permission> findDeniedPermissions(@Param("role") Role role, 
                                         @Param("plan") SubscriptionPlan plan);

    @Query("SELECT p FROM Permission p WHERE p.resource = :resource AND p.allowed = :allowed")
    List<Permission> findByResourceAndAllowed(@Param("resource") Resource resource, 
                                            @Param("allowed") boolean allowed);

    @Query("SELECT DISTINCT p.resource FROM Permission p WHERE p.role = :role AND p.subscriptionPlan = :plan AND p.allowed = true")
    List<Resource> findAllowedResourcesByRoleAndPlan(@Param("role") Role role, 
                                                   @Param("plan") SubscriptionPlan plan);

    @Query("SELECT DISTINCT p.action FROM Permission p WHERE p.role = :role AND p.subscriptionPlan = :plan AND p.resource = :resource AND p.allowed = true")
    List<Action> findAllowedActionsByRoleAndPlanAndResource(@Param("role") Role role, 
                                                          @Param("plan") SubscriptionPlan plan,
                                                          @Param("resource") Resource resource);

    boolean existsByResourceAndActionAndRoleAndSubscriptionPlan(
        Resource resource, Action action, Role role, SubscriptionPlan subscriptionPlan);
}