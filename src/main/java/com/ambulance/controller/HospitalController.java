package com.ambulance.controller;

import com.ambulance.model.Hospital;
import com.ambulance.repository.HospitalRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/hospitals")
public class HospitalController {

    private final HospitalRepository hospitals;

    public HospitalController(HospitalRepository hospitals) {
        this.hospitals = hospitals;
    }

    @GetMapping
    public List<Hospital> list() {
        return hospitals.findAll(Sort.by("name"));
    }
}
