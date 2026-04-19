package org.sokybot.behaviors.combat.internal;

import java.util.Random;

import org.osgi.service.component.annotations.Component;
import org.sokybot.commons.SilkroadUtils;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.NavigationException;

@Component(service = INavigator.class, immediate = true)
public final class WalkNavigatorImpl implements INavigator {

    private final Random random = new Random();

    @Override
    public void randomShortWalk(IWorkflowContext ctx, float maxRadiusWorldUnits) throws NavigationException {
        if (ctx == null || ctx.getGameModel() == null || ctx.getGameModel().getTrainer() == null) {
            throw new NavigationException("trainer unavailable");
        }
        GamePosition pos = ctx.getGameModel().getTrainer().getPosition();
        if (pos == null) {
            throw new NavigationException("trainer position unavailable");
        }
        float cx = pos.getX();
        float cy = pos.getY();
        float cz = pos.getZ();
        double angle = random.nextDouble() * Math.PI * 2;
        float cap = Math.max(1f, Math.min(maxRadiusWorldUnits, 48f));
        float dist = 5f + random.nextFloat() * Math.min(cap, 20f);
        float nx = cx + (float) (Math.cos(angle) * dist);
        float ny = cy + (float) (Math.sin(angle) * dist);
        float nz = cz;
        short sectorX = SilkroadUtils.getSectorX(nx);
        byte sectorY = SilkroadUtils.getSectorY(ny);
        CombatPackets.sendCharMove(ctx, nx, ny, nz, sectorX & 0xFFFF, sectorY & 0xFF);
    }
}
