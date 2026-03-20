package com.library.controller;

import com.library.config.SecurityLogger;
import com.library.model.BorrowRequest;
import com.library.model.User;
import com.library.service.BorrowService;
import com.library.service.UserService;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrow")
@Validated
public class BorrowController {

    private final BorrowService borrowService;
    private final UserService userService;
    private final SecurityLogger securityLogger;

    public BorrowController(BorrowService borrowService,
                            UserService userService,
                            SecurityLogger securityLogger) {
        this.borrowService = borrowService;
        this.userService = userService;
        this.securityLogger = securityLogger;
    }

    @PostMapping("/request/{bookId}")
    public ResponseEntity<?> requestBorrow(@PathVariable @Positive Long bookId,
                                           Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        BorrowRequest request = borrowService.requestBorrow(user, bookId);
        return ResponseEntity.ok(Map.of(
                "message", "Borrow request submitted",
                "requestId", request.getId(),
                "status", request.getStatus()
        ));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<BorrowRequest>> myRequests(
            Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(borrowService.getRequestsByUser(user));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('ROLE_LIBRARIAN')")
    public ResponseEntity<List<BorrowRequest>> pendingRequests() {
        return ResponseEntity.ok(borrowService.getPendingRequests());
    }

    @PutMapping("/approve/{requestId}")
    @PreAuthorize("hasAuthority('ROLE_LIBRARIAN')")
    public ResponseEntity<?> approve(@PathVariable @Positive Long requestId,
                                     Authentication authentication) {
        BorrowRequest request = borrowService.approveRequest(requestId);
        securityLogger.logBorrowApproved(authentication.getName(), requestId);
        return ResponseEntity.ok(Map.of(
                "message", "Request approved",
                "requestId", request.getId(),
                "status", request.getStatus()
        ));
    }

    @PutMapping("/reject/{requestId}")
    @PreAuthorize("hasAuthority('ROLE_LIBRARIAN')")
    public ResponseEntity<?> reject(@PathVariable @Positive Long requestId,
                                    Authentication authentication) {
        BorrowRequest request = borrowService.rejectRequest(requestId);
        securityLogger.logBorrowRejected(authentication.getName(), requestId);
        return ResponseEntity.ok(Map.of(
                "message", "Request rejected",
                "requestId", request.getId(),
                "status", request.getStatus()
        ));
    }
}
