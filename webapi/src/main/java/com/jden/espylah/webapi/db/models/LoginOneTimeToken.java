package com.jden.espylah.webapi.db.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Getter
@Setter
@Table(name = "login_one_time_tokens")
public class LoginOneTimeToken {

    @Id
    private String token;

    private Instant expiresAt;

    private Boolean used;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "username", foreignKey = @ForeignKey(name = "fk_lott_to_user"))
    private User user;

}