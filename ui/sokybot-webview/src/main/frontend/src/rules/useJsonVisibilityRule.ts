import { useEffect, useMemo, useState } from 'react';
import { evaluateJsonVisibility } from './evaluateVisibility';

export type JsonVisibility = 'pending' | boolean;

/**
 * Async rule evaluation for declarative components. Pending yields no subtree until resolved.
 */
export function useJsonVisibilityRule(
    rule: unknown | undefined,
    facts: Record<string, unknown>
): JsonVisibility {
    const factsKey = useMemo(() => {
        try {
            return JSON.stringify(facts);
        } catch {
            return String(facts);
        }
    }, [facts]);

    const [visible, setVisible] = useState<JsonVisibility>(() => (rule == null ? true : 'pending'));

    useEffect(() => {
        if (rule == null) {
            setVisible(true);
            return;
        }
        let cancelled = false;
        setVisible('pending');
        evaluateJsonVisibility(rule, facts).then((v) => {
            if (!cancelled) {
                setVisible(v);
            }
        });
        return () => {
            cancelled = true;
        };
    }, [rule, factsKey, facts]);

    return visible;
}
