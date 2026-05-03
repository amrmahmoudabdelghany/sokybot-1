package org.sokybot.behaviors.combat.internal.shapeshifter;

import java.util.List;
import java.util.Optional;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.sokybot.behaviors.combat.internal.CombatPackets;
import org.sokybot.combat.api.shapeshifter.IWeaponFlexService;
import org.sokybot.engine.IEngine;
import org.sokybot.engine.api.workflow.IWorkflowContext;
import org.sokybot.runtime.IGroupContext;
import org.sokybot.runtime.IMachineContext;
import org.sokybot.runtime.ISokybotContext;
import org.sokybot.town.api.IInventorySnapshot;
import org.sokybot.town.api.ITownSnapshot;
import org.sokybot.town.api.ItemStackSnapshot;
import org.sokybot.town.projections.api.ITownModel;

/**
 * Epic #22: weapon equip by ref id using inventory move (slot 6 primary weapon; backpack slots {@code >= 13}).
 */
@Component(service = IWeaponFlexService.class, immediate = true)
public final class WeaponFlexServiceImpl implements IWeaponFlexService {

    private static final byte SLOT_WEAPON_MAIN = 6;

    /** Standard Silkroad backpack grid begins at slot 13 for picked-up items. */
    private static final int INVENTORY_BACKPACK_MIN_SLOT = 13;

    @Reference
    private ISokybotContext sokybotContext;

    @Reference
    private ITownModel townModel;

    @Override
    public int getCurrentWeaponRefId(String machineFullName) {
        if (machineFullName == null || machineFullName.trim().isEmpty()) {
            return 0;
        }
        ITownModel model = townModel;
        if (model == null) {
            return 0;
        }
        try {
            Optional<ITownSnapshot> snapOpt = model.snapshot(machineFullName.trim());
            if (!snapOpt.isPresent()) {
                return 0;
            }
            IInventorySnapshot inv = snapOpt.get().getInventory();
            if (inv == null) {
                return 0;
            }
            Optional<ItemStackSnapshot> eq = inv.findSlot(SLOT_WEAPON_MAIN);
            if (!eq.isPresent()) {
                return 0;
            }
            return eq.get().getItemRefId();
        } catch (RuntimeException ex) {
            return 0;
        }
    }

    @Override
    public boolean equipWeaponByRefId(String machineFullName, int targetWeaponRefId) {
        if (machineFullName == null || machineFullName.trim().isEmpty() || targetWeaponRefId <= 0) {
            return false;
        }
        IMachineContext machine = resolveMachine(machineFullName.trim());
        ITownModel model = townModel;
        if (machine == null || model == null) {
            return false;
        }
        try {
            Optional<ITownSnapshot> snapOpt = model.snapshot(machine.fullName());
            if (!snapOpt.isPresent()) {
                return false;
            }
            IInventorySnapshot inv = snapOpt.get().getInventory();
            if (inv == null) {
                return false;
            }
            ItemStackSnapshot stack = findBackpackStack(inv, targetWeaponRefId);
            if (stack == null) {
                return false;
            }
            IEngine engine = machine.getEngine();
            if (engine == null) {
                return false;
            }
            Optional<IWorkflowContext> wfOpt = engine.optionalWorkflowContext();
            if (!wfOpt.isPresent()) {
                return false;
            }
            int qty = stack.getQuantity();
            short q = qty > Short.MAX_VALUE ? Short.MAX_VALUE : (short) qty;
            CombatPackets.sendInventoryMoveSlot(
                    wfOpt.get(),
                    (byte) stack.getSlotIndex(),
                    SLOT_WEAPON_MAIN,
                    q);
            return true;
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static ItemStackSnapshot findBackpackStack(IInventorySnapshot inv, int targetWeaponRefId) {
        List<ItemStackSnapshot> stacks = inv.listStacks();
        if (stacks == null) {
            return null;
        }
        for (ItemStackSnapshot s : stacks) {
            if (s == null) {
                continue;
            }
            if (s.getSlotIndex() >= INVENTORY_BACKPACK_MIN_SLOT && s.getItemRefId() == targetWeaponRefId) {
                return s;
            }
        }
        return null;
    }

    private IMachineContext resolveMachine(String machineFullName) {
        ISokybotContext ctx = sokybotContext;
        if (ctx == null) {
            return null;
        }
        try {
            for (IGroupContext group : ctx.getGroups()) {
                if (group == null) {
                    continue;
                }
                for (IMachineContext m : group.getMachines()) {
                    if (m != null && machineFullName.equals(m.fullName())) {
                        return m;
                    }
                }
            }
        } catch (RuntimeException ex) {
            return null;
        }
        return null;
    }
}
