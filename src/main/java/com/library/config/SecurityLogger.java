package com.library.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SecurityLogger {

    private static final Logger log = LoggerFactory.getLogger("SECURITY");

    public void logLogin(String username) {
        log.info("[AUTH] User logged in: {}", username);
    }

    public void logFailedLogin(String username) {
        log.warn("[AUTH] Failed login attempt for: {}", username);
    }

    public void logRegistration(String username, String role) {
        log.info("[AUTH] New user registered: {} with role: {}", username, role);
    }

    public void logBookAdded(String username, String title) {
        log.info("[ADMIN] Book added by {}: {}", username, title);
    }

    public void logBookDeleted(String username, Long bookId) {
        log.info("[ADMIN] Book deleted by {}: id={}", username, bookId);
    }

    public void logBorrowApproved(String username, Long requestId) {
        log.info("[ADMIN] Borrow request {} approved by {}", requestId, username);
    }

    public void logBorrowRejected(String username, Long requestId) {
        log.info("[ADMIN] Borrow request {} rejected by {}", requestId, username);
    }
}