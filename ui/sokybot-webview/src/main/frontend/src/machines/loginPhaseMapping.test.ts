import { describe, it, expect } from 'vitest';
import {
    mapPhaseToUX,
    deriveStepStates,
    resolvePhaseToUX,
} from './loginPhaseMapping';

describe('mapPhaseToUX', () => {
    // -----------------------------------------------------------------------
    // Known phases
    // -----------------------------------------------------------------------

    it.each([
        ['DISCONNECTED', 1, 'neutral', 'not_started', false],
        ['MISSING_GATEWAY', 1, 'warn', 'not_started', false],
        ['CONNECTING_GATEWAY', 1, 'info', 'not_started', false],
        ['WAITING_FOR_AGENTS', 2, 'info', 'not_started', false],
        ['WAITING_FOR_AGENTS_TIMEOUT', 2, 'warn', 'not_started', false],
        ['MISSING_AGENT_SERVER', 2, 'warn', 'not_started', false],
        ['SERVER_INSPECTION', 2, 'warn', 'in_progress', false],
        ['MISSING_CREDENTIALS', 3, 'warn', 'not_started', false],
        ['LOGIN_SENT', 3, 'info', 'in_progress', false],
        ['WAITING_FOR_PASSCODE', 3, 'warn', 'in_progress', false],
        ['IN_QUEUE', 3, 'info', 'in_progress', false],
        ['AUTHENTICATED', 4, 'success', 'success', false],
        ['LOADING_ENVIRONMENT', 4, 'info', 'in_progress', false],
        ['IN_GAME', 4, 'success', 'success', false],
        ['MISSING_CHARACTER_SELECTION', 4, 'warn', 'success', false],
        ['FAILED', 3, 'error', 'failed', true],
        ['MANUAL_VERIFICATION_REQUIRED', 3, 'error', 'failed', true],
        ['RETRY_DELAY', 3, 'info', 'in_progress', false],
        ['RETRY_DISABLED', 3, 'warn', 'failed', true],
        ['RETRY_LIMIT_REACHED', 3, 'error', 'failed', true],
    ] as const)(
        'maps %s → step=%d severity=%s auth=%s fatal=%s',
        (phase, step, severity, authState, isFatal) => {
            const ux = mapPhaseToUX(phase);
            expect(ux.progressStep).toBe(step);
            expect(ux.severity).toBe(severity);
            expect(ux.authState).toBe(authState);
            expect(ux.isFatal).toBe(isFatal);
            expect(ux.rawPhase).toBe(phase);
            expect(ux.displayTitle.length).toBeGreaterThan(0);
            expect(ux.displayDescription.length).toBeGreaterThan(0);
        },
    );

    // -----------------------------------------------------------------------
    // Fallback / unknown
    // -----------------------------------------------------------------------

    it('returns safe fallback for unknown phase', () => {
        const ux = mapPhaseToUX('SOME_FUTURE_PHASE');
        expect(ux.severity).toBe('warn');
        expect(ux.progressStep).toBeGreaterThanOrEqual(1);
        expect(ux.progressStep).toBeLessThanOrEqual(4);
        expect(ux.rawPhase).toBe('SOME_FUTURE_PHASE');
        expect(ux.primaryAction).toBe('check_settings');
    });

    it('humanizes unknown phase for displayTitle', () => {
        const ux = mapPhaseToUX('WAITING_FOR_AGENTS_V2');
        expect(ux.displayTitle).toMatch(/waiting/i);
    });

    it('treats null as UNKNOWN', () => {
        const ux = mapPhaseToUX(null);
        expect(ux.rawPhase).toBe('UNKNOWN');
        expect(ux.severity).toBe('warn');
    });

    it('treats undefined as UNKNOWN', () => {
        const ux = mapPhaseToUX(undefined);
        expect(ux.rawPhase).toBe('UNKNOWN');
    });

    it('treats empty string as UNKNOWN', () => {
        const ux = mapPhaseToUX('');
        expect(ux.rawPhase).toBe('UNKNOWN');
    });

    it('treats whitespace-only string as UNKNOWN', () => {
        const ux = mapPhaseToUX('   ');
        expect(ux.rawPhase).toBe('UNKNOWN');
    });

    // -----------------------------------------------------------------------
    // progressStep clamping (compilation safety – all known phases already in 1-4)
    // -----------------------------------------------------------------------

    it('all known phases have progressStep in 1-4', () => {
        const phases = [
            'DISCONNECTED', 'MISSING_GATEWAY', 'CONNECTING_GATEWAY',
            'WAITING_FOR_AGENTS', 'WAITING_FOR_AGENTS_TIMEOUT', 'MISSING_AGENT_SERVER',
            'SERVER_INSPECTION',
            'MISSING_CREDENTIALS', 'LOGIN_SENT', 'WAITING_FOR_PASSCODE', 'IN_QUEUE',
            'AUTHENTICATED', 'LOADING_ENVIRONMENT', 'IN_GAME', 'MISSING_CHARACTER_SELECTION',
            'FAILED', 'MANUAL_VERIFICATION_REQUIRED', 'RETRY_DELAY', 'RETRY_DISABLED', 'RETRY_LIMIT_REACHED',
        ];
        for (const p of phases) {
            const ux = mapPhaseToUX(p);
            expect(ux.progressStep).toBeGreaterThanOrEqual(1);
            expect(ux.progressStep).toBeLessThanOrEqual(4);
        }
    });

    // -----------------------------------------------------------------------
    // Action intents
    // -----------------------------------------------------------------------

    it('MISSING_GATEWAY primaryAction is open_gateway', () => {
        expect(mapPhaseToUX('MISSING_GATEWAY').primaryAction).toBe('open_gateway');
    });

    it('MISSING_CREDENTIALS primaryAction is focus_credentials', () => {
        expect(mapPhaseToUX('MISSING_CREDENTIALS').primaryAction).toBe('focus_credentials');
    });
    it('MISSING_CREDENTIALS uses ready-to-sign-in copy', () => {
        const ux = mapPhaseToUX('MISSING_CREDENTIALS');
        expect(ux.displayTitle).toMatch(/ready to sign in/i);
    });

    it('MISSING_AGENT_SERVER primaryAction is select_agent', () => {
        expect(mapPhaseToUX('MISSING_AGENT_SERVER').primaryAction).toBe('select_agent');
    });
    it('MISSING_AGENT_SERVER uses connected-gateway copy', () => {
        const ux = mapPhaseToUX('MISSING_AGENT_SERVER');
        expect(ux.displayDescription).toMatch(/connected to gateway/i);
    });

    it('MISSING_CHARACTER_SELECTION primaryAction is choose_character', () => {
        expect(mapPhaseToUX('MISSING_CHARACTER_SELECTION').primaryAction).toBe('choose_character');
    });

    it('RETRY_DELAY primaryAction is retry_now', () => {
        expect(mapPhaseToUX('RETRY_DELAY').primaryAction).toBe('retry_now');
    });

    it('FAILED primaryAction is focus_credentials', () => {
        expect(mapPhaseToUX('FAILED').primaryAction).toBe('focus_credentials');
    });

    it('LOGIN_SENT primaryAction is cancel', () => {
        expect(mapPhaseToUX('LOGIN_SENT').primaryAction).toBe('cancel');
    });
});

