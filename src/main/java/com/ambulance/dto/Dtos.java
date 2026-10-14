package com.ambulance.dto;

import com.ambulance.model.Ambulance;
import com.ambulance.model.BookingStatus;
import com.ambulance.model.Role;
import com.ambulance.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** All request/response shapes in one place, as small records. */
public final class Dtos {

    private Dtos() {
    }

    public record RegisterRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Email is required")
            @Email(message = "Enter a valid email address") String email,
            @NotBlank(message = "Password is required")
            @Size(min = 6, message = "Password must be at least 6 characters") String password,
            @NotBlank(message = "Phone number is required")
            @Pattern(regexp = "\\d{10}", message = "Phone number must be 10 digits") String phone) {
    }

    public record LoginRequest(
            @NotBlank(message = "Email is required") String email,
            @NotBlank(message = "Password is required") String password) {
    }

    public record BookingRequest(
            @NotBlank(message = "Patient name is required") String patientName,
            @NotBlank(message = "Pickup address is required")
            @Size(max = 500, message = "Pickup address is too long") String pickupAddress,
            @NotNull(message = "Choose a hospital") Long hospitalId,
            @Size(max = 1000, message = "Emergency details are too long") String emergencyDetails) {
    }

    public record AmbulanceRequest(
            @NotBlank(message = "Vehicle number is required") String vehicleNumber,
            @NotBlank(message = "Ambulance type is required")
            @Pattern(regexp = "Basic|ICU", message = "Type must be Basic or ICU") String type,
            Long driverId) {
    }

    public record AssignDriverRequest(Long driverId) {
    }

    public record HospitalRequest(
            @NotBlank(message = "Hospital name is required") String name,
            @NotBlank(message = "Hospital address is required") String address,
            @NotBlank(message = "Hospital phone is required") String phone) {
    }

    public record AvailabilityRequest(
            @NotNull(message = "Availability is required") Boolean available) {
    }

    public record StatusRequest(
            @NotNull(message = "Status is required") BookingStatus status) {
    }

    /** Safe view of a user (no password). */
    public record UserInfo(Long id, String name, String email, String phone, Role role) {
        public static UserInfo from(User u) {
            return new UserInfo(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole());
        }
    }

    public record DriverProfile(String name, String phone, Ambulance ambulance, boolean onTrip) {
    }
}
