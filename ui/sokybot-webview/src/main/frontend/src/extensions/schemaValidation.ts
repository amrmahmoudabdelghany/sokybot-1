import type { UIComponent } from './ui-types';
import { uiSchemaRoot } from '../schemas/uiComponent';

export interface SchemaIssue {
    path: string;
    message: string;
}

function formatZodPath(path: (string | number)[]): string {
    if (path.length === 0) return '$';
    let out = '$';
    for (const segment of path) {
        if (typeof segment === 'number') {
            out += `[${segment}]`;
        } else {
            out += `.${segment}`;
        }
    }
    return out;
}

export function validateSchema(root: UIComponent | UIComponent[] | null | undefined): SchemaIssue[] {
    if (root == null) {
        return [{ path: '$', message: 'Node is null/undefined' }];
    }

    const result = uiSchemaRoot.safeParse(root);
    if (result.success) {
        return [];
    }

    return result.error.issues.map((issue) => ({
        path: formatZodPath(issue.path as (string | number)[]),
        message: issue.message,
    }));
}
