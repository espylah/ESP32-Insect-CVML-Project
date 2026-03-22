package com.jden.espylah.webapi.db.repos;

import com.jden.espylah.webapi.db.models.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface DeviceRepo extends JpaRepository<Device, UUID>, JpaSpecificationExecutor<Device> {

    @Query("""
            select d from Device d
                left join fetch d.speciesTargets t
                    where d.id = :uuid
            """)
    Optional<Device> findByIdAndFetchTree(UUID uuid);
}
