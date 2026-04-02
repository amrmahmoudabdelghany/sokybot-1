import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@sokybot/frontend-shared';

const connectSchema = z.object({
    targetGateway: z.string().min(1, 'Gateway is required'),
});

type ConnectValues = z.infer<typeof connectSchema>;

interface GatewayConnectCardProps {
    initialGateway: string;
    inFlight: boolean;
    isOffline: boolean;
    onSubmit: (values: ConnectValues) => Promise<void>;
}

export const GatewayConnectCard: React.FC<GatewayConnectCardProps> = ({
    initialGateway,
    inFlight,
    isOffline,
    onSubmit,
}) => {
    const form = useForm<ConnectValues>({
        resolver: zodResolver(connectSchema),
        defaultValues: { targetGateway: initialGateway },
    });

    useEffect(() => {
        if (!form.formState.isDirty) {
            form.reset({ targetGateway: initialGateway });
        }
    }, [initialGateway, form]);

    return (
        <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3">
            <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Connect</div>
            <input
                className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                placeholder="Target Gateway (IP or host)"
                {...form.register('targetGateway')}
            />
            {form.formState.errors.targetGateway && (
                <p className="text-[11px] text-destructive">
                    {form.formState.errors.targetGateway.message}
                </p>
            )}
            <Button
                className="w-full"
                disabled={inFlight || isOffline}
                onClick={form.handleSubmit(onSubmit)}
            >
                {inFlight ? 'Connecting...' : 'Connect'}
            </Button>
        </div>
    );
};
