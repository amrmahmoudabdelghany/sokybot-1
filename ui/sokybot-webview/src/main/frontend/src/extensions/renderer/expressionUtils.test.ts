import { describe, expect, it } from 'vitest';
import { formatObjectForReactText, resolveTemplateInObject } from './expressionUtils';

describe('resolveTemplateInObject pure ${...} templates', () => {
    it('stringifies Gson-like byte array JSON instead of returning an object', () => {
        const gsonBytes = {
            values: [65, 66],
            strings: ['foo'],
            empty: false,
            valueCount: 2,
            bytes: [65, 66],
        };
        const out = resolveTemplateInObject('${x}', { x: gsonBytes });
        expect(out).toBe('[binary data, 2 bytes]');
    });

    it('preserves booleans for flags like hidden', () => {
        expect(resolveTemplateInObject('${ok}', { ok: true })).toBe(true);
        expect(resolveTemplateInObject('${ok}', { ok: false })).toBe(false);
    });

    it('preserves numbers for numeric props', () => {
        expect(resolveTemplateInObject('${n}', { n: 42 })).toBe(42);
    });
});

describe('formatObjectForReactText', () => {
    it('handles Gson byte view shape', () => {
        expect(
            formatObjectForReactText({
                values: [],
                strings: [],
                empty: true,
                valueCount: 0,
                bytes: [],
            })
        ).toBe('[binary data, 0 bytes]');
    });
});
