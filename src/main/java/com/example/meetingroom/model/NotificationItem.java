package com.example.meetingroom.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.time.LocalDateTime;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class NotificationItem {
    private Long id;
    private String recipientUsername;
    private String title;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public NotificationItem() {
    }

    public NotificationItem(Long id, String recipientUsername, String title, String message) {
        this.id = id;
        this.recipientUsername = recipientUsername;
        this.title = title;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getRecipientUsername() {
        return recipientUsername;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public boolean isRead() {
        return readAt != null;
    }

    public void markRead() {
        if (readAt == null) {
            readAt = LocalDateTime.now();
        }
    }
}
