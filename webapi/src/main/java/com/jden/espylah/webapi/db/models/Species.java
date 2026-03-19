package com.jden.espylah.webapi.db.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "species")
@Getter
@Setter
@ToString
public class Species {

    @Id
    private String species;

    @Column(name = "description")
    private String description;
}
