package com.jden.espylah.webapi.db.models;

import jakarta.persistence.Embeddable;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.VarcharUUIDJdbcType;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NotNull
@Embeddable
@NoArgsConstructor
public class SpeciesTargetId implements Serializable {

    private String species;

    private UUID device;
}
