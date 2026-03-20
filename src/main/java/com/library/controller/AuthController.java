package com.library.controller;

import com.library.config.JwtService;
import com.library.config.SecurityLogger;
import com.library.dto.LoginRequest;
import com.library.dto.RegisterRequest;
import com.library.model.Role;
import com.library.model.User;
import com.library.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final SecurityLogger securityLogger;

    public AuthController(UserService userService,
                          JwtService jwtService,
                          AuthenticationManager authenticationManager,
                          SecurityLogger securityLogger) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.securityLogger = securityLogger;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        Role role = Role.USER;
        if ("LIBRARIAN".equalsIgnoreCase(request.getRole())) {
            role = Role.LIBRARIAN;
        }
        User user = userService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword(),
                role
        );
        securityLogger.logRegistration(user.getUsername(), user.getRole().name());
        return ResponseEntity.ok(Map.of(
                "message", "User registered successfully",
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

            // Generic message — never reveal if username or password is wrong

            securityLogger.logFailedLogin(request.getUsername());
//>>>>>>> 5d23a5541c0f1644befed79d29e16141df738aee
            return ResponseEntity.status(401)
                    .body(Map.of("error", "Invalid credentials"));
        }

        UserDetails userDetails =
                userService.loadUserByUsername(request.getUsername());
        String token = jwtService.generateToken(userDetails);
        securityLogger.logLogin(request.getUsername());

        return ResponseEntity.ok(Map.of("token", token));
    }
}