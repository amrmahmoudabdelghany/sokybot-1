import { describe, it, expect } from 'vitest';
import { redactSensitiveData, truncateForDisplay } from './redactSensitiveData';

describe('redactSensitiveData', () => {
    it('redacts password=value patterns', () => {
        expect(redactSensitiveData('password=secret123')).toContain('[REDACTED]');
        expect(redactSensitiveData('password=secret123')).not.toContain('secret123');
    });

    it('redacts token=value patterns', () => {
        const result = redactSensitiveData('access_token=abc123def');
        expect(result).not.toContain('abc123def');
    });

    it('redacts bearer tokens', () => {
        const result = redactSensitiveData('Authorization: bearer eyJhbGciOiJIUzI1NiJ9.test');
        expect(result).toContain('[REDACTED]');
        expect(result).not.toContain('eyJhbGciOiJIUzI1NiJ9');
    });

    it('redacts high-entropy hex strings (32+ chars)', () => {
        const hex = 'a'.repeat(32);
        const result = redactSensitiveData(`session: ${hex}`);
        expect(result).toContain('[REDACTED]');
        expect(result).not.toContain(hex);
    });

    it('preserves non-sensitive content', () => {
        const input = 'Gateway login response timeout';
        expect(redactSensitiveData(input)).toBe(input);
    });

    it('handles null input', () => {
        expect(redactSensitiveData(null)).toBe('');
    });

    it('handles undefined input', () => {
        expect(redactSensitiveData(undefined)).toBe('');
    });

    it('handles empty string', () => {
        expect(redactSensitiveData('')).toBe('');
    });

    it('does not crash on very long strings', () => {
        const longString = 'x'.repeat(10000);
        expect(() => redactSensitiveData(longString)).not.toThrow();
    });
});

describe('truncateForDisplay', () => {
    it('returns short strings unchanged', () => {
        expect(truncateForDisplay('hello', 100)).toBe('hello');
    });

    it('truncates long strings with ellipsis', () => {
        const result = truncateForDisplay('abcdefghij', 5);
        expect(result).toBe('abcde…');
        expect(result.length).toBe(6); // 5 chars + ellipsis
    });
});
