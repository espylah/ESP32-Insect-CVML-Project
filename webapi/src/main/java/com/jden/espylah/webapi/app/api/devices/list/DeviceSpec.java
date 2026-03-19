package com.jden.espylah.webapi.app.api.devices.list;

import com.jden.espylah.webapi.app.RunMode;
import com.jden.espylah.webapi.db.models.Device;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public class DeviceSpec {

    private DeviceSpec() {
    }

    public static Specification<Device> forUser(String username) {
        return (root, query, cb) -> cb.equal(root.get("user").get("username"), username);
    }

    public static Specification<Device> nameContains(String name) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<Device> hasState(Device.State state) {
        return (root, query, cb) -> cb.equal(root.get("state"), state);
    }

    public static Specification<Device> hasRunMode(RunMode runMode) {
        return (root, query, cb) -> cb.equal(root.get("runMode"), runMode);
    }

    public static Specification<Device> isEnabled(boolean enabled) {
        return (root, query, cb) -> cb.equal(root.get("enabled"), enabled);
    }

    public static Specification<Device> seenSince(Instant since) {
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("lastSeenAt"), since);
    }
}
