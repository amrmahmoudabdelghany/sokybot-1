package org.sokybot.webview.handler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;
import org.sokybot.webview.api.dto.MachineInfoDto;

import reactor.core.publisher.Mono;

/**
 * Single-call workspace bootstrap: groups plus machines (does not replace {@code group.list} / {@code machine.list}).
 */
@Component(service = IRSocketHandler.class, property = IRSocketHandler.METHOD_PROPERTY + "=workspace.summary")
public class WorkspaceSummaryHandler implements IRSocketHandler {

    private volatile ISokybotContext sokybotContext;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL)
    protected void setSokybotContext(ISokybotContext sokybotContext) {
        this.sokybotContext = sokybotContext;
    }

    protected void unsetSokybotContext(ISokybotContext ctx) {
        this.sokybotContext = null;
    }

    @Override
    public String[] getMethods() {
        return new String[] { "workspace.summary" };
    }

    @Override
    public String getDescription() {
        return "Combined groups and machines for UI bootstrap";
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        ISokybotContext ctx = this.sokybotContext;
        if (ctx == null) {
            return Mono.just(RSocketResponse.success(Map.of("groups", List.of(), "machines", List.of())));
        }

        List<Map<String, Object>> groups = new ArrayList<>();
        List<MachineInfoDto> machines = new ArrayList<>();
        Map<String, MachineInfoDto> machineDedup = new HashMap<>();

        for (IGroupContext g : ctx.getGroups()) {
            Map<String, Object> gmap = new HashMap<>();
            gmap.put("name", g.name());
            IMachineContext[] ms = g.getMachines();
            gmap.put("machineCount", ms.length);
            groups.add(gmap);
            for (IMachineContext m : ms) {
                MachineInfoDto dto = new MachineInfoDto(m.fullName(), m.getMachineName(), g.name(), m.isRunning());
                machineDedup.put(dto.getMachineId(), dto);
            }
        }
        machines.addAll(machineDedup.values());

        Map<String, Object> body = new HashMap<>();
        body.put("groups", groups);
        body.put("machines", machines);
        return Mono.just(RSocketResponse.success(body));
    }
}
