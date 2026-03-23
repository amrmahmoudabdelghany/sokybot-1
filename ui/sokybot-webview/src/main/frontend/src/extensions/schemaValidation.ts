import type { UIComponent } from './ui-types';

export interface SchemaIssue {
    path: string;
    message: string;
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
    return !!value && typeof value === 'object' && !Array.isArray(value);
}

export function validateSchema(root: UIComponent | UIComponent[] | null | undefined): SchemaIssue[] {
    const issues: SchemaIssue[] = [];

    const visit = (node: any, path: string) => {
        if (node == null) {
            issues.push({ path, message: 'Node is null/undefined' });
            return;
        }

        if (Array.isArray(node)) {
            node.forEach((child, idx) => visit(child, `${path}[${idx}]`));
            return;
        }

        // allow $ref objects used by schema loader
        if (isPlainObject(node) && '$ref' in node) {
            return;
        }

        if (!isPlainObject(node)) {
            issues.push({ path, message: `Node must be an object; got ${typeof node}` });
            return;
        }

        if (typeof node.type !== 'string' || node.type.trim() === '') {
            issues.push({ path, message: 'Missing/invalid "type" (must be non-empty string)' });
        }

        if (node.props !== undefined && node.props !== null && !isPlainObject(node.props)) {
            issues.push({ path, message: '"props" must be an object when provided' });
        }

        if (node.className !== undefined && node.className !== null && typeof node.className !== 'string') {
            issues.push({ path, message: '"className" must be a string when provided' });
        }

        if (node.style !== undefined && node.style !== null && !isPlainObject(node.style)) {
            issues.push({ path, message: '"style" must be an object when provided' });
        }

        const children = node.children;
        if (children !== undefined && children !== null) {
            if (typeof children === 'string') {
                // ok
            } else if (Array.isArray(children)) {
                children.forEach((child, idx) => visit(child, `${path}.children[${idx}]`));
            } else if (isPlainObject(children)) {
                // allow a single child object, although our UIComponent type prefers array|string
                visit(children, `${path}.children`);
            } else {
                issues.push({ path, message: '"children" must be a string or array (or object) when provided' });
            }
        }
    };

    visit(root as any, '$');
    return issues;
}

