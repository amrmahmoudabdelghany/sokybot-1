/**
 * Unified expression evaluation utilities for declarative JSON schemas.
 *
 * Supports the full range of JavaScript-like expressions used in schemas:
 *   - Simple property access:  item.name, settings.level
 *   - Logical operators:       error || 'Unknown', !item.enabled
 *   - Ternary:                 connected ? 'Active' : 'Offline'
 *   - Comparisons:             activeTab === 'traffic', count > 0
 *   - Function calls:          new Date(ts).toLocaleTimeString()
 *   - Arithmetic:              (currentHP / maxHP) * 100
 */

const SIMPLE_PATH_RE = /^[a-zA-Z_$][\w$]*(\.[a-zA-Z_$][\w$]*|\[\d+\])*$/;

function walkPath(expr: string, ctx: Record<string, any>): any {
    const parts = expr.split(/[.\[\]]/).filter(Boolean);
    let value: any = ctx;
    for (const part of parts) {
        if (value != null && typeof value === 'object') {
            value = value[part];
        } else {
            return undefined;
        }
    }
    return value;
}

/**
 * Evaluate any JS-like expression against a context object.
 * Uses a fast path for simple property access (obj.prop) and falls back
 * to `new Function` for everything else.
 */
export function safeEval(expr: string, ctx: Record<string, any>): any {
    const trimmed = expr.trim();
    if (!trimmed) return undefined;

    // Fast path: simple property access like "item.name" or "settings.level"
    if (SIMPLE_PATH_RE.test(trimmed)) {
        // Direct key lookup first (handles single-word keys that may shadow paths)
        if (trimmed in ctx) return ctx[trimmed];
        return walkPath(trimmed, ctx);
    }

    // Full evaluation via Function constructor
    try {
        const keys = Object.keys(ctx);
        const values = keys.map(k => ctx[k]);
        const fn = new Function(...keys, `try{return(${trimmed})}catch(e){return undefined}`);
        return fn(...values);
    } catch {
        return undefined;
    }
}

/** Gson-style JSON view of a Java byte[] (or similar) — not safe to pass to React as a text child. */
function isGsonByteArrayLike(o: object): boolean {
    return (
        'values' in o &&
        'empty' in o &&
        'valueCount' in o &&
        ('bytes' in o || 'strings' in o)
    );
}

/**
 * Turn backend objects into a single display string for template output and React text leaves.
 */
export function formatObjectForReactText(o: object): string {
    if (o instanceof Date) return o.toISOString();
    if (isGsonByteArrayLike(o)) {
        const any = o as Record<string, unknown>;
        const n =
            typeof any.valueCount === 'number'
                ? any.valueCount
                : Array.isArray(any.values)
                  ? any.values.length
                  : 0;
        return `[binary data, ${n} bytes]`;
    }
    try {
        return JSON.stringify(o);
    } catch {
        return '[object]';
    }
}

/**
 * Values from a whole-string "${...}" template must not stay as arbitrary objects:
 * they are used as DOM props / React children where only primitives are valid.
 */
function coercePureTemplateResult(raw: unknown): unknown {
    if (raw == null) return '';
    const t = typeof raw;
    if (t === 'string' || t === 'number' || t === 'boolean') return raw;
    if (raw instanceof Date) return raw.toISOString();
    if (Array.isArray(raw)) {
        if (raw.length === 0) return '';
        if (raw.every((e) => e == null || ['string', 'number', 'boolean'].includes(typeof e))) {
            return raw.map((e) => (e == null ? '' : String(e))).join(', ');
        }
        try {
            return JSON.stringify(raw);
        } catch {
            return '';
        }
    }
    if (t === 'object') return formatObjectForReactText(raw as object);
    return String(raw);
}

/**
 * Replace all ${...} template expressions in a string using safeEval.
 * Unresolvable or undefined values use a sensible default (0 for count-like, '' otherwise).
 */
export function resolveTemplate(template: string, ctx: Record<string, any>): string {
    if (!template || typeof template !== 'string') return template;

    return template.replace(/\$\{([^}]+)\}/g, (_match, expr) => {
        try {
            const value = safeEval(expr.trim(), ctx);
            if (value != null) return String(value);
            // Undefined/null: use 0 for count-like keys so UI never shows literal "${...}"
            const key = expr.trim();
            const isCount = /count|length|size|num|total/i.test(key);
            return isCount ? '0' : '';
        } catch {
            const key = typeof expr === 'string' ? expr.trim() : '';
            const isCount = /count|length|size|num|total/i.test(key);
            return isCount ? '0' : '';
        }
    });
}

/**
 * Recursively resolve ${...} template expressions inside objects, arrays, and strings.
 */
export function resolveTemplateInObject(obj: any, ctx: Record<string, any>): any {
    if (typeof obj === 'string') {
        // Pure expression: preserve original type (array, number, boolean, object)
        const pureMatch = obj.match(/^\$\{([^}]+)\}$/);
        if (pureMatch) {
            const raw = safeEval(pureMatch[1].trim(), ctx);
            return coercePureTemplateResult(raw);
        }
        return resolveTemplate(obj, ctx);
    }
    if (Array.isArray(obj)) {
        return obj.map(item => resolveTemplateInObject(item, ctx));
    }
    if (obj && typeof obj === 'object') {
        const resolved: any = {};
        for (const [key, value] of Object.entries(obj)) {
            resolved[key] = resolveTemplateInObject(value, ctx);
        }
        return resolved;
    }
    return obj;
}

/**
 * Remove entries from a params object whose values still contain unresolved
 * ${...} template expressions.  Prevents sending literal template strings
 * to the backend.
 */
export function cleanResolvedParams(params: Record<string, any>): Record<string, any> {
    const cleaned: Record<string, any> = {};
    for (const [key, value] of Object.entries(params)) {
        if (typeof value === 'string' && value.includes('${')) continue;
        cleaned[key] = value;
    }
    return cleaned;
}
