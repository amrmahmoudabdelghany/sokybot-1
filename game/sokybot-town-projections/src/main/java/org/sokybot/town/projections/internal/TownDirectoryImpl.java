package org.sokybot.town.projections.internal;

import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.sokybot.town.api.NpcRef;
import org.sokybot.town.api.NpcRole;
import org.sokybot.town.projections.api.ITownDirectory;

/**
 * Placeholder directory until PK2/JSON world data is wired; returns static stand-ins.
 */
@Component(service = ITownDirectory.class)
public final class TownDirectoryImpl implements ITownDirectory {

    @Override
    public Optional<NpcRef> nearestVendor(String machineFullName) {
        NpcRef v = NpcRef.builder()
                .catalogueNpcRefId(1)
                .displayName("placeholder-vendor")
                .role(NpcRole.POTIONS)
                .regionId(0)
                .build();
        return Optional.of(v);
    }

    @Override
    public Optional<NpcRef> nearestTown(String machineFullName) {
        NpcRef t = NpcRef.builder()
                .catalogueNpcRefId(2)
                .displayName("placeholder-town")
                .role(NpcRole.TELEPORT)
                .regionId(0)
                .build();
        return Optional.of(t);
    }
}
