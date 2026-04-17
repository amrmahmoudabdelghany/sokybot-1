const GATEWAY_RESULT_CODES: Record<number, string> = {
    1: 'Wrong username or password.',
    2: 'Wrong username or password.',
    4: 'Account is already connected. Wait before retrying.',
    6: 'Disconnected or blocked by the gateway.',
    0x0b: 'Wrong username or password.',
    0x0c: 'Wrong username or password.',
    0x0d: 'Wrong username or password.',
};

const AGENT_RESULT_CODES: Record<number, string> = {
    1: 'Agent authentication succeeded.',
};

function normalize(input: number | null | undefined): number | null {
    if (typeof input !== 'number' || Number.isNaN(input)) return null;
    return input;
}

export function describeGatewayResult(code: number | null | undefined): string | null {
    const value = normalize(code);
    if (value == null) return null;
    return GATEWAY_RESULT_CODES[value] ?? `Unknown gateway result code: ${value}`;
}

export function describeAgentResult(code: number | null | undefined): string | null {
    const value = normalize(code);
    if (value == null) return null;
    return AGENT_RESULT_CODES[value] ?? `Agent result code: ${value}`;
}

export function maybeVersionMismatchMessage(
    code: number | null | undefined,
    configuredClientVersion: number | null | undefined
): string | null {
    const value = normalize(code);
    const configured = normalize(configuredClientVersion);
    if (value == null || configured == null) return null;
    if (value === 6) {
        return `Possible client version mismatch. Configured gateway client version is ${configured}.`;
    }
    return null;
}

