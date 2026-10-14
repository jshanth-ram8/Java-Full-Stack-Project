package com.ambulance.controller;

import com.ambulance.dto.Dtos.AmbulanceRequest;
import com.ambulance.dto.Dtos.AssignDriverRequest;
import com.ambulance.dto.Dtos.HospitalRequest;
import com.ambulance.dto.Dtos.RegisterRequest;
import com.ambulance.dto.Dtos.UserInfo;
import com.ambulance.exception.ApiException;
import com.ambulance.model.Ambulance;
import com.ambulance.model.Booking;
import com.ambulance.model.Hospital;
import com.ambulance.model.Role;
import com.ambulance.model.User;
import com.ambulance.repository.AmbulanceRepository;
import com.ambulance.repository.BookingRepository;
import com.ambulance.repository.HospitalRepository;
import com.ambulance.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository users;
    private final AmbulanceRepository ambulances;
    private final HospitalRepository hospitals;
    private final BookingRepository bookings;
    private final PasswordEncoder encoder;

    public AdminController(UserRepository users, AmbulanceRepository ambulances,
                           HospitalRepository hospitals, BookingRepository bookings,
                           PasswordEncoder encoder) {
        this.users = users;
        this.ambulances = ambulances;
        this.hospitals = hospitals;
        this.bookings = bookings;
        this.encoder = encoder;
    }

    // ------------------------------------------------------------ users and drivers

    @GetMapping("/users")
    public List<UserInfo> listUsers() {
        return users.findAll().stream().map(UserInfo::from).toList();
    }

    @PostMapping("/drivers")
    @ResponseStatus(HttpStatus.CREATED)
    public UserInfo addDriver(@Valid @RequestBody RegisterRequest req) {
        String email = req.email().trim().toLowerCase();
        if (users.findByEmail(email).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }
        User driver = new User();
        driver.setName(req.name().trim());
        driver.setEmail(email);
        driver.setPassword(encoder.encode(req.password()));
        driver.setPhone(req.phone());
        driver.setRole(Role.DRIVER);
        return UserInfo.from(users.save(driver));
    }

    @DeleteMapping("/users/{id}")
    public Map<String, String> deleteUser(@PathVariable Long id) {
        User user = users.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
        if (user.getRole() == Role.ADMIN) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Admin accounts can't be deleted");
        }
        users.delete(user);
        return Map.of("message", "User deleted");
    }

    // ------------------------------------------------------------ ambulances

    @GetMapping("/ambulances")
    public List<Ambulance> listAmbulances() {
        return ambulances.findAll();
    }

    @PostMapping("/ambulances")
    @ResponseStatus(HttpStatus.CREATED)
    public Ambulance addAmbulance(@Valid @RequestBody AmbulanceRequest req) {
        String plate = req.vehicleNumber().trim().toUpperCase();
        if (ambulances.existsByVehicleNumber(plate)) {
            throw new ApiException(HttpStatus.CONFLICT, "An ambulance with this vehicle number already exists");
        }
        Ambulance ambulance = new Ambulance();
        ambulance.setVehicleNumber(plate);
        ambulance.setType(req.type());
        if (req.driverId() != null) {
            ambulance.setDriver(resolveDriver(req.driverId(), null));
        }
        return ambulances.save(ambulance);
    }

    @PutMapping("/ambulances/{id}/driver")
    public Ambulance assignDriver(@PathVariable Long id, @RequestBody AssignDriverRequest req) {
        Ambulance ambulance = ambulances.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Ambulance not found"));
        ambulance.setDriver(req.driverId() == null ? null : resolveDriver(req.driverId(), id));
        return ambulances.save(ambulance);
    }

    @DeleteMapping("/ambulances/{id}")
    public Map<String, String> deleteAmbulance(@PathVariable Long id) {
        if (!ambulances.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Ambulance not found");
        }
        ambulances.deleteById(id);
        return Map.of("message", "Ambulance deleted");
    }

    // ------------------------------------------------------------ hospitals

    @PostMapping("/hospitals")
    @ResponseStatus(HttpStatus.CREATED)
    public Hospital addHospital(@Valid @RequestBody HospitalRequest req) {
        Hospital hospital = new Hospital();
        hospital.setName(req.name().trim());
        hospital.setAddress(req.address().trim());
        hospital.setPhone(req.phone().trim());
        return hospitals.save(hospital);
    }

    @DeleteMapping("/hospitals/{id}")
    public Map<String, String> deleteHospital(@PathVariable Long id) {
        if (!hospitals.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Hospital not found");
        }
        hospitals.deleteById(id);
        return Map.of("message", "Hospital deleted");
    }

    // ------------------------------------------------------------ bookings

    @GetMapping("/bookings")
    public List<Booking> listBookings() {
        return bookings.findAllByOrderByCreatedAtDesc();
    }

    // ------------------------------------------------------------ helpers

    /** Checks the user is a driver and doesn't already have a different ambulance. */
    private User resolveDriver(Long driverId, Long ambulanceId) {
        User driver = users.findById(driverId)
                .filter(u -> u.getRole() == Role.DRIVER)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Choose a valid driver"));
        boolean busy = ambulances.findFirstByDriverId(driverId)
                .filter(a -> !a.getId().equals(ambulanceId))
                .isPresent();
        if (busy) {
            throw new ApiException(HttpStatus.CONFLICT, "That driver already has an ambulance");
        }
        return driver;
    }
}
