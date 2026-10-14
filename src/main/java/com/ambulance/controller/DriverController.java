package com.ambulance.controller;

import com.ambulance.config.CurrentUser;
import com.ambulance.dto.Dtos.AvailabilityRequest;
import com.ambulance.dto.Dtos.DriverProfile;
import com.ambulance.dto.Dtos.StatusRequest;
import com.ambulance.model.Ambulance;
import com.ambulance.model.Booking;
import com.ambulance.service.BookingService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/driver")
public class DriverController {

    private final BookingService service;
    private final CurrentUser currentUser;

    public DriverController(BookingService service, CurrentUser currentUser) {
        this.service = service;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public DriverProfile me(HttpSession session) {
        return service.driverProfile(currentUser.get(session));
    }

    @GetMapping("/bookings")
    public List<Booking> bookings(HttpSession session) {
        return service.forDriver(currentUser.get(session));
    }

    @PutMapping("/availability")
    public Ambulance availability(@Valid @RequestBody AvailabilityRequest req, HttpSession session) {
        return service.setAvailability(currentUser.get(session), req.available());
    }

    @PutMapping("/bookings/{id}/status")
    public Booking updateStatus(@PathVariable Long id,
                                @Valid @RequestBody StatusRequest req,
                                HttpSession session) {
        return service.updateStatus(currentUser.get(session), id, req.status());
    }
}
