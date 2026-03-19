package com.jden.espylah.webapi.app.api.species;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class SpeciesController {

    private final SpeciesService speciesService;

    public SpeciesController(SpeciesService speciesService) {
        this.speciesService = speciesService;
    }

    @GetMapping("/api/species")
    public List<SpeciesDto> listSpecies() {
        return speciesService.listAll();
    }
}
