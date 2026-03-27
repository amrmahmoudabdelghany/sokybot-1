import type { MachineInfo } from '../RSocketClient';

export function normalizeMachines(input: MachineInfo[]): MachineInfo[] {
    const deduped = new Map<string, MachineInfo>();
    input.forEach((machine) => {
        if (!machine?.machineId) return;
        const prev = deduped.get(machine.machineId);
        deduped.set(machine.machineId, {
            ...(prev || {}),
            ...machine,
            groupName: machine.groupName || prev?.groupName,
        });
    });
    return Array.from(deduped.values());
}
