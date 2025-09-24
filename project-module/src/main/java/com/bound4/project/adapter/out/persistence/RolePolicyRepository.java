package com.bound4.project.adapter.out.persistence;

import com.bound4.project.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolePolicyRepository extends JpaRepository<RolePolicy, Long> {

    Optional<RolePolicy> findByRoleAndResourceAndActionAndSubscriptionPlan(
        Role role, Resource resource, Action action, SubscriptionPlan subscriptionPlan);

    List<RolePolicy> findByRole(Role role);

    List<RolePolicy> findByRoleAndSubscriptionPlan(Role role, SubscriptionPlan subscriptionPlan);

    List<RolePolicy> findByResource(Resource resource);

    List<RolePolicy> findByResourceAndAction(Resource resource, Action action);

    @Query("SELECT rp FROM RolePolicy rp WHERE rp.role = :role AND rp.subscriptionPlan = :plan AND rp.allowed = true")
    List<RolePolicy> findAllowedPolicies(@Param("role") Role role, 
                                       @Param("plan") SubscriptionPlan plan);

    @Query("SELECT rp FROM RolePolicy rp WHERE rp.role = :role AND rp.subscriptionPlan = :plan AND rp.allowed = false")
    List<RolePolicy> findDeniedPolicies(@Param("role") Role role, 
                                      @Param("plan") SubscriptionPlan plan);

    List<RolePolicy> findBySystemPolicy(boolean systemPolicy);

    @Query("SELECT rp FROM RolePolicy rp WHERE rp.systemPolicy = false")
    List<RolePolicy> findCustomPolicies();

    @Query("SELECT rp FROM RolePolicy rp WHERE rp.systemPolicy = true")
    List<RolePolicy> findSystemPolicies();

    @Query("SELECT rp FROM RolePolicy rp WHERE rp.role = :role AND rp.resource = :resource AND rp.allowed = true")
    List<RolePolicy> findAllowedPoliciesByRoleAndResource(@Param("role") Role role, 
                                                        @Param("resource") Resource resource);

    @Query("SELECT DISTINCT rp.resource FROM RolePolicy rp WHERE rp.role = :role AND rp.subscriptionPlan = :plan AND rp.allowed = true")
    List<Resource> findAllowedResourcesByRoleAndPlan(@Param("role") Role role, 
                                                   @Param("plan") SubscriptionPlan plan);

    @Query("SELECT DISTINCT rp.action FROM RolePolicy rp WHERE rp.role = :role AND rp.subscriptionPlan = :plan AND rp.resource = :resource AND rp.allowed = true")
    List<Action> findAllowedActionsByRoleAndPlanAndResource(@Param("role") Role role, 
                                                          @Param("plan") SubscriptionPlan plan,
                                                          @Param("resource") Resource resource);

    boolean existsByRoleAndResourceAndActionAndSubscriptionPlan(
        Role role, Resource resource, Action action, SubscriptionPlan subscriptionPlan);

    @Query("SELECT COUNT(rp) FROM RolePolicy rp WHERE rp.systemPolicy = false")
    long countCustomPolicies();

    @Query("SELECT COUNT(rp) FROM RolePolicy rp WHERE rp.role = :role AND rp.allowed = true")
    long countAllowedPoliciesByRole(@Param("role") Role role);
}