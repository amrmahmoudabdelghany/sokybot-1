import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
    Button,
    Select as SelectRoot,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    encodeSelectItemValue,
    decodeSelectItemValue,
} from '@sokybot/frontend-shared';

const connectSchema = z.object({
    targetGateway: z.string().min(1, 'Gateway is required'),
});

const agentSchema = z.object({
    targetAgent: z.string().min(1, 'Select an agent'),
});

const credentialsSchema = z.object({
    username: z.string().min(1, 'Username is required'),
    password: z.string(),
    passcode: z.string(),
});

const characterSchema = z.object({
    selectedCharacter: z.string().min(1, 'Select a character'),
});

export type OnboardingFormValues = {
    targetGateway: string;
    username: string;
    password: string;
    passcode: string;
    targetAgent: string;
    selectedCharacter: string;
};

type MachineStatus = {
    loginPhase: string;
    connected: boolean;
    authenticated: boolean;
    inGame: boolean;
    agentOptions: Array<{ value: string; label: string }>;
    availableCharacters: string[];
    selectedCharacter: string | null;
};

interface MachineOnboardingPanelProps {
    selectedMachineId: string | null;
    currentMachineStatus: MachineStatus;
    showConnectCard: boolean;
    showAgentServerCard: boolean;
    currentOnboardingForm: OnboardingFormValues;
    connectInFlightByMachine: Record<string, boolean>;
    saveLoginPayload: (
        machineId: string,
        payload: Record<string, unknown>,
        startAfterSave?: boolean
    ) => Promise<void>;
}

