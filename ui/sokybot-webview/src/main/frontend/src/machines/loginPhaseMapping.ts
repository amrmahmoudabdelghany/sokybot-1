/**
 * Centralized mapping from raw backend loginPhase strings to a UX-friendly model.
 *
 * This module is the single source of truth for how each engine phase is
 * presented in the onboarding UI.  Both MachineOnboardingSection (header) and
 * MachineOnboardingPanel (stepper / CTAs) consume this.
 *
 * ## Phase → Step Matrix
 *
 * | progressStep | Label              | Phases mapped here                                                              |
 * |--------------|--------------------|---------------------------------------------------------------------------------|
 * | 1            | Connect gateway    | DISCONNECTED, MISSING_GATEWAY, CONNECTING_GATEWAY                               |
 * | 2            | Discover servers   | WAITING_FOR_AGENTS, WAITING_FOR_AGENTS_TIMEOUT, MISSING_AGENT_SERVER            |
 * | 3            | Sign in            | MISSING_CREDENTIALS, LOGIN_SENT, FAILED, MANUAL_VERIFICATION_REQUIRED,          |
 * |              |                    | RETRY_DELAY, RETRY_DISABLED, RETRY_LIMIT_REACHED                               |
 * | 4            | Enter game         | AUTHENTICATED, MISSING_CHARACTER_SELECTION                                      |
 */

// ---------------------------------------------------------------------------
// Types
// ---------------------------------------------------------------------------

export type AuthState = 'not_started' | 'in_progress' | 'success' | 'failed';
export type Severity = 'neutral' | 'info' | 'warn' | 'error' | 'success';

export type ActionIntent =
    | 'open_gateway'
    | 'focus_credentials'
    | 'select_agent'
    | 'choose_character'
    | 'retry_now'
    | 'cancel'
    | 'check_settings'
    | 'acknowledge'
    | null;

export interface UXPhaseModel {
    /** User-facing title for this phase. */
    displayTitle: string;
    /** User-facing description / guidance text. */
    displayDescription: string;
    /** Aggregate authentication state for the auth badge. */
    authState: AuthState;
    /** 1-4 stepper position. */
    progressStep: 1 | 2 | 3 | 4;
    /** Visual severity for badges / borders. */
    severity: Severity;
    /** Primary CTA intent (highest priority). */
    primaryAction: ActionIntent;
    /** Secondary CTA intent. */
    secondaryAction: ActionIntent;
    /** Original raw phase string preserved for diagnostics. */
    rawPhase: string;
    /** Whether this phase represents a fatal (non-retryable) condition. */
    isFatal: boolean;
}

export type UXCategory = 'CONNECT' | 'AGENT' | 'AUTH' | 'CHARACTER' | 'INGAME' | 'ERROR';

export interface UXHints {
    loginPhase?: string | null;
    uxCategory?: UXCategory | null;
    requiresInput?: boolean | null;
    fatal?: boolean | null;
}

// ---------------------------------------------------------------------------
// Step labels (exported for the Stepper component)
// ---------------------------------------------------------------------------

export const STEP_LABELS = [
    'Connect gateway',
    'Discover server list',
    'Sign in',
    'Enter game',
] as const;

// ---------------------------------------------------------------------------
// Internal mapping table
// ---------------------------------------------------------------------------

type PhaseEntry = Omit<UXPhaseModel, 'rawPhase'>;