describe('deriveStepStates', () => {
    const fullPrereqs = { hasGateway: true, hasCredentials: true, hasAgent: true, hasCharacter: true };

    it('marks previous steps as completed', () => {
        const ux = mapPhaseToUX('LOGIN_SENT'); // step 3
        const result = deriveStepStates(ux, fullPrereqs);
        expect(result.currentStep).toBe(2); // 0-based → step index 2
        expect(result.completedSteps).toEqual([0, 1]);
    });

    it('marks step 0 blocked when gateway missing', () => {
        const ux = mapPhaseToUX('MISSING_GATEWAY'); // step 1
        const result = deriveStepStates(ux, { ...fullPrereqs, hasGateway: false });
        expect(result.blockedSteps).toContain(0);
    });

    it('returns empty completed and blocked for step 1 with all prereqs', () => {
        const ux = mapPhaseToUX('DISCONNECTED'); // step 1
        const result = deriveStepStates(ux, fullPrereqs);
        expect(result.currentStep).toBe(0);
        expect(result.completedSteps).toEqual([]);
        expect(result.blockedSteps).toEqual([]);
    });

    it('handles AUTHENTICATED at step 4', () => {
        const ux = mapPhaseToUX('AUTHENTICATED'); // step 4
        const result = deriveStepStates(ux, fullPrereqs);
        expect(result.currentStep).toBe(3); // 0-based
        expect(result.completedSteps).toEqual([0, 1, 2]);
    });
});

describe('resolvePhaseToUX', () => {
    it('prefers backend fatal hint over static mapping', () => {
        const ux = resolvePhaseToUX({
            loginPhase: 'LOGIN_SENT',
            fatal: true,
        });
        expect(ux.isFatal).toBe(true);
    });

    it('uses backend category hints for unknown phase', () => {
        const ux = resolvePhaseToUX({
            loginPhase: 'SERVER_MAINTENANCE',
            uxCategory: 'AGENT',
            requiresInput: false,
            fatal: true,
        });
        expect(ux.progressStep).toBe(2);
        expect(ux.severity).toBe('error');
        expect(ux.rawPhase).toBe('SERVER_MAINTENANCE');
    });

    it('uses input-required hint to drive actionable CTA', () => {
        const ux = resolvePhaseToUX({
            loginPhase: 'WAITING_FOR_OTP',
            uxCategory: 'AUTH',
            requiresInput: true,
            fatal: false,
        });
        expect(ux.primaryAction).toBe('focus_credentials');
    });
});