export const MachineOnboardingPanel: React.FC<MachineOnboardingPanelProps> = ({
    selectedMachineId,
    currentMachineStatus,
    showConnectCard,
    showAgentServerCard,
    currentOnboardingForm,
    connectInFlightByMachine,
    saveLoginPayload,
}) => {
    const connectForm = useForm<z.infer<typeof connectSchema>>({
        resolver: zodResolver(connectSchema),
        defaultValues: { targetGateway: currentOnboardingForm.targetGateway },
    });

    const agentForm = useForm<z.infer<typeof agentSchema>>({
        resolver: zodResolver(agentSchema),
        defaultValues: { targetAgent: currentOnboardingForm.targetAgent },
    });

    const credentialsForm = useForm<z.infer<typeof credentialsSchema>>({
        resolver: zodResolver(credentialsSchema),
        defaultValues: {
            username: currentOnboardingForm.username,
            password: currentOnboardingForm.password,
            passcode: currentOnboardingForm.passcode,
        },
    });

    const characterForm = useForm<z.infer<typeof characterSchema>>({
        resolver: zodResolver(characterSchema),
        defaultValues: { selectedCharacter: currentOnboardingForm.selectedCharacter },
    });

    useEffect(() => {
        connectForm.reset({ targetGateway: currentOnboardingForm.targetGateway });
    }, [selectedMachineId, currentOnboardingForm.targetGateway, connectForm]);

    useEffect(() => {
        agentForm.reset({ targetAgent: currentOnboardingForm.targetAgent });
    }, [selectedMachineId, currentOnboardingForm.targetAgent, agentForm]);

    useEffect(() => {
        credentialsForm.reset({
            username: currentOnboardingForm.username,
            password: currentOnboardingForm.password,
            passcode: currentOnboardingForm.passcode,
        });
    }, [
        selectedMachineId,
        currentOnboardingForm.username,
        currentOnboardingForm.password,
        currentOnboardingForm.passcode,
        credentialsForm,
    ]);

    useEffect(() => {
        characterForm.reset({ selectedCharacter: currentOnboardingForm.selectedCharacter });
    }, [selectedMachineId, currentOnboardingForm.selectedCharacter, characterForm]);

    if (!selectedMachineId) {
        return null;
    }

    return (
        <div className="space-y-4">
            {showConnectCard && (
                <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3">
                    <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Connect</div>
                    <input
                        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                        placeholder="Target Gateway (IP or host)"
                        {...connectForm.register('targetGateway')}
                    />
                    {connectForm.formState.errors.targetGateway && (
                        <p className="text-[11px] text-destructive">
                            {connectForm.formState.errors.targetGateway.message}
                        </p>
                    )}
                    <Button
                        className="w-full"
                        disabled={Boolean(connectInFlightByMachine[selectedMachineId])}
                        onClick={connectForm.handleSubmit(async ({ targetGateway }) => {
                            await saveLoginPayload(
                                selectedMachineId,
                                { targetGateway: targetGateway.trim(), autoLogin: true },
                                true
                            );
                        })}
                    >
                        {connectInFlightByMachine[selectedMachineId] ? 'Connecting...' : 'Connect'}
                    </Button>
                </div>
            )}

            {showAgentServerCard && (
                <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3">
                    <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Agent Server</div>
                    {currentMachineStatus.loginPhase === 'WAITING_FOR_AGENTS'
                        && currentMachineStatus.agentOptions.length === 0 && (
                        <p className="text-[11px] text-muted-foreground leading-snug">
                            Waiting for the gateway to return the agent list…
                        </p>
                    )}
                    {currentMachineStatus.loginPhase === 'WAITING_FOR_AGENTS_TIMEOUT' && (
                        <p className="text-[11px] text-amber-600/90 dark:text-amber-400/90 leading-snug">
                            Agent list timed out. Pick an agent if the list appears, or set a manual agent on the
                            Connection page and retry.
                        </p>
                    )}
                    <SelectRoot
                        value={encodeSelectItemValue(agentForm.watch('targetAgent') ?? '')}
                        onValueChange={(v) =>
                            agentForm.setValue('targetAgent', decodeSelectItemValue(v), {
                                shouldValidate: true,
                                shouldTouch: true,
                            })
                        }
                    >
                        <SelectTrigger className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm">
                            <SelectValue
                                placeholder={
                                    currentMachineStatus.agentOptions.length === 0
                                        ? 'Select discovered agent'
                                        : 'Select agent server'
                                }
                            />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value={encodeSelectItemValue('')}>
                                {currentMachineStatus.agentOptions.length === 0
                                    ? 'Select discovered agent'
                                    : 'Select agent server'}
                            </SelectItem>
                            {currentMachineStatus.agentOptions.map((option) => (
                                <SelectItem key={option.value} value={encodeSelectItemValue(option.value)}>
                                    {option.label}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </SelectRoot>
                    {agentForm.formState.errors.targetAgent && (
                        <p className="text-[11px] text-destructive">
                            {agentForm.formState.errors.targetAgent.message}
                        </p>
                    )}
                    <Button
                        className="w-full"
                        disabled={
                            !agentForm.watch('targetAgent')
                            || (currentMachineStatus.loginPhase === 'WAITING_FOR_AGENTS'
                                && currentMachineStatus.agentOptions.length === 0)
                        }
                        onClick={agentForm.handleSubmit(async ({ targetAgent }) => {
                            await saveLoginPayload(selectedMachineId, { targetAgent }, false);
                        })}
                    >
                        Save Agent Server
                    </Button>
                </div>
            )}

            {currentMachineStatus.loginPhase === 'MISSING_CREDENTIALS' && (
                <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3">
                    <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Authentication</div>
                    <input
                        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                        placeholder="Username"
                        {...credentialsForm.register('username')}
                    />
                    <input
                        type="password"
                        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                        placeholder="Password"
                        {...credentialsForm.register('password')}
                    />
                    <input
                        type="password"
                        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                        placeholder="Passcode"
                        {...credentialsForm.register('passcode')}
                    />
                    {(credentialsForm.formState.errors.username
                        || credentialsForm.formState.errors.password
                        || credentialsForm.formState.errors.passcode) && (
                        <p className="text-[11px] text-destructive">
                            {credentialsForm.formState.errors.username?.message
                                || credentialsForm.formState.errors.password?.message
                                || credentialsForm.formState.errors.passcode?.message}
                        </p>
                    )}
                    <Button
                        className="w-full"
                        onClick={credentialsForm.handleSubmit(async (vals) => {
                            await saveLoginPayload(selectedMachineId, {
                                username: vals.username,
                                password: vals.password,
                                passcode: vals.passcode,
                                autoLogin: true,
                            }, false);
                        })}
                    >
                        Save Credentials
                    </Button>
                </div>
            )}

            {currentMachineStatus.loginPhase === 'MISSING_CHARACTER_SELECTION' && (
                <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3">
                    <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Character List</div>
                    <SelectRoot
                        value={encodeSelectItemValue(characterForm.watch('selectedCharacter') ?? '')}
                        onValueChange={(v) =>
                            characterForm.setValue('selectedCharacter', decodeSelectItemValue(v), {
                                shouldValidate: true,
                                shouldTouch: true,
                            })
                        }
                    >
                        <SelectTrigger className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm">
                            <SelectValue placeholder="Select character" />
                        </SelectTrigger>
                        <SelectContent>
                            <SelectItem value={encodeSelectItemValue('')}>Select character</SelectItem>
                            {currentMachineStatus.availableCharacters.map((name) => (
                                <SelectItem key={name} value={encodeSelectItemValue(name)}>
                                    {name}
                                </SelectItem>
                            ))}
                        </SelectContent>
                    </SelectRoot>
                    {characterForm.formState.errors.selectedCharacter && (
                        <p className="text-[11px] text-destructive">
                            {characterForm.formState.errors.selectedCharacter.message}
                        </p>
                    )}
                    <Button
                        className="w-full"
                        onClick={characterForm.handleSubmit(async ({ selectedCharacter }) => {
                            await saveLoginPayload(
                                selectedMachineId,
                                { selectedCharacter },
                                false
                            );
                        })}
                    >
                        Save Character
                    </Button>
                </div>
            )}
        </div>
    );
};
