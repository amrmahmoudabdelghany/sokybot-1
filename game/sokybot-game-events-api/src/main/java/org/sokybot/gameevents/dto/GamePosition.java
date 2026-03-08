package org.sokybot.gameevents.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GamePosition implements Serializable {

    private float x;
    private float z;
    private float y;

    // Optional flag if needed, usually 0
    private byte flag;

    // Helper accessors matching original Position entity for compatibility

    public byte getSectorY() {
        return (byte) Math.floor(y / 192 + 92);
    }

    public byte getSectorX() {
        return (byte) Math.floor(x / 192 + 135);
    }

    public int getXOffset() {
        int res = (int) (x % 192);
        if (res < 0) {
            res += 192;
        }
        return res;
    }

    public int getYOffset() {
        int res = (int) (y % 192);
        if (res < 0) {
            res += 192;
        }
        return res;
    }
}
