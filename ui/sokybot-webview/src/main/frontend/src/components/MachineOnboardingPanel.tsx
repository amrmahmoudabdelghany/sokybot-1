import React from 'react';
import { OnboardingStepper } from './OnboardingStepper';
import { OnboardingCTA } from './OnboardingCTA';
import { RetryCountdown } from './RetryCountdown';
import { GatewayConnectCard } from './GatewayConnectCard';
import { AgentServerCard } from './AgentServerCard';
import { CredentialsCard } from './CredentialsCard';
import { CharacterSelectionCard } from './CharacterSelectionCard';
import { deriveStepStates, resolvePhaseToUX } from '../machines/loginPhaseMapping';
import type { ActionIntent } from '../machines/loginPhaseMapping';
import type { RetryCountdownState } from '../machines/useRetryCountdown';

export type OnboardingFormValues = {
    targetGateway: string;
    username: string;
    password: string;
    passcode: string;
    targetAgent: string;
    selectedCharacter: string;
    selectedCharacterSlot?: number;
    characterSlotBase?: number;
    characterSelectionStrictMode?: boolean;
    agentWaitTimeoutMs?: number;
    loginResponseTimeoutMs?: number;
    agentAuthTimeoutMs?: number;
    passcodeWaitTimeoutMs?: number;
    passcodeUserInputTimeoutMs?: number;
};

type MachineStatus = {
    loginPhase: string;
    connected: boolean;
    authenticated: boolean;
    inGame: boolean;
    agentOptions: Array<{ value: string; label: string }>;
    availableCharacters: string[];
    selectedCharacter: string | null;
    reason?: string | null;
    failureClass?: string | null;
    fatal?: boolean;
    uxCategory?: 'CONNECT' | 'AGENT' | 'AUTH' | 'CHARACTER' | 'INGAME' | 'ERROR' | null;
    requiresInput?: boolean;
    loginDetailMessage?: string | null;
    gatewayResultCode?: number | null;
    agentAuthResultCode?: number | null;
    failureReason?: string | null;
    queuePosition?: number | null;
};

interface MachineOnboardingPanelProps {
    selectedMachineId: string | null;
    currentMachineStatus: MachineStatus;
    showConnectCard: boolean;
    showAgentServerCard: boolean;
    currentOnboardingForm: OnboardingFormValues;
    connectInFlightByMachine: Record<string, boolean>;
    abortInFlightByMachine: Record<string, boolean>;
    isOffline: boolean;
    saveLoginPayload: (
        machineId: string,
        payload: Record<string, unknown>,
        startAfterSave?: boolean
    ) => Promise<void>;
    /** Start/resume bot (machine.start) without mutating saved login payload. */
    startMachine?: (machineId: string) => Promise<void>;
    abortLogin: (machineId: string) => Promise<void>;
    /** Retry countdown state from useRetryCountdown. */
    retryCountdown?: RetryCountdownState;
}

