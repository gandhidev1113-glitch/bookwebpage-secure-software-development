package com.library.controller;

import com.library.config.JwtService;
import com.library.config.SecurityLogger;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.model.Role;
import com.library.model.User;
import com.library.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SecurityLogger securityLogger;
    private final String librarianBootstrapToken;

    public AuthController(UserService userService,
                          JwtService jwtService,
                          AuthenticationManager authenticationManager,
                          SecurityLogger securityLogger,
                          @Value("${app.bootstrap.librarian-token:}") String librarianBootstrapToken) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.securityLogger = securityLogger;
        this.librarianBootstrapToken = librarianBootstrapToken;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                Role.USER
        );
        securityLogger.logRegistration(user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
                "username", user.getUsername(),
                "role", user.getRole()
        ));
    }

    @PostMapping("/register-librarian")
    public ResponseEntity<?> registerLibrarian(
            @Valid @RequestBody RegisterRequest request,
            @RequestHeader(value = "X-Setup-Token", required = false) String setupToken
    ) {
        if (userService.hasAnyLibrarian()) {
            return ResponseEntity.status(409)
                    .body(Map.of("error", "Librarian bootstrap already completed"));
        }

        if (librarianBootstrapToken == null || librarianBootstrapToken.isBlank()) {
            return ResponseEntity.status(503)
                    .body(Map.of("error", "Librarian bootstrap is disabled"));
        }

        if (!tokensMatch(setupToken, librarianBootstrapToken)) {
            securityLogger.logFailedLibrarianBootstrap(request.getUsername());
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid setup token"));
        }

        User user = userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                Role.LIBRARIAN
        );
        securityLogger.logRegistration(user.getUsername(), user.getRole().name());
        return ResponseEntity.status(201).body(Map.of(
                "message", "Librarian registered successfully",
                "username", user.getUsername(),
                "role", user.getRole()
        ));
    }

    @PostMapping("/create-librarian")
    @PreAuthorize("hasAuthority('ROLE_LIBRARIAN')")
    public ResponseEntity<?> createLibrarian(
            @Valid @RequestBody RegisterRequest request,
            Authentication authentication
    ) {
        User user = userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                Role.LIBRARIAN
        );
        securityLogger.logRegistration(user.getUsername(), user.getRole().name());
        return ResponseEntity.status(201).body(Map.of(
                "message", "Librarian created successfully",
                "createdBy", authentication.getName(),
                "username", user.getUsername(),
                "role", user.getRole()
        ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );
        } catch (BadCredentialsException e) {
            securityLogger.logFailedLogin(request.getUsername());
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid credentials"));
        }

        UserDetails userDetails =
                userService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(userDetails);
        securityLogger.logLogin(request.getUsername());

        return ResponseEntity.ok(Map.of("token", token));
    }

    @GetMapping("/me")
    public ResponseEntity<?> me(Authentication authentication) {
        return ResponseEntity.ok(Map.of(
                "username", authentication.getName(),
                "authorities", authentication.getAuthorities().toString()
        ));
    }

    private boolean tokensMatch(String provided, String expected) {
        if (provided == null || expected == null) {
            return false;
        }
        return MessageDigest.isEqual(
                provided.getBytes(StandardCharsets.UTF_8),
                expected.getBytes(StandardCharsets.UTF_8)
        );
    }
}
