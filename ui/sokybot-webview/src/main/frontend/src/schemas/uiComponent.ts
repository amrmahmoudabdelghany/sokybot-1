import { z } from 'zod';

const streamBindingSchema = z
    .object({
        streamId: z.string(),
    })
    .passthrough();

/** Schema tree node: matches rules previously enforced in validateSchema (plus passthrough for extra UI fields). */
export const uiComponentSchema: z.ZodType<unknown> = z.lazy(() =>
    z.union([
        z.object({ $ref: z.string() }),
        z
            .object({
                type: z.string().min(1),
                props: z.record(z.string(), z.unknown()).optional().nullable(),
                className: z.string().optional().nullable(),
                style: z.record(z.string(), z.unknown()).optional().nullable(),
                visibilityRule: z.unknown().optional().nullable(),
                disabledRule: z.unknown().optional().nullable(),
                stream: streamBindingSchema.optional().nullable(),
                children: z
                    .union([
                        z.string(),
                        z.array(uiComponentSchema),
                        uiComponentSchema,
                    ])
                    .optional()
                    .nullable(),
            })
            .passthrough(),
    ])
);

export const uiSchemaRoot = z.union([uiComponentSchema, z.array(uiComponentSchema)]);
