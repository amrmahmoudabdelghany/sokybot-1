import { useCallback, useRef, useState } from 'react';

type OnboardingFormValues = {
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

const DEFAULT_ONBOARDING_FORM: OnboardingFormValues = {
    targetGateway: '',
    username: '',
    password: '',
    passcode: '',
    targetAgent: '',
    selectedCharacter: '',
    selectedCharacterSlot: -1,
    characterSlotBase: 0,
    characterSelectionStrictMode: false,
    agentWaitTimeoutMs: 15000,
    loginResponseTimeoutMs: 15000,
    agentAuthTimeoutMs: 30000,
    passcodeWaitTimeoutMs: 60000,
    passcodeUserInputTimeoutMs: 60000,
};

export function useOnboardingFormState(machineId: string) {
    const [onboardingFormByMachine, setOnboardingFormByMachine] = useState<Record<string, OnboardingFormValues>>({});
    const [abortInFlightByMachine, setAbortInFlightByMachine] = useState<Record<string, boolean>>({});
    const abortInFlightRef = useRef<Record<string, boolean>>({});
    const connectOperationGenRef = useRef(0);

    const parseMachineParts = useCallback((id: string) => {
        const parts = id.split('.', 2);
        if (parts.length < 2) return null;
        return { group: parts[0], name: parts[1] };
    }, []);

    const currentOnboardingForm = onboardingFormByMachine[machineId] || DEFAULT_ONBOARDING_FORM;

    return {
        onboardingFormByMachine,
        setOnboardingFormByMachine,
        abortInFlightByMachine,
        setAbortInFlightByMachine,
        abortInFlightRef,
        connectOperationGenRef,
        parseMachineParts,
        currentOnboardingForm,
    };
}

