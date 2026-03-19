package com.jden.espylah.webapi.db.models;

import com.jden.espylah.webapi.app.RunMode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.descriptor.jdbc.VarcharUUIDJdbcType;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Entity
@Table(name = "devices")
@Getter
@Setter
public class Device {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcType(VarcharUUIDJdbcType.class)
    @Column(name = "id")
    private UUID id;

    private String name;

    @Enumerated(EnumType.STRING)
    private RunMode runMode;

    @Column(name = "device_mac")
    private Long deviceMac;

    @Column(name = "device_mac_str",length = 32)
    private String deviceMacStr;

    @Enumerated(EnumType.STRING)
    @Column(name = "device_state")
    private State state;

    @Column(name = "device_enabled")
    private boolean enabled;

    @Column(name = "api_token", length = 96)
    private String apiToken;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "username")
    private User user;

    @OneToMany(fetch = FetchType.LAZY, mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<SpeciesTarget> speciesTargets = new HashSet<>();

    @CreationTimestamp
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant lastSeenAt;

    @Override
    public String toString() {
        return "Device{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", runMode=" + runMode +
                ", deviceMac=" + deviceMac +
                ", state=" + state +
                ", enabled=" + enabled +
                ", apiToken='[REDACTED]'" +
                ", user=" + user.getUsername() +
                ", speciesTargets=" + speciesTargets.stream().map(st -> st.getTargetSpecies().getSpecies() + ":" + st.getThresholdDetect()).collect(Collectors.joining(",")) +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }

    public enum State {
        UNPROVISIONED,
        PROVISIONED
    }

}