const PHASE_MAP: Record<string, PhaseEntry> = {
    DISCONNECTED: {
        displayTitle: 'Disconnected',
        displayDescription: 'Not connected to any gateway. Configure your gateway to begin.',
        authState: 'not_started',
        progressStep: 1,
        severity: 'neutral',
        primaryAction: 'open_gateway',
        secondaryAction: null,
        isFatal: false,
    },
    MISSING_GATEWAY: {
        displayTitle: 'Gateway required',
        displayDescription: 'Please enter a gateway address to connect.',
        authState: 'not_started',
        progressStep: 1,
        severity: 'warn',
        primaryAction: 'open_gateway',
        secondaryAction: null,
        isFatal: false,
    },
    CONNECTING_GATEWAY: {
        displayTitle: 'Connecting…',
        displayDescription: 'Establishing connection to the gateway server.',
        authState: 'not_started',
        progressStep: 1,
        severity: 'info',
        primaryAction: 'cancel',
        secondaryAction: null,
        isFatal: false,
    },
    WAITING_FOR_AGENTS: {
        displayTitle: 'Discovering servers…',
        displayDescription: 'Waiting for the gateway to return the server list.',
        authState: 'not_started',
        progressStep: 2,
        severity: 'info',
        primaryAction: 'cancel',
        secondaryAction: null,
        isFatal: false,
    },
    WAITING_FOR_AGENTS_TIMEOUT: {
        displayTitle: 'Server list timed out',
        displayDescription: 'The gateway did not return a server list in time. Select an agent if available, or retry.',
        authState: 'not_started',
        progressStep: 2,
        severity: 'warn',
        primaryAction: 'retry_now',
        secondaryAction: 'select_agent',
        isFatal: false,
    },
    MISSING_AGENT_SERVER: {
        displayTitle: 'Select target server',
        displayDescription: 'Connected to gateway. Please select a target game server (shard) to continue.',
        authState: 'not_started',
        progressStep: 2,
        severity: 'warn',
        primaryAction: 'select_agent',
        secondaryAction: null,
        isFatal: false,
    },
    MISSING_CREDENTIALS: {
        displayTitle: 'Ready to sign in',
        displayDescription: 'Ready to sign in. Please enter your account credentials to continue.',
        authState: 'not_started',
        progressStep: 3,
        severity: 'warn',
        primaryAction: 'focus_credentials',
        secondaryAction: null,
        isFatal: false,
    },
    LOGIN_SENT: {
        displayTitle: 'Signing in…',
        displayDescription: 'Login request sent. Waiting for server response.',
        authState: 'in_progress',
        progressStep: 3,
        severity: 'info',
        primaryAction: 'cancel',
        secondaryAction: null,
        isFatal: false,
    },
    AUTHENTICATED: {
        displayTitle: 'Authenticated',
        displayDescription: 'Login successful. Entering game world.',
        authState: 'success',
        progressStep: 4,
        severity: 'success',
        primaryAction: null,
        secondaryAction: null,
        isFatal: false,
    },
    LOADING_ENVIRONMENT: {
        displayTitle: 'Loading environment…',
        displayDescription: 'Synchronizing world entities before bot logic starts.',
        authState: 'in_progress',
        progressStep: 4,
        severity: 'info',
        primaryAction: 'cancel',
        secondaryAction: null,
        isFatal: false,
    },
    IN_GAME: {
        displayTitle: 'In game',
        displayDescription: 'World synchronization completed.',
        authState: 'success',
        progressStep: 4,
        severity: 'success',
        primaryAction: null,
        secondaryAction: null,
        isFatal: false,
    },
    WAITING_FOR_PASSCODE: {
        displayTitle: 'Passcode required',
        displayDescription: 'Additional verification is required. Enter passcode or complete challenge.',
        authState: 'in_progress',
        progressStep: 3,
        severity: 'warn',
        primaryAction: 'focus_credentials',
        secondaryAction: 'cancel',
        isFatal: false,
    },
    IN_QUEUE: {
        displayTitle: 'Waiting in queue…',
        displayDescription: 'Server queue in progress. Connection is kept alive.',
        authState: 'in_progress',
        progressStep: 3,
        severity: 'info',
        primaryAction: 'cancel',
        secondaryAction: null,
        isFatal: false,
    },
    SERVER_INSPECTION: {
        displayTitle: 'Server under inspection',
        displayDescription: 'Target server is under inspection. Rechecking automatically.',
        authState: 'in_progress',
        progressStep: 2,
        severity: 'warn',
        primaryAction: 'cancel',
        secondaryAction: 'select_agent',
        isFatal: false,
    },
    MISSING_CHARACTER_SELECTION: {
        displayTitle: 'Select a character',
        displayDescription: 'Choose a character from the list to enter the game.',
        authState: 'success',
        progressStep: 4,
        severity: 'warn',
        primaryAction: 'choose_character',
        secondaryAction: null,
        isFatal: false,
    },
    FAILED: {
        displayTitle: 'Login failed',
        displayDescription: 'Authentication was rejected. Check your credentials and try again.',
        authState: 'failed',
        progressStep: 3,
        severity: 'error',
        primaryAction: 'focus_credentials',
        secondaryAction: 'check_settings',
        isFatal: true,
    },
    MANUAL_VERIFICATION_REQUIRED: {
        displayTitle: 'Verification required',
        displayDescription: 'The server requires manual verification (e.g. captcha). Complete the challenge to continue.',
        authState: 'failed',
        progressStep: 3,
        severity: 'error',
        primaryAction: 'acknowledge',
        secondaryAction: null,
        isFatal: true,
    },
    RETRY_DELAY: {
        displayTitle: 'Retrying…',
        displayDescription: 'Waiting before the next login attempt.',
        authState: 'in_progress',
        progressStep: 3,
        severity: 'info',
        primaryAction: 'retry_now',
        secondaryAction: 'cancel',
        isFatal: false,
    },
    RETRY_DISABLED: {
        displayTitle: 'Retry disabled',
        displayDescription: 'Automatic reconnection is turned off. Enable it in settings or retry manually.',
        authState: 'failed',
        progressStep: 3,
        severity: 'warn',
        primaryAction: 'check_settings',
        secondaryAction: 'retry_now',
        isFatal: true,
    },
    RETRY_LIMIT_REACHED: {
        displayTitle: 'Retry limit reached',
        displayDescription: 'Maximum retry attempts exhausted. Adjust settings or retry manually.',
        authState: 'failed',
        progressStep: 3,
        severity: 'error',
        primaryAction: 'check_settings',
        secondaryAction: 'retry_now',
        isFatal: true,
    },
};

