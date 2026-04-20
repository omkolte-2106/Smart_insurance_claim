package com.smartinsure.controller;

import com.smartinsure.entity.Notification;
import com.smartinsure.repository.NotificationRepository;
import com.smartinsure.service.CurrentUserService;
import com.smartinsure.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final CurrentUserService currentUserService;
    private final NotificationRepository notificationRepository;

    @GetMapping
    public Page<Notification> list(Pageable pageable) {
        return notificationService.forUser(currentUserService.currentUser().getId(), pageable);
    }

    @PostMapping("/{id}/read")
    public void markRead(@PathVariable Long id) {
        notificationService.markRead(id, currentUserService.currentUser().getId());
    }

    @GetMapping("/unread-count")
    public long unread() {
        return notificationRepository.countByUserIdAndReadFlagFalse(currentUserService.currentUser().getId());
    }
}
