package com.jden.espylah.webapi.db.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "species_targets")
public class SpeciesTarget {

    @EmbeddedId
    private SpeciesTargetId id = new SpeciesTargetId();

    @Column(name = "detection_threshold")
    private Double thresholdDetect;

    @MapsId("device")
    @JoinColumn(name = "device", foreignKey = @ForeignKey(name = "fk_st_to_device"))
    @ManyToOne(fetch = FetchType.LAZY)
    private Device device;

    @MapsId("species")
    @JoinColumn(name = "species", foreignKey = @ForeignKey(name = "fk_st_to_specie"))
    @ManyToOne(fetch = FetchType.LAZY)
    private Species targetSpecies;

}
