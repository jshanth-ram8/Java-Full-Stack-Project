package com.ambulance.controller;

import com.ambulance.config.CurrentUser;
import com.ambulance.dto.Dtos.BookingRequest;
import com.ambulance.model.Booking;
import com.ambulance.service.BookingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService service;
    private final CurrentUser currentUser;

    public BookingController(BookingService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Booking create(@Valid @RequestBody BookingRequest req, HttpSession session) {
        return service.create(currentUser.get(session), req);
    }

    @GetMapping("/my")
    public List<Booking> mine(HttpSession session) {
        return service.forUser(currentUser.get(session));
    }

    @PutMapping("/{id}/cancel")
    public Booking cancel(@PathVariable Long id, HttpSession session) {
        return service.cancel(currentUser.get(session), id);
    }
}
