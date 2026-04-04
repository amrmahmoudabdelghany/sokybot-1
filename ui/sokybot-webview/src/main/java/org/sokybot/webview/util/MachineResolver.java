package org.sokybot.webview.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;

/**
 * Resolves {@link IMachineContext} from UI / RSocket identifiers ({@code group.bot} or simple machine name).
 */
public final class MachineResolver {

    private MachineResolver() {
    }

    /**
     * Resolve a machine: try {@code group.machine} first (first dot only), then simple name across all groups,
     * then optional single-group fallback (legacy deployments).
     */
    public static Optional<IMachineContext> resolve(ISokybotContext sokybotContext, IGroupContext singleGroupContext,
            String machineId) {
        if (machineId == null || machineId.isEmpty()) {
            return Optional.empty();
        }
        if (sokybotContext != null) {
            int dotIndex = machineId.indexOf('.');
            if (dotIndex > 0) {
                String groupName = machineId.substring(0, dotIndex);
                String machineName = machineId.substring(dotIndex + 1);
                Optional<IMachineContext> byFull = sokybotContext.findGroupCtx(groupName)
                        .flatMap(g -> g.findMachineCtx(machineName));
                if (byFull.isPresent()) {
                    return byFull;
                }
            }
            for (IGroupContext group : sokybotContext.getGroups()) {
                Optional<IMachineContext> m = group.findMachineCtx(machineId);
                if (m.isPresent()) {
                    return m;
                }
            }
        }
        if (singleGroupContext != null) {
            return singleGroupContext.findMachineCtx(machineId);
        }
        return Optional.empty();
    }

    /** All machines matching a simple (non-qualified) name across groups. */
    public static List<IMachineContext> findAllBySimpleName(ISokybotContext ctx, String machineName) {
        List<IMachineContext> matches = new ArrayList<>();
        if (ctx == null || machineName == null || machineName.isEmpty()) {
            return matches;
        }
        for (IGroupContext group : ctx.getGroups()) {
            group.findMachineCtx(machineName).ifPresent(matches::add);
        }
        return matches;
    }
}
