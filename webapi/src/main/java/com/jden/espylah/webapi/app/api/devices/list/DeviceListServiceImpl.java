package com.jden.espylah.webapi.app.api.devices.list;

import com.jden.espylah.webapi.app.utils.TimeUtil;
import com.jden.espylah.webapi.db.repos.DeviceRepo;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;

@Log4j2
@Component
public class DeviceListServiceImpl implements DeviceListService {

    private final DeviceRepo deviceRepo;
    private final TimeUtil timeUtil;

    public DeviceListServiceImpl(DeviceRepo deviceRepo, TimeUtil timeUtil) {
        this.deviceRepo = deviceRepo;
        this.timeUtil = timeUtil;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<DeviceListItem> listDevices(Authentication authentication, DeviceFilter filter, Pageable pageable) {
        Specification<com.jden.espylah.webapi.db.models.Device> spec =
                Specification.where(DeviceSpec.forUser(authentication.getName()));

        if (filter.name() != null && !filter.name().isBlank()) {
            spec = spec.and(DeviceSpec.nameContains(filter.name()));
        }
        if (filter.state() != null) {
            spec = spec.and(DeviceSpec.hasState(filter.state()));
        }
        if (filter.runMode() != null) {
            spec = spec.and(DeviceSpec.hasRunMode(filter.runMode()));
        }
        if (filter.enabled() != null) {
            spec = spec.and(DeviceSpec.isEnabled(filter.enabled()));
        }
        if (Boolean.TRUE.equals(filter.online())) {
            spec = spec.and(DeviceSpec.seenSince(timeUtil.now().minus(Duration.ofHours(7))));
        }

        log.debug("{} querying devices with filter: {}", authentication.getName(), filter);

        return deviceRepo.findAll(spec, pageable)
                .map(d -> new DeviceListItem(d.getId(), d.getName(), d.getRunMode(), d.getState(), d.isEnabled(), d.getCreatedAt(), d.getLastSeenAt()));
    }
}
