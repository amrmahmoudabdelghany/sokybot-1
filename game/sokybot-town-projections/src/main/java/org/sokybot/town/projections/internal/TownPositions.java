package org.sokybot.town.projections.internal;

import org.sokybot.commons.SilkroadUtils;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.gameevents.dto.PlayerData;
import org.sokybot.gameevents.dto.SpawnData;
import org.sokybot.gameevents.events.character.CharacterLoadedEvent;

final class TownPositions {

    private TownPositions() {
    }

    static float[] xyz(GamePosition p) {
        if (p == null) {
            return null;
        }
        return new float[] { p.getX(), p.getY(), p.getZ() };
    }

    static float[] xyzFromSpawn(SpawnData sd) {
        if (sd == null) {
            return null;
        }
        GamePosition pos = sd.getPosition();
        if (pos != null) {
            return xyz(pos);
        }
        float x = SilkroadUtils.getXCoord(sd.getXOffset(), (short) sd.getXSector());
        float y = SilkroadUtils.getYCoord(sd.getYOffset(), (short) sd.getYSector());
        float z = sd.getZOffset();
        return new float[] { x, y, z };
    }

    static float[] xyzFromCharacterLoaded(CharacterLoadedEvent e) {
        float x = SilkroadUtils.getXCoord(e.getXOffset(), (short) e.getXSector());
        float y = SilkroadUtils.getYCoord(e.getYOffset(), (short) e.getYSector());
        return new float[] { x, y, e.getZOffset() };
    }

    static float[] xyzFromPlayer(PlayerData p) {
        if (p == null) {
            return null;
        }
        return xyzFromSpawn(p);
    }
}
