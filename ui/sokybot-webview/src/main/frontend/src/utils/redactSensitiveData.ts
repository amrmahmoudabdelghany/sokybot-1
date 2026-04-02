/**
 * Redact sensitive data from diagnostic strings before rendering in UI.
 *
 * Patterns scrubbed:
 *   - Common secret keys: password, token, secret, passcode, session_id, auth, bearer
 *   - Key=value pairs where the key matches a sensitive pattern
 *   - High-entropy token-like substrings (32+ hex chars)
 *   - Bearer token values in authorization headers
 *
 * The redaction runs BEFORE data reaches React state used for UI rendering.
 */

const KEY_VALUE_PATTERN = /(password|passwd|token|secret|passcode|session[_-]?id|auth[_-]?header|bearer|api[_-]?key|access[_-]?key|credential)\s*[:=]\s*\S+/gi;
const BEARER_PATTERN = /bearer\s+[A-Za-z0-9._~+/=-]+/gi;
const HIGH_ENTROPY_HEX = /\b[0-9a-fA-F]{32,}\b/g;

const REDACTED = '[REDACTED]';
const MAX_DISPLAY_LENGTH = 2000;

/**
 * Redact sensitive values from a string.
 *
 * @param input - Raw diagnostic string (reason, topic, phase detail, etc.)
 * @returns Sanitized string safe for rendering.
 */
export function redactSensitiveData(input: string | null | undefined): string {
    if (input == null) return '';
    let s = String(input);

    // Cap length first to avoid regex DoS on oversized payloads
    if (s.length > MAX_DISPLAY_LENGTH * 2) {
        s = s.slice(0, MAX_DISPLAY_LENGTH * 2);
    }

    // Redact key=value pairs
    s = s.replace(KEY_VALUE_PATTERN, (match) => {
        const eqIdx = match.search(/[:=]/);
        if (eqIdx >= 0) {
            return match.slice(0, eqIdx + 1) + REDACTED;
        }
        return REDACTED;
    });

    // Redact bearer tokens
    s = s.replace(BEARER_PATTERN, `bearer ${REDACTED}`);

    // Redact high-entropy hex strings (likely tokens/session IDs)
    s = s.replace(HIGH_ENTROPY_HEX, REDACTED);

    return s;
}

/**
 * Truncate a string for display, appending an ellipsis if truncated.
 */
export function truncateForDisplay(input: string, maxLength: number = MAX_DISPLAY_LENGTH): string {
    if (input.length <= maxLength) return input;
    return input.slice(0, maxLength) + '…';
}
