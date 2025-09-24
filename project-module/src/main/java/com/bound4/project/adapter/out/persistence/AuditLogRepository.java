package com.bound4.project.adapter.out.persistence;

import com.bound4.project.domain.AuditAction;
import com.bound4.project.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    List<AuditLog> findByAdminUserIdOrderByCreatedAtDesc(Long adminUserId);

    List<AuditLog> findByTargetUserIdOrderByCreatedAtDesc(Long targetUserId);

    List<AuditLog> findByActionOrderByCreatedAtDesc(AuditAction action);

    @Query("SELECT a FROM AuditLog a WHERE a.createdAt >= :fromDate ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentAuditLogs(@Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType AND a.entityId = :entityId ORDER BY a.createdAt DESC")
    List<AuditLog> findByEntityTypeAndEntityId(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    @Query("SELECT a FROM AuditLog a WHERE a.action IN :actions ORDER BY a.createdAt DESC")
    Page<AuditLog> findByActionsOrderByCreatedAtDesc(@Param("actions") List<AuditAction> actions, Pageable pageable);

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.adminUserId = :adminUserId AND a.createdAt >= :fromDate")
    long countByAdminUserIdAndCreatedAtAfter(@Param("adminUserId") Long adminUserId, @Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT a FROM AuditLog a WHERE a.adminUserId = :adminUserId AND a.action = :action ORDER BY a.createdAt DESC")
    List<AuditLog> findByAdminUserIdAndAction(@Param("adminUserId") Long adminUserId, @Param("action") AuditAction action);
}