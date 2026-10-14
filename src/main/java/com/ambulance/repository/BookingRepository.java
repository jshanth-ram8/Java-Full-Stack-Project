package com.ambulance.repository;

import com.ambulance.model.Booking;
import com.ambulance.model.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findByUserIdOrderByCreatedAtDesc(Long userId);

    List<Booking> findByAmbulanceDriverIdOrderByCreatedAtDesc(Long driverId);

    List<Booking> findAllByOrderByCreatedAtDesc();

    boolean existsByAmbulanceIdAndStatusIn(Long ambulanceId, Collection<BookingStatus> statuses);
}
