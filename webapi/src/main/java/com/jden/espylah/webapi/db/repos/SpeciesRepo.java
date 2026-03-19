package com.jden.espylah.webapi.db.repos;

import com.jden.espylah.webapi.db.models.Species;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesRepo extends JpaRepository<Species, String> {
}
