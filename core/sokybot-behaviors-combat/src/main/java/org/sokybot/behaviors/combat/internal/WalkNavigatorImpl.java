package org.sokybot.behaviors.combat.internal;

import java.util.Random;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.commons.SilkroadUtils;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.gameevents.dto.GamePosition;
import org.sokybot.navigation.api.INavigator;
import org.sokybot.navigation.api.IPathfinder;
import org.sokybot.navigation.api.NavigationException;
import org.sokybot.navigation.api.WorldPoint;

@Component(service = INavigator.class, immediate = true)
public final class WalkNavigatorImpl implements INavigator {

    private final Random random = new Random();

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    private volatile IPathfinder pathfinder;

    @Override
    public void walkTo(IWorkflowContext ctx, WorldPoint target) throws NavigationException {
        if (target == null) {
            throw new NavigationException("target required");
        }
        if (ctx == null || ctx.getGameModel() == null || ctx.getGameModel().getTrainer() == null) {
            throw new NavigationException("trainer unavailable");
        }
        GamePosition pos = ctx.getGameModel().getTrainer().getPosition();
        if (pos == null) {
            throw new NavigationException("trainer position unavailable");
        }
        WorldPoint origin = new WorldPoint(pos.getX(), pos.getY(), pos.getZ());
        IPathfinder pf = pathfinder;
        if (pf != null && !pf.isReachable(origin, target)) {
            throw new NavigationException("target unreachable");
        }
        float nx = target.getX();
        float ny = target.getY();
        float nz = target.getZ();
        short sectorX = SilkroadUtils.getSectorX(nx);
        byte sectorY = SilkroadUtils.getSectorY(ny);
        CombatPackets.sendCharMove(ctx, nx, ny, nz, sectorX & 0xFFFF, sectorY & 0xFF);
    }

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
