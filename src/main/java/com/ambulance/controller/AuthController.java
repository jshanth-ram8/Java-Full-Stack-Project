package com.ambulance.controller;

import com.ambulance.config.CurrentUser;
import com.ambulance.dto.Dtos.LoginRequest;
import com.ambulance.dto.Dtos.RegisterRequest;
import com.ambulance.dto.Dtos.UserInfo;
import com.ambulance.exception.ApiException;
import com.ambulance.model.Role;
import com.ambulance.model.User;
import com.ambulance.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final CurrentUser currentUser;

    public AuthController(UserRepository users, PasswordEncoder encoder, CurrentUser currentUser) {
        this.users = users;
        this.encoder = encoder;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserInfo register(@Valid @RequestBody RegisterRequest req, HttpSession session) {
        String email = req.email().trim().toLowerCase();
        if (users.findByEmail(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPassword(encoder.encode(req.password()));
        user.setPhone(req.phone());
        user.setRole(Role.USER);
        users.save(user);

        startSession(session, user);
        return UserInfo.from(user);
    }

    @PostMapping("/login")
    public UserInfo login(@Valid @RequestBody LoginRequest req, HttpSession session) {
        User user = users.findByEmail(req.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));
        if (!encoder.matches(req.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }
        startSession(session, user);
        return UserInfo.from(user);
    }

    @PostMapping("/logout")
    public Map<String, String> logout(HttpSession session) {
        session.invalidate();
        return Map.of("message", "Logged out");
    }

    @GetMapping("/me")
    public UserInfo me(HttpSession session) {
        return UserInfo.from(currentUser.get(session));
    }

    private void startSession(HttpSession session, User user) {
        session.setAttribute("userId", user.getId());
        session.setAttribute("role", user.getRole().name());
    }
}
