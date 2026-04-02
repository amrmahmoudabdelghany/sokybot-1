import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
    Button,
    Select as SelectRoot,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    encodeSelectItemValue,
    decodeSelectItemValue,
} from '@sokybot/frontend-shared';

const agentSchema = z.object({
    targetAgent: z.string().min(1, 'Select an agent'),
});

type AgentValues = z.infer<typeof agentSchema>;

interface AgentServerCardProps {
    initialAgent: string;
    loginPhase: string;
    agentOptions: Array<{ value: string; label: string }>;
    isOffline: boolean;
    onSubmit: (values: AgentValues) => Promise<void>;
}

export const AgentServerCard: React.FC<AgentServerCardProps> = ({
    initialAgent,
    loginPhase,
    agentOptions,
    isOffline,
    onSubmit,
}) => {
    const form = useForm<AgentValues>({
        resolver: zodResolver(agentSchema),
        defaultValues: { targetAgent: initialAgent },
    });

    useEffect(() => {
        if (!form.formState.isDirty) {
            form.reset({ targetAgent: initialAgent });
        }
    }, [initialAgent, form]);

    return (
        <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3">
            <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Agent Server</div>
            {loginPhase === 'WAITING_FOR_AGENTS' && agentOptions.length === 0 && (
                <p className="text-[11px] text-muted-foreground leading-snug">
                    Waiting for the gateway to return the agent list...
                </p>
            )}
            {loginPhase === 'WAITING_FOR_AGENTS_TIMEOUT' && (
                <p className="text-[11px] text-amber-600/90 dark:text-amber-400/90 leading-snug">
                    Agent list timed out. Pick an agent if the list appears, or set a manual agent and retry.
                </p>
            )}
            <SelectRoot
                value={encodeSelectItemValue(form.watch('targetAgent') ?? '')}
                onValueChange={(v) =>
                    form.setValue('targetAgent', decodeSelectItemValue(v), {
                        shouldValidate: true,
                        shouldTouch: true,
                    })
                }
            >
                <SelectTrigger className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm">
                    <SelectValue placeholder={agentOptions.length === 0 ? 'Select discovered agent' : 'Select agent server'} />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value={encodeSelectItemValue('')}>
                        {agentOptions.length === 0 ? 'Select discovered agent' : 'Select agent server'}
                    </SelectItem>
                    {agentOptions.map((option) => (
                        <SelectItem key={option.value} value={encodeSelectItemValue(option.value)}>
                            {option.label}
                        </SelectItem>
                    ))}
                </SelectContent>
            </SelectRoot>
            {form.formState.errors.targetAgent && (
                <p className="text-[11px] text-destructive">{form.formState.errors.targetAgent.message}</p>
            )}
            <Button
                className="w-full"
                disabled={!form.watch('targetAgent') || (loginPhase === 'WAITING_FOR_AGENTS' && agentOptions.length === 0) || isOffline}
                onClick={form.handleSubmit(onSubmit)}
            >
                Save Agent Server
            </Button>
        </div>
    );
};
