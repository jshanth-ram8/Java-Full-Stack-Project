package com.ambulance.config;

import com.ambulance.exception.ApiException;
import com.ambulance.model.User;
import com.ambulance.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/** Looks up the logged-in user from the HTTP session. */
@Component
public class CurrentUser {

    private final UserRepository users;

    public CurrentUser(UserRepository users) {
        this.users = users;
    }

    public User get(HttpSession session) {
        Object id = session.getAttribute("userId");
        if (!(id instanceof Long)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue");
        }
        return users.findById((Long) id)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Please log in to continue"));
    }
}
