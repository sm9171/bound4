package com.bound4.project.adapter.out.persistence;

import com.bound4.project.domain.NotificationType;
import com.bound4.project.domain.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserNotificationRepository extends JpaRepository<UserNotification, Long> {

    List<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<UserNotification> findByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId);

    @Query("SELECT COUNT(n) FROM UserNotification n WHERE n.userId = :userId AND n.read = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    List<UserNotification> findByUserIdAndTypeOrderByCreatedAtDesc(Long userId, NotificationType type);

    @Query("SELECT n FROM UserNotification n WHERE n.userId = :userId AND n.createdAt >= :fromDate ORDER BY n.createdAt DESC")
    List<UserNotification> findRecentNotifications(@Param("userId") Long userId, @Param("fromDate") LocalDateTime fromDate);

    @Query("SELECT n FROM UserNotification n WHERE n.adminUserId = :adminUserId ORDER BY n.createdAt DESC")
    List<UserNotification> findByAdminUserId(@Param("adminUserId") Long adminUserId);

    void deleteByUserIdAndReadTrueAndCreatedAtBefore(Long userId, LocalDateTime beforeDate);
}