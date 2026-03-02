package com.example.demo.services;

import com.example.demo.BoekRepository;
import com.example.demo.dto.BoekDto;
import org.springframework.stereotype.Service;

@Service
public class BoekService {

    private final BoekRepository boekRepository;

    public BoekService(BoekRepository boekRepository) {
        this.boekRepository = boekRepository;
    }

    public BoekDto importByIsbn(String isbn) {
        return null;
    }
}
