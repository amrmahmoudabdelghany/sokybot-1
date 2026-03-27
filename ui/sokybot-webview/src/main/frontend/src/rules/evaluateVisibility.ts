import { Engine } from 'json-rules-engine';
import type { Rule } from 'json-rules-engine';

function ruleEngineFromDef(def: unknown, successEventType: string, ruleName: string): Engine | null {
    if (def == null) {
        return null;
    }
    if (typeof def !== 'object') {
        console.warn(`[${ruleName}] expected object, got`, typeof def);
        return null;
    }

    const engine = new Engine([], { allowUndefinedFacts: true });

    if ('rules' in def && Array.isArray((def as { rules: unknown }).rules)) {
        for (const rule of (def as { rules: Rule[] }).rules) {
            engine.addRule(rule);
        }
    } else if ('all' in def || 'any' in def) {
        const conditions =
            'all' in def
                ? { all: (def as { all: unknown }).all }
                : { any: (def as { any: unknown }).any };
        engine.addRule({
            conditions: conditions as Rule['conditions'],
            event: { type: successEventType },
            name: ruleName,
            priority: 100,
        });
    } else {
        console.warn(`[${ruleName}] unsupported shape`, def);
        return null;
    }

    return engine;
}

/**
 * Run json-rules-engine with facts. Visibility is true when any rule emits event type `visible`.
 * Supports either full `rules` array or shorthand `{ all: [...] }` / `{ any: [...] }` condition trees.
 */
export async function evaluateJsonVisibility(
    def: unknown,
    facts: Record<string, unknown>
): Promise<boolean> {
    const engine = ruleEngineFromDef(def, 'visible', 'schema-visibility');
    if (def == null) {
        return true;
    }
    if (!engine) {
        return true;
    }
    const { events } = await engine.run(facts);
    return events.some((e) => e.type === 'visible');
}

/**
 * Returns true when rules emit `disabled` (component should be disabled).
 * Null/unsupported definition means not disabled.
 */
export async function evaluateJsonDisabled(
    def: unknown,
    facts: Record<string, unknown>
): Promise<boolean> {
    const engine = ruleEngineFromDef(def, 'disabled', 'schema-disabled');
    if (def == null) {
        return false;
    }
    if (!engine) {
        return false;
    }
    const { events } = await engine.run(facts);
    return events.some((e) => e.type === 'disabled');
}
