package com.ambulance.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Ambulance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String vehicleNumber;

    /** "Basic" or "ICU" */
    private String type;

    private boolean available = true;

    @ManyToOne
    @JoinColumn(name = "driver_id")
    private User driver;
}
