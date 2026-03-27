import { describe, it, expect } from 'vitest';
import { evaluateJsonDisabled, evaluateJsonVisibility } from './evaluateVisibility';

describe('evaluateJsonVisibility', () => {
    it('returns true when rule is null', async () => {
        expect(await evaluateJsonVisibility(null, {})).toBe(true);
    });

    it('evaluates shorthand all conditions', async () => {
        const def = {
            all: [
                { fact: 'ready', operator: 'equal', value: true },
            ],
        };
        expect(await evaluateJsonVisibility(def, { ready: true })).toBe(true);
        expect(await evaluateJsonVisibility(def, { ready: false })).toBe(false);
    });
});

describe('evaluateJsonDisabled', () => {
    it('returns false when rule is null', async () => {
        expect(await evaluateJsonDisabled(null, {})).toBe(false);
    });

    it('is true when shorthand all matches (emits disabled)', async () => {
        const def = {
            all: [{ fact: 'locked', operator: 'equal', value: true }],
        };
        expect(await evaluateJsonDisabled(def, { locked: true })).toBe(true);
        expect(await evaluateJsonDisabled(def, { locked: false })).toBe(false);
    });
});