// ---------------------------------------------------------------------------
// Fallback for unknown / future phases
// ---------------------------------------------------------------------------

function humanizePhase(raw: string): string {
    return raw
        .replace(/_/g, ' ')
        .toLowerCase()
        .replace(/\b\w/g, (c) => c.toUpperCase())
        .trim();
}

const UNKNOWN_ENTRY: PhaseEntry = {
    displayTitle: 'Unknown status',
    displayDescription: 'The current login status is unrecognised. Check settings if the issue persists.',
    authState: 'not_started',
    progressStep: 1,
    severity: 'warn',
    primaryAction: 'check_settings',
    secondaryAction: null,
    isFatal: false,
};

// ---------------------------------------------------------------------------
// Public API
// ---------------------------------------------------------------------------

/**
 * Map a raw backend loginPhase string to a fully-resolved UX model.
 *
 * Handles null / undefined / empty / unknown phases with safe fallbacks.
 * The returned `progressStep` is always clamped to 1-4.
 */
export function mapPhaseToUX(phase: string | null | undefined): UXPhaseModel {
    const rawPhase = (phase ?? '').trim() || 'UNKNOWN';
    const entry = PHASE_MAP[rawPhase];

    if (entry) {
        return { ...entry, rawPhase };
    }

    // Unknown / future phase – generate best-effort display from raw value
    return {
        ...UNKNOWN_ENTRY,
        displayTitle: humanizePhase(rawPhase),
        rawPhase,
    };
}

export function resolvePhaseToUX(input: UXHints): UXPhaseModel {
    const base = mapPhaseToUX(input.loginPhase);
    const category = input.uxCategory ?? null;
    const requiresInput = input.requiresInput === true;
    const fatal = input.fatal ?? base.isFatal;

    if (!category) {
        return { ...base, isFatal: fatal };
    }

    // Prefer backend hints for unseen phases or when explicit UX category is provided.
    if (base.rawPhase === 'UNKNOWN' || !PHASE_MAP[base.rawPhase]) {
        return modelFromCategory(base.rawPhase, category, requiresInput, fatal);
    }
    return { ...base, isFatal: fatal };
}

