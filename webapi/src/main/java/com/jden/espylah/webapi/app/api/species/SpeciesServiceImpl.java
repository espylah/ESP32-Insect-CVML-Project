package com.jden.espylah.webapi.app.api.species;

import com.jden.espylah.webapi.db.repos.SpeciesRepo;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SpeciesServiceImpl implements SpeciesService {

    private final SpeciesRepo speciesRepo;

    public SpeciesServiceImpl(SpeciesRepo speciesRepo) {
        this.speciesRepo = speciesRepo;
    }

    @Override
    @Cacheable("species")
    public List<SpeciesDto> listAll() {
        return speciesRepo.findAll().stream()
                .map(s -> new SpeciesDto(s.getSpecies(), s.getDescription()))
                .toList();
    }
}
