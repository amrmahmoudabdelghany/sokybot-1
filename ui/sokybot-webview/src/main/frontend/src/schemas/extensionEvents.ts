import { z } from 'zod';

const extensionPageAddedSchema = z.object({
    type: z.literal('extension.page.added'),
    pageId: z.string(),
    title: z.string(),
    iconPath: z.string().optional().nullable(),
    schema: z.unknown().optional().nullable(),
});

const extensionPageRemovedSchema = z.object({
    type: z.literal('extension.page.removed'),
    pageId: z.string(),
});

export const extensionUiEventDataSchema = z.discriminatedUnion('type', [
    extensionPageAddedSchema,
    extensionPageRemovedSchema,
]);

export type ExtensionUiEventData = z.infer<typeof extensionUiEventDataSchema>;
