package com.gharse.notification;

import java.util.UUID;

/** Extension point: add WhatsApp/SMS/email implementations later without touching callers. */
public interface NotificationService {

    void notify(UUID userId, NotificationType type, String title, String message);
}
