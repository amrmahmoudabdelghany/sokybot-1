import { useState } from 'react';
import { Button } from '@sokybot/frontend-shared';
import { AlertsFeed } from '../components/social/AlertsFeed';
import { LiveChatPanel } from '../components/social/LiveChatPanel';
import { WebhookSettingsPanel } from '../components/social/WebhookSettingsPanel';
import { ensureNotificationPermission } from '../lib/desktopNotify';

export interface SocialPageProps {
    pageId: string;
    machineId?: string;
}

/**
 * Machine-scoped Social hub: live chat + alert feed. Registered as extension component type {@code social}.
 */
export function SocialPage({ machineId }: SocialPageProps) {
    const [permHint, setPermHint] = useState<string | null>(null);

    if (!machineId) {
        return (
            <div className="flex flex-1 items-center justify-center p-6 text-sm text-muted-foreground">
                Select a machine to view Social.
            </div>
        );
    }

    const onEnableDesktop = async () => {
        const p = await ensureNotificationPermission();
        setPermHint(p === 'granted' ? 'Desktop alerts enabled.' : `Permission: ${p}`);
    };

    return (
        <div className="flex min-h-0 flex-1 flex-col gap-3 p-4">
            <div className="flex flex-wrap items-center justify-between gap-2 border-b border-border/50 pb-3">
                <div>
                    <h2 className="text-sm font-semibold tracking-tight">Social</h2>
                    <p className="text-[11px] text-muted-foreground font-mono">{machineId}</p>
                </div>
                <Button type="button" size="sm" variant="outline" onClick={onEnableDesktop}>
                    Enable desktop alerts
                </Button>
            </div>
            {permHint ? <p className="text-[11px] text-muted-foreground">{permHint}</p> : null}
            <div className="grid min-h-0 flex-1 gap-4 lg:grid-cols-2">
                <LiveChatPanel machineId={machineId} />
                <AlertsFeed />
            </div>
            <WebhookSettingsPanel machineId={machineId} />
        </div>
    );
}
