package com.ambulance.repository;

import com.ambulance.model.Ambulance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AmbulanceRepository extends JpaRepository<Ambulance, Long> {

    /** First free ambulance that actually has a driver. */
    Optional<Ambulance> findFirstByAvailableTrueAndDriverIsNotNull();

    Optional<Ambulance> findFirstByDriverId(Long driverId);

    boolean existsByVehicleNumber(String vehicleNumber);
}
