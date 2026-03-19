package com.jden.espylah.webapi.db.repos;

import com.jden.espylah.webapi.db.models.Device;
import com.jden.espylah.webapi.db.models.DeviceRegistrationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DeviceRegistrationTokenRepo extends JpaRepository<DeviceRegistrationToken, String> {
    @Query("""
            select t 
            from DeviceRegistrationToken t
            join fetch t.device d
            where t.token = :token
            """)
    DeviceRegistrationToken findByTokenFetchDevice(String token);

    List<DeviceRegistrationToken> findByDevice(Device device);
}
