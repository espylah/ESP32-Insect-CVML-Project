package com.jden.espylah.webapi.db.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.time.Instant;

@Entity
@Getter
@Setter
@ToString
@Table(name = "device_registration_tokens")
public class DeviceRegistrationToken {

    @Id
    private String token;

    private Instant expiresAt;

    private Boolean used;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "username", foreignKey = @ForeignKey(name = "fk_drt_to_user"))
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    private Device device;

}
