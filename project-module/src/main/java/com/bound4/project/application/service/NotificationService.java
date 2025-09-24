package com.bound4.project.application.service;

import com.bound4.project.adapter.out.persistence.UserNotificationRepository;
import com.bound4.project.domain.UserNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final UserNotificationRepository notificationRepository;

    public NotificationService(UserNotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public void notifyRoleChanged(Long userId, Long adminUserId, String oldRole, String newRole, String reason) {
        log.info("역할 변경 알림 전송: userId={}, oldRole={}, newRole={}", userId, oldRole, newRole);
        
        UserNotification notification = UserNotification.roleChanged(userId, adminUserId, oldRole, newRole, reason);
        notificationRepository.save(notification);
    }

    public void notifyPermissionChanged(Long userId, Long adminUserId, String resource, String action, boolean allowed) {
        log.info("권한 변경 알림 전송: userId={}, resource={}, action={}, allowed={}", 
                userId, resource, action, allowed);
        
        UserNotification notification = UserNotification.permissionChanged(userId, adminUserId, resource, action, allowed);
        notificationRepository.save(notification);
    }

    public void notifySystemNotice(Long userId, String title, String message) {
        log.info("시스템 공지 알림 전송: userId={}, title={}", userId, title);
        
        UserNotification notification = UserNotification.systemNotice(userId, title, message);
        notificationRepository.save(notification);
    }

    public void broadcastSystemNotice(List<Long> userIds, String title, String message) {
        log.info("시스템 공지 일괄 전송: userCount={}, title={}", userIds.size(), title);
        
        List<UserNotification> notifications = userIds.stream()
                .map(userId -> UserNotification.systemNotice(userId, title, message))
                .toList();
        
        notificationRepository.saveAll(notifications);
    }

    @Transactional(readOnly = true)
    public List<UserNotification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<UserNotification> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId)
                .filter(n -> n.getUserId().equals(userId))
                .ifPresent(notification -> {
                    notification.markAsRead();
                    notificationRepository.save(notification);
                    log.debug("알림 읽음 처리: notificationId={}, userId={}", notificationId, userId);
                });
    }

    public void markAllAsRead(Long userId) {
        List<UserNotification> unreadNotifications = notificationRepository.findByUserIdAndReadFalseOrderByCreatedAtDesc(userId);
        unreadNotifications.forEach(UserNotification::markAsRead);
        notificationRepository.saveAll(unreadNotifications);
        
        log.info("모든 알림 읽음 처리: userId={}, count={}", userId, unreadNotifications.size());
    }

    public void cleanupOldNotifications(Long userId, int daysToKeep) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(daysToKeep);
        notificationRepository.deleteByUserIdAndReadTrueAndCreatedAtBefore(userId, cutoffDate);
        
        log.info("오래된 알림 정리 완료: userId={}, cutoffDate={}", userId, cutoffDate);
    }
}