package com.example.demo.mappers;

import com.example.demo.dto.BoekDto;
import com.example.demo.entities.Boek;

public class BoekMapper {

    public static BoekDto toDto(Boek boek) {
        if (boek == null) {
            return null;
        }

        BoekDto dto = new BoekDto();
        dto.setId(boek.getId());
        dto.setTitel(boek.getTitel());
        dto.setAuteur(boek.getAuteur());
        dto.setCover(boek.getCover());
        dto.setBeschrijving(boek.getBeschrijving());
        dto.setGenre(boek.getGenre());
        dto.setUitgaveDatum(boek.getUitgaveDatum());
        dto.setPaginas(boek.getPaginas());
        dto.setTaal(boek.getTaal());
        dto.setUitgeverij(boek.getUitgeverij());

        return dto;
    }

    public static Boek toEntity(BoekDto dto) {
        if (dto == null) {
            return null;
        }

        Boek boek = new Boek();
        boek.setId(dto.getId());
        boek.setTitel(dto.getTitel());
        boek.setAuteur(dto.getAuteur());
        boek.setCover(dto.getCover());
        boek.setBeschrijving(dto.getBeschrijving());
        boek.setGenre(dto.getGenre());
        boek.setUitgaveDatum(dto.getUitgaveDatum());
        boek.setPaginas(dto.getPaginas());
        boek.setTaal(dto.getTaal());
        boek.setUitgeverij(dto.getUitgeverij());

        return boek;
    }
}

