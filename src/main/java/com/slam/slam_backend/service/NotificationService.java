package com.slam.slam_backend.service;

import com.slam.slam_backend.entity.Notification;
import com.slam.slam_backend.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    public List<Notification> getNotificationsByUserId(String userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getUnreadNotificationsByUserId(String userId) {
        return notificationRepository.findByUserIdAndReadOrderByCreatedAtDesc(userId, false);
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countByUserIdAndRead(userId, false);
    }

    public Notification createNotification(String userId, String type, String message, String data) {
        Notification notification = new Notification(userId, type, message, data);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAsRead(Long notificationId, String userId) {
        notificationRepository.markAsReadByIdAndUserId(notificationId, userId);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    // Helper methods for creating specific notification types
    public void createMembershipNotification(String userId, String branch, boolean approved) {
        String type = approved ? "membership_approved" : "membership_rejected";
        String message = approved 
            ? "Your " + branch + " membership has been approved!"
            : "Your " + branch + " membership application was not approved.";
        String data = "{\"branch\": \"" + branch + "\"}";
        createNotification(userId, type, message, data);
    }

    public void createCommentNotification(String userId, String authorName, String postTitle, Long postId) {
        String type = "comment";
        String message = authorName + " commented on your post: \"" + postTitle + "\"";
        String data = "{\"author\": \"" + authorName + "\", \"postTitle\": \"" + postTitle + "\", \"postId\": " + postId + "}";
        createNotification(userId, type, message, data);
    }

    public void createLikeNotification(String userId, String authorName, String targetType) {
        String type = "like";
        String message = authorName + " liked your " + targetType;
        String data = "{\"author\": \"" + authorName + "\", \"targetType\": \"" + targetType + "\"}";
        createNotification(userId, type, message, data);
    }

    public void createEventReminderNotification(String userId, String eventTitle, Long eventId) {
        String type = "event_reminder";
        String message = "Reminder: \"" + eventTitle + "\" starts in 1 hour";
        String data = "{\"eventTitle\": \"" + eventTitle + "\", \"eventId\": " + eventId + "}";
        createNotification(userId, type, message, data);
    }
}