function modelFromCategory(rawPhase: string, category: UXCategory, requiresInput: boolean, fatal: boolean): UXPhaseModel {
    switch (category) {
        case 'CONNECT':
            return {
                displayTitle: humanizePhase(rawPhase),
                displayDescription: requiresInput ? 'Connection setup requires user input.' : 'Preparing connection to gateway.',
                authState: fatal ? 'failed' : 'not_started',
                progressStep: 1,
                severity: fatal ? 'error' : 'info',
                primaryAction: requiresInput ? 'open_gateway' : 'cancel',
                secondaryAction: null,
                rawPhase,
                isFatal: fatal,
            };
        case 'AGENT':
            return {
                displayTitle: humanizePhase(rawPhase),
                displayDescription: requiresInput ? 'Server selection requires user input.' : 'Resolving or validating target server.',
                authState: fatal ? 'failed' : 'in_progress',
                progressStep: 2,
                severity: fatal ? 'error' : 'warn',
                primaryAction: requiresInput ? 'select_agent' : 'cancel',
                secondaryAction: requiresInput ? null : 'retry_now',
                rawPhase,
                isFatal: fatal,
            };
        case 'CHARACTER':
            return {
                displayTitle: humanizePhase(rawPhase),
                displayDescription: 'Character selection is required to continue.',
                authState: 'success',
                progressStep: 4,
                severity: requiresInput ? 'warn' : 'info',
                primaryAction: 'choose_character',
                secondaryAction: null,
                rawPhase,
                isFatal: fatal,
            };
        case 'INGAME':
            return {
                displayTitle: humanizePhase(rawPhase),
                displayDescription: 'World synchronization in progress or completed.',
                authState: 'success',
                progressStep: 4,
                severity: 'success',
                primaryAction: null,
                secondaryAction: null,
                rawPhase,
                isFatal: fatal,
            };
        case 'ERROR':
            return {
                displayTitle: humanizePhase(rawPhase),
                displayDescription: requiresInput ? 'User intervention is required before continuing.' : 'An error occurred. Retry or review settings.',
                authState: 'failed',
                progressStep: 3,
                severity: 'error',
                primaryAction: requiresInput ? 'acknowledge' : 'check_settings',
                secondaryAction: 'retry_now',
                rawPhase,
                isFatal: true,
            };
        case 'AUTH':
        default:
            return {
                displayTitle: humanizePhase(rawPhase),
                displayDescription: requiresInput ? 'Authentication input/challenge required.' : 'Authentication in progress.',
                authState: fatal ? 'failed' : 'in_progress',
                progressStep: 3,
                severity: fatal ? 'error' : 'info',
                primaryAction: requiresInput ? 'focus_credentials' : 'cancel',
                secondaryAction: requiresInput ? 'cancel' : null,
                rawPhase,
                isFatal: fatal,
            };
    }
}

/**
 * Derive stepper step states from the current UX model and prerequisite data.
 *
 * Returns arrays of step indices (0-based) that are completed, current, or blocked.
 */
export function deriveStepStates(
    ux: UXPhaseModel,
    prereqs: {
        hasGateway: boolean;
        hasCredentials: boolean;
        hasAgent: boolean;
        hasCharacter: boolean;
    },
): {
    completedSteps: number[];
    currentStep: number;
    blockedSteps: number[];
} {
    const stepIndex = ux.progressStep - 1; // 0-based

    // Completed = all steps before the current one
    const completedSteps: number[] = [];
    for (let i = 0; i < stepIndex; i++) {
        completedSteps.push(i);
    }

    // Blocked steps based on missing prerequisites
    const blockedSteps: number[] = [];
    if (!prereqs.hasGateway) blockedSteps.push(0);
    if (!prereqs.hasAgent && stepIndex >= 2) blockedSteps.push(1);
    if (!prereqs.hasCredentials && stepIndex >= 3) blockedSteps.push(2);
    if (!prereqs.hasCharacter && stepIndex >= 3) blockedSteps.push(3);

    // Remove current step from completed if it's also blocked
    const filteredCompleted = completedSteps.filter((s) => !blockedSteps.includes(s));

    return {
        completedSteps: filteredCompleted,
        currentStep: stepIndex,
        blockedSteps,
    };
}
