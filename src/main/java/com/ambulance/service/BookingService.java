package com.ambulance.service;

import com.ambulance.dto.Dtos.BookingRequest;
import com.ambulance.dto.Dtos.DriverProfile;
import com.ambulance.exception.ApiException;
import com.ambulance.model.Ambulance;
import com.ambulance.model.Booking;
import com.ambulance.model.BookingStatus;
import com.ambulance.model.Hospital;
import com.ambulance.model.User;
import com.ambulance.repository.AmbulanceRepository;
import com.ambulance.repository.BookingRepository;
import com.ambulance.repository.HospitalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BookingService {

    /** The order a trip moves through. */
    private static final List<BookingStatus> FLOW = List.of(
            BookingStatus.BOOKING_CONFIRMED,
            BookingStatus.AMBULANCE_DISPATCHED,
            BookingStatus.PATIENT_PICKED_UP,
            BookingStatus.TRIP_COMPLETED);

    /** Statuses where the ambulance is still busy. */
    private static final List<BookingStatus> ACTIVE = List.of(
            BookingStatus.BOOKING_CONFIRMED,
            BookingStatus.AMBULANCE_DISPATCHED,
            BookingStatus.PATIENT_PICKED_UP);

    private final BookingRepository bookings;
    private final AmbulanceRepository ambulances;
    private final HospitalRepository hospitals;

    public BookingService(BookingRepository bookings, AmbulanceRepository ambulances, HospitalRepository hospitals) {
        this.bookings = bookings;
        this.ambulances = ambulances;
        this.hospitals = hospitals;
    }

    // ---------------------------------------------------------------- patient side

    /** Creates a booking and assigns the first free ambulance. */
    @Transactional
    public Booking create(User user, BookingRequest req) {
        Hospital hospital = hospitals.findById(req.hospitalId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Hospital not found"));

        Ambulance ambulance = ambulances.findFirstByAvailableTrueAndDriverIsNotNull()
                .orElseThrow(() -> new ApiException(HttpStatus.CONFLICT,
                        "No ambulance is free right now. Please call 108 or 112."));

        ambulance.setAvailable(false);
        ambulances.save(ambulance);

        Booking booking = new Booking();
        booking.setUser(user);
        booking.setAmbulance(ambulance);
        booking.setHospital(hospital);
        booking.setPatientName(req.patientName().trim());
        booking.setPickupAddress(req.pickupAddress().trim());
        booking.setEmergencyDetails(req.emergencyDetails() == null ? "" : req.emergencyDetails().trim());
        booking.setStatus(BookingStatus.BOOKING_CONFIRMED);
        return bookings.save(booking);
    }

    public List<Booking> forUser(User user) {
        return bookings.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional
    public Booking cancel(User user, Long bookingId) {
        Booking booking = find(bookingId);
        if (!booking.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Booking not found");
        }
        BookingStatus s = booking.getStatus();
        if (s != BookingStatus.BOOKING_CONFIRMED && s != BookingStatus.AMBULANCE_DISPATCHED) {
            throw new ApiException(HttpStatus.CONFLICT, "This trip can no longer be cancelled");
        }
        booking.setStatus(BookingStatus.CANCELLED);
        freeAmbulance(booking.getAmbulance());
        return bookings.save(booking);
    }

    // ---------------------------------------------------------------- driver side

    public DriverProfile driverProfile(User driver) {
        Ambulance ambulance = ambulances.findFirstByDriverId(driver.getId()).orElse(null);
        boolean onTrip = ambulance != null
                && bookings.existsByAmbulanceIdAndStatusIn(ambulance.getId(), ACTIVE);
        return new DriverProfile(driver.getName(), driver.getPhone(), ambulance, onTrip);
    }

    public List<Booking> forDriver(User driver) {
        return bookings.findByAmbulanceDriverIdOrderByCreatedAtDesc(driver.getId());
    }

    @Transactional
    public Ambulance setAvailability(User driver, boolean available) {
        Ambulance ambulance = ambulances.findFirstByDriverId(driver.getId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND,
                        "No ambulance is assigned to you yet. Ask the admin."));
        if (available && bookings.existsByAmbulanceIdAndStatusIn(ambulance.getId(), ACTIVE)) {
            throw new ApiException(HttpStatus.CONFLICT, "Finish your current trip before going available");
        }
        ambulance.setAvailable(available);
        return ambulances.save(ambulance);
    }

    /** Moves a trip one step forward. Steps can't be skipped. */
    @Transactional
    public Booking updateStatus(User driver, Long bookingId, BookingStatus next) {
        Booking booking = find(bookingId);
        Ambulance ambulance = booking.getAmbulance();
        if (ambulance == null || ambulance.getDriver() == null
                || !ambulance.getDriver().getId().equals(driver.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This booking is not assigned to you");
        }

        int index = FLOW.indexOf(booking.getStatus());
        if (index < 0 || index == FLOW.size() - 1) {
            throw new ApiException(HttpStatus.CONFLICT, "This trip is already closed");
        }
        if (next != FLOW.get(index + 1)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Steps must be followed in order");
        }

        booking.setStatus(next);
        if (next == BookingStatus.TRIP_COMPLETED) {
            freeAmbulance(ambulance);
        }
        return bookings.save(booking);
    }

    // ---------------------------------------------------------------- helpers

    private Booking find(Long id) {
        return bookings.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Booking not found"));
    }

    private void freeAmbulance(Ambulance ambulance) {
        if (ambulance != null) {
            ambulance.setAvailable(true);
            ambulances.save(ambulance);
        }
    }
}
