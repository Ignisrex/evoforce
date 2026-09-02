package com.silverignis.environment;

import com.silverignis.entities.Battlefield;
import com.silverignis.particles.Anchor;
import com.silverignis.particles.Channel;
import com.silverignis.particles.Drive;
import com.silverignis.particles.EmitterHandle;
import com.silverignis.particles.Vfx;

/** ICE: the frozen-sheet frag plus frost fog pooling on each ice tile. */
public class IcePanelSurface extends ShaderPanelSurface {

    private final EmitterHandle[][] fog = new EmitterHandle[Battlefield.COLS][Battlefield.ROWS];

    public IcePanelSurface() {
        super(Battlefield.PanelType.ICE, "ice");
    }

    @Override public void tileGained(int col, int row) {
        fog[col][row] = Vfx.frostFog().play(host.particles(),
            Anchor.at(Battlefield.floorX(col), 0f, Battlefield.floorZ(row)),
            Drive.FULL, Channel.COMBAT);
    }

    @Override public void tileLost(int col, int row) {
        if (fog[col][row] != null) { fog[col][row].stop(); fog[col][row] = null; }
    }

    @Override public void dispose() {
        for (int c = 0; c < Battlefield.COLS; c++)
            for (int r = 0; r < Battlefield.ROWS; r++)
                tileLost(c, r);
        super.dispose();
    }
}
