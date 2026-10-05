package com.gharse.notification;

import com.gharse.auth.AuthUser;
import com.gharse.common.ApiException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    public record NotificationResponse(UUID id, NotificationType type, String title, String message, boolean read,
                                       Instant createdAt) {}

    public record NotificationList(long unread, List<NotificationResponse> items) {}

    private final NotificationRepository repo;

    public NotificationController(NotificationRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public NotificationList mine() {
        UUID me = AuthUser.current().id();
        var items = repo.findByUserIdOrderByCreatedAtDesc(me, PageRequest.of(0, 50)).stream()
                .map(n -> new NotificationResponse(n.getId(), n.getType(), n.getTitle(), n.getMessage(), n.isRead(), n.getCreatedAt()))
                .toList();
        return new NotificationList(repo.countByUserIdAndReadFalse(me), items);
    }

    @PostMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        Notification n = repo.findById(id).filter(x -> x.getUserId().equals(AuthUser.current().id()))
                .orElseThrow(() -> ApiException.notFound("Notification"));
        n.setRead(true);
        return ResponseEntity.noContent().build();
    }
}
