package com.gharse.notification;

import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** MVP implementation: in-app notifications stored in PostgreSQL. */
@Service
public class DatabaseNotificationService implements NotificationService {

    private final NotificationRepository repo;

    public DatabaseNotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED)
    public void notify(UUID userId, NotificationType type, String title, String message) {
        Notification n = new Notification();
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        repo.save(n);
    }
}
