import { useEffect, useMemo, useState } from 'react';
import { evaluateJsonDisabled } from './evaluateVisibility';

export type JsonDisabled = 'pending' | boolean;

/**
 * Async rule evaluation: when pending, treat as not disabled (avoid blocking input until resolved).
 */
export function useJsonDisabledRule(
    rule: unknown | undefined,
    facts: Record<string, unknown>
): JsonDisabled {
    const factsKey = useMemo(() => {
        try {
            return JSON.stringify(facts);
        } catch {
            return String(facts);
        }
    }, [facts]);

    const [disabled, setDisabled] = useState<JsonDisabled>(() => (rule == null ? false : 'pending'));

    useEffect(() => {
        if (rule == null) {
            setDisabled(false);
            return;
        }
        let cancelled = false;
        setDisabled('pending');
        evaluateJsonDisabled(rule, facts).then((v) => {
            if (!cancelled) {
                setDisabled(v);
            }
        });
        return () => {
            cancelled = true;
        };
    }, [rule, factsKey, facts]);

    return disabled;
}
