package com.jden.espylah.webapi.db.models;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.JdbcType;
import org.hibernate.type.descriptor.jdbc.BooleanJdbcType;

import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
@ToString
@Table(name = "users")
public class User {

    @Id
    @Column(name = "username", unique = true, nullable = false)
    private String username;

    @Column(name = "password", length = 128, nullable = false)
    private String password;

    @JdbcType(BooleanJdbcType.class)
    @Column(name = "enabled", nullable = false)
    private boolean enabled;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "authorities"
            , joinColumns = {
            @JoinColumn(name = "username", foreignKey = @ForeignKey(name = "fk_authority_to_user"))
    })
    @Column(name = "authority", nullable = false)
    private Set<String> userAuthorities = new HashSet<>();
}
