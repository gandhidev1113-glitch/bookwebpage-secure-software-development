package com.library.controller;

import com.library.model.BorrowRequest;
import com.library.model.User;
import com.library.service.BorrowService;
import com.library.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/borrow")
public class BorrowController {

    private final BorrowService borrowService;
    private final UserService userService;

    public BorrowController(BorrowService borrowService,
                            UserService userService) {
        this.borrowService = borrowService;
        this.userService = userService;
    }

    @PostMapping("/request/{bookId}")
    public ResponseEntity<?> requestBorrow(@PathVariable Long bookId,
                                           Authentication authentication) {
        // Get currently logged-in user from JWT token
        User user = userService.findByUsername(authentication.getName());
        BorrowRequest request = borrowService.requestBorrow(user, bookId);
        return ResponseEntity.ok(Map.of(
                "message", "Borrow request submitted",
                "requestId", request.getId(),
                "status", request.getStatus()
        ));
    }

    @GetMapping("/my-requests")
    public ResponseEntity<List<BorrowRequest>> myRequests(
            Authentication authentication) {
        User user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok(borrowService.getRequestsByUser(user));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<List<BorrowRequest>> pendingRequests() {
        return ResponseEntity.ok(borrowService.getPendingRequests());
    }

    @PutMapping("/approve/{requestId}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<?> approve(@PathVariable Long requestId) {
        BorrowRequest request = borrowService.approveRequest(requestId);
        return ResponseEntity.ok(Map.of(
                "message", "Request approved",
                "requestId", request.getId(),
                "status", request.getStatus()
        ));
    }

    @PutMapping("/reject/{requestId}")
    @PreAuthorize("hasRole('LIBRARIAN')")
    public ResponseEntity<?> reject(@PathVariable Long requestId) {
        BorrowRequest request = borrowService.rejectRequest(requestId);
        return ResponseEntity.ok(Map.of(
                "message", "Request rejected",
                "requestId", request.getId(),
                "status", request.getStatus()
        ));
    }
}