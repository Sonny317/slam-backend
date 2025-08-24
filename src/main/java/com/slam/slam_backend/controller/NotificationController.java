package com.slam.slam_backend.controller;

import com.slam.slam_backend.entity.Notification;
import com.slam.slam_backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<?> getUserNotifications(Authentication authentication) {
        try {
            System.out.println("Notification API called - Getting user notifications");
            
            if (authentication == null || !authentication.isAuthenticated()) {
                System.out.println("No authentication found for notifications");
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            String userEmail = authentication.getName();
            System.out.println("Fetching notifications for user: " + userEmail);
            
            List<Notification> notifications = notificationService.getNotificationsByUserId(userEmail);
            System.out.println("Found " + notifications.size() + " notifications");
            
            return ResponseEntity.ok(notifications != null ? notifications : new java.util.ArrayList<>());
        } catch (Exception e) {
            System.err.println("Error in getUserNotifications: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch notifications",
                "message", e.getMessage() != null ? e.getMessage() : "Unknown error",
                "type", e.getClass().getSimpleName()
            ));
        }
    }

    @GetMapping("/unread")
    public ResponseEntity<?> getUnreadNotifications(Authentication authentication) {
        try {
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
            }

            String userEmail = authentication.getName();
            List<Notification> notifications = notificationService.getUnreadNotificationsByUserId(userEmail);
            return ResponseEntity.ok(notifications != null ? notifications : new java.util.ArrayList<>());
        } catch (Exception e) {
            System.err.println("Error in getUnreadNotifications: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                "error", "Failed to fetch unread notifications",
                "message", e.getMessage() != null ? e.getMessage() : "Unknown error"
            ));
        }
    }

    @GetMapping("/unread/count")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userEmail = authentication.getName();
        long count = notificationService.getUnreadCount(userEmail);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userEmail = authentication.getName();
        notificationService.markAsRead(id, userEmail);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/mark-all-read")
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userEmail = authentication.getName();
        notificationService.markAllAsRead(userEmail);
        return ResponseEntity.ok().build();
    }

    // Create notification endpoint
    @PostMapping
    public ResponseEntity<Notification> createNotification(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String recipientEmail = (String) request.get("recipientEmail");
        String type = (String) request.get("type");
        String message = (String) request.get("message");
        Map<String, Object> data = (Map<String, Object>) request.get("data");
        String dataString = data != null ? data.toString() : null;

        Notification notification = notificationService.createNotification(recipientEmail, type, message, dataString);
        return ResponseEntity.ok(notification);
    }

    // Admin endpoint to create test notifications
    @PostMapping("/test")
    public ResponseEntity<Notification> createTestNotification(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {
        
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        String userEmail = authentication.getName();
        String type = (String) request.get("type");
        String message = (String) request.get("message");
        String data = (String) request.get("data");

        Notification notification = notificationService.createNotification(userEmail, type, message, data);
        return ResponseEntity.ok(notification);
    }
}
