import { describe, it, expect } from 'vitest';
import { isOnboardingCtaButtonDisabled } from './OnboardingCTA';

describe('isOnboardingCtaButtonDisabled', () => {
    const cases: Array<{
        isOffline: boolean;
        inFlight: boolean;
        aborting: boolean;
        intent: 'retry_now' | 'cancel';
        expected: boolean;
    }> = [
        { isOffline: false, inFlight: true, aborting: false, intent: 'retry_now', expected: true },
        { isOffline: false, inFlight: true, aborting: false, intent: 'cancel', expected: false },
        { isOffline: false, inFlight: true, aborting: true, intent: 'cancel', expected: true },
        { isOffline: true, inFlight: false, aborting: false, intent: 'cancel', expected: true },
    ];

    it.each(cases)(
        'offline=$isOffline inFlight=$inFlight aborting=$aborting intent=$intent => disabled=$expected',
        ({ isOffline, inFlight, aborting, intent, expected }) => {
            expect(isOnboardingCtaButtonDisabled(isOffline, inFlight, aborting, intent)).toBe(expected);
        }
    );

    it('disables non-cancel intents when aborting', () => {
        expect(isOnboardingCtaButtonDisabled(false, false, true, 'retry_now')).toBe(true);
    });

    it('allows retry_now when idle online', () => {
        expect(isOnboardingCtaButtonDisabled(false, false, false, 'retry_now')).toBe(false);
    });
});