export const MachineOnboardingPanel: React.FC<MachineOnboardingPanelProps> = ({
    selectedMachineId,
    currentMachineStatus,
    showConnectCard,
    showAgentServerCard,
    currentOnboardingForm,
    connectInFlightByMachine,
    abortInFlightByMachine,
    isOffline,
    saveLoginPayload,
    startMachine,
    abortLogin,
    retryCountdown,
}) => {
    if (!selectedMachineId) {
        return null;
    }

    return (
        <div className="space-y-4">
            {/* 4-step progress tracker */}
            {(() => {
                const ux = resolvePhaseToUX({
                    loginPhase: currentMachineStatus.loginPhase,
                    uxCategory: currentMachineStatus.uxCategory,
                    requiresInput: currentMachineStatus.requiresInput,
                    fatal: currentMachineStatus.fatal,
                });
                const stepStates = deriveStepStates(ux, {
                    hasGateway: !showConnectCard,
                    hasCredentials: currentMachineStatus.loginPhase !== 'MISSING_CREDENTIALS',
                    hasAgent: !showAgentServerCard,
                    hasCharacter: currentMachineStatus.loginPhase !== 'MISSING_CHARACTER_SELECTION',
                });
                return (
                    <>
                        <OnboardingStepper
                            currentStep={stepStates.currentStep}
                            completedSteps={stepStates.completedSteps}
                            blockedSteps={stepStates.blockedSteps}
                        />

                        {/* Phase description + failure info */}
                        <div className="text-[11px] text-muted-foreground leading-snug">
                            {ux.displayDescription}
                        </div>

                        {currentMachineStatus.loginPhase === 'IN_QUEUE'
                            && typeof currentMachineStatus.queuePosition === 'number' && (
                            <div className="text-[11px] text-muted-foreground">
                                Queue position:
                                {' '}
                                <span className="font-medium text-foreground tabular-nums">
                                    {currentMachineStatus.queuePosition}
                                </span>
                            </div>
                        )}

                        {/* Derived / stream failure line */}
                        {(currentMachineStatus.loginDetailMessage || currentMachineStatus.reason)
                            && (ux.severity === 'error'
                                || ux.severity === 'warn'
                                || currentMachineStatus.loginPhase === 'FAILED'
                                || currentMachineStatus.loginPhase === 'MANUAL_VERIFICATION_REQUIRED') && (
                            <div className="text-[11px] text-destructive/90 bg-destructive/5 border border-destructive/20 rounded-md px-3 py-2 leading-snug">
                                {currentMachineStatus.loginDetailMessage || currentMachineStatus.reason}
                            </div>
                        )}

                        {(currentMachineStatus.gatewayResultCode != null
                            || currentMachineStatus.agentAuthResultCode != null
                            || (currentMachineStatus.failureReason
                                && String(currentMachineStatus.failureReason).trim().length > 0)) && (
                            <details className="text-[10px] text-muted-foreground border border-border/60 rounded-md px-2 py-1.5">
                                <summary className="cursor-pointer select-none text-foreground/80">
                                    Raw login diagnostics
                                </summary>
                                <dl className="mt-1.5 space-y-0.5 font-mono">
                                    {currentMachineStatus.gatewayResultCode != null && (
                                        <>
                                            <dt className="inline text-muted-foreground">gateway</dt>
                                            <dd className="inline ml-1">{currentMachineStatus.gatewayResultCode}</dd>
                                        </>
                                    )}
                                    {currentMachineStatus.agentAuthResultCode != null && (
                                        <>
                                            <dt className="inline text-muted-foreground">agent</dt>
                                            <dd className="inline ml-1">{currentMachineStatus.agentAuthResultCode}</dd>
                                        </>
                                    )}
                                    {currentMachineStatus.failureReason
                                        && String(currentMachineStatus.failureReason).trim().length > 0 && (
                                        <>
                                            <dt className="block text-muted-foreground mt-1">failureReason</dt>
                                            <dd className="break-words">{currentMachineStatus.failureReason}</dd>
                                        </>
                                    )}
                                </dl>
                            </details>
                        )}

                        {/* Retry countdown */}
                        {retryCountdown && <RetryCountdown countdown={retryCountdown} />}
                    </>
                );
            })()}
            {showConnectCard && (
                <GatewayConnectCard
                    initialGateway={currentOnboardingForm.targetGateway}
                    inFlight={Boolean(connectInFlightByMachine[selectedMachineId])}
                    isOffline={isOffline}
                    onSubmit={async ({ targetGateway }) => {
                        await saveLoginPayload(
                            selectedMachineId,
                            {
                                targetGateway: targetGateway.trim(),
                                targetAgent: '',
                                selectedCharacter: '',
                                autoLogin: true,
                            },
                            true
                        );
                    }}
                />
            )}

            {showAgentServerCard && (
                <AgentServerCard
                    initialAgent={currentOnboardingForm.targetAgent}
                    loginPhase={currentMachineStatus.loginPhase}
                    agentOptions={currentMachineStatus.agentOptions}
                    isOffline={isOffline}
                    onSubmit={async ({ targetAgent }) => {
                        await saveLoginPayload(selectedMachineId, { targetAgent }, true);
                    }}
                />
            )}

            {currentMachineStatus.loginPhase === 'MISSING_CREDENTIALS' && (
                <CredentialsCard
                    initialValues={{
                        username: currentOnboardingForm.username,
                        password: currentOnboardingForm.password,
                        passcode: currentOnboardingForm.passcode,
                    }}
                    isOffline={isOffline}
                    onSubmit={async (vals) => {
                        await saveLoginPayload(selectedMachineId, {
                            username: vals.username,
                            password: vals.password,
                            passcode: vals.passcode,
                            autoLogin: true,
                        }, true);
                    }}
                />
            )}

            {currentMachineStatus.loginPhase === 'MISSING_CHARACTER_SELECTION' && (
                <CharacterSelectionCard
                    initialCharacter={currentOnboardingForm.selectedCharacter}
                    availableCharacters={currentMachineStatus.availableCharacters}
                    isOffline={isOffline}
                    onSubmit={async ({ selectedCharacter }) => {
                        await saveLoginPayload(selectedMachineId, { selectedCharacter }, false);
                    }}
                />
            )}

            {/* Contextual CTAs (shown when no specific form card is active) */}
            {!showConnectCard
                && !showAgentServerCard
                && currentMachineStatus.loginPhase !== 'MISSING_CREDENTIALS'
                && currentMachineStatus.loginPhase !== 'MISSING_CHARACTER_SELECTION'
                && (() => {
                    const ux = resolvePhaseToUX({
                        loginPhase: currentMachineStatus.loginPhase,
                        uxCategory: currentMachineStatus.uxCategory,
                        requiresInput: currentMachineStatus.requiresInput,
                        fatal: currentMachineStatus.fatal,
                    });
                    const handleAction = (intent: ActionIntent) => {
                        if (!selectedMachineId || !intent) return;
                        switch (intent) {
                            case 'connect_bot':
                                if (startMachine) {
                                    void startMachine(selectedMachineId);
                                }
                                break;
                            case 'retry_now':
                            case 'check_settings':
                                // Re-trigger login with current saved payload
                                void saveLoginPayload(selectedMachineId, { autoLogin: true }, true);
                                break;
                            case 'cancel':
                                void abortLogin(selectedMachineId);
                                break;
                            case 'acknowledge':
                                // Acknowledged – no further action
                                break;
                            default:
                                break;
                        }
                    };
                    return (
                        <OnboardingCTA
                            ux={ux}
                            onAction={handleAction}
                            inFlight={Boolean(connectInFlightByMachine[selectedMachineId ?? '']) || isOffline}
                            aborting={Boolean(abortInFlightByMachine[selectedMachineId ?? ''])}
                        />
                    );
                })()}
        </div>
    );
};
