package com.smartinsure.service;

import com.smartinsure.entity.AppUser;
import com.smartinsure.entity.Notification;
import com.smartinsure.exception.ApiException;
import com.smartinsure.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public void notify(AppUser user, String title, String body) {
        Notification n = Notification.builder()
                .user(user)
                .title(title)
                .body(body)
                .readFlag(false)
                .build();
        notificationRepository.save(n);
    }

    @Transactional(readOnly = true)
    public Page<Notification> forUser(Long userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification n = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notification not found"));
        if (!n.getUser().getId().equals(userId)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cannot update notification");
        }
        n.setReadFlag(true);
    }
}
