package com.querySense.auth;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserDetailsService userDetailsService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(UserDetailsService userDetailsService,
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          UserRepository userRepository) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        if (request.username() == null || request.username().isBlank()
                || request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Username and password are required."));
        }
        if (userRepository.existsByUsername(request.username())) {
            return ResponseEntity.status(409)   // 409 Conflict
                    .body(Map.of("error", "Username already taken."));
        }

        UserEntity user = new UserEntity(
                request.username(),
                passwordEncoder.encode(request.password()),   // hash before saving
                "USER"
        );
        userRepository.save(user);

        return ResponseEntity.status(201)   // 201 Created
                .body(Map.of("message", "User registered successfully."));
    }

    @PostMapping("/login")
    public ResponseEntity<Map<String, String>> login(@RequestBody LoginRequest request) {
        UserDetails user;
        try {
            user = userDetailsService.loadUserByUsername(request.username());
        }  catch (Exception e) {
        return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
    }
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid credentials"));
        }

        String role = user.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("USER");

        String token = jwtService.generateToken(user.getUsername(), role);
        return ResponseEntity.ok(Map.of("token", token));
    }
}