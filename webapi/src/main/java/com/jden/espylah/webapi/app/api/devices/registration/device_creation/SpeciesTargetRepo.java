package com.jden.espylah.webapi.app.api.devices.registration.device_creation;

import com.jden.espylah.webapi.db.models.SpeciesTarget;
import com.jden.espylah.webapi.db.models.SpeciesTargetId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesTargetRepo extends JpaRepository<SpeciesTarget, SpeciesTargetId> {
}
