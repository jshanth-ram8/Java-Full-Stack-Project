package com.ambulance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Ambulance ambulance;

    @ManyToOne
    private Hospital hospital;

    private String patientName;

    @Column(length = 500)
    private String pickupAddress;

    @Column(length = 1000)
    private String emergencyDetails;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingStatus status = BookingStatus.BOOKING_CONFIRMED;

    private Instant createdAt = Instant.now();
}
