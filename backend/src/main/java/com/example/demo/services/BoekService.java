package com.example.demo.services;

import com.example.demo.BoekRepository;
import com.example.demo.dto.BoekDto;
import com.example.demo.entities.Boek;
import com.example.demo.mappers.BoekMapper;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BoekService {

    private final BoekRepository boekRepository;

    public BoekService(BoekRepository boekRepository) {
        this.boekRepository = boekRepository;
    }

    public BoekDto importByIsbn(String isbn) {
        Optional<Boek> bestaand = boekRepository.findByIsbn(isbn);
        return bestaand.map(BoekMapper::toDto).orElse(null);
    }
}
