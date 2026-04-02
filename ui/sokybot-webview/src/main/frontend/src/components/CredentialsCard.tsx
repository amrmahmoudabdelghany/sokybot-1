import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { Button } from '@sokybot/frontend-shared';

const credentialsSchema = z.object({
    username: z.string().min(1, 'Username is required'),
    password: z.string().min(1, 'Password is required'),
    passcode: z.string(),
});

type CredentialsValues = z.infer<typeof credentialsSchema>;

interface CredentialsCardProps {
    initialValues: CredentialsValues;
    isOffline: boolean;
    onSubmit: (values: CredentialsValues) => Promise<void>;
}

export const CredentialsCard: React.FC<CredentialsCardProps> = ({
    initialValues,
    isOffline,
    onSubmit,
}) => {
    const form = useForm<CredentialsValues>({
        resolver: zodResolver(credentialsSchema),
        defaultValues: initialValues,
    });

    useEffect(() => {
        if (!form.formState.isDirty) {
            form.reset(initialValues);
        }
    }, [initialValues, form]);

    return (
        <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3">
            <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Authentication</div>
            <input
                className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                placeholder="Username"
                {...form.register('username')}
            />
            <input
                type="password"
                className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                placeholder="Password"
                {...form.register('password')}
            />
            <input
                type="password"
                className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                placeholder="Passcode"
                {...form.register('passcode')}
            />
            {(form.formState.errors.username || form.formState.errors.password || form.formState.errors.passcode) && (
                <p className="text-[11px] text-destructive">
                    {form.formState.errors.username?.message
                        || form.formState.errors.password?.message
                        || form.formState.errors.passcode?.message}
                </p>
            )}
            <Button className="w-full" disabled={isOffline} onClick={form.handleSubmit(onSubmit)}>
                Save Credentials
            </Button>
        </div>
    );
};
