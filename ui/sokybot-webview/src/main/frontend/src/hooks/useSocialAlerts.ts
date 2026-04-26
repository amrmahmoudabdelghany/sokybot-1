import { useEffect } from 'react';
import { useRSocketService } from '../RSocketProvider';
import { maybeShowDesktopNotification, shouldDesktopNotify, titleForAlert } from '../lib/desktopNotify';
import type { SocialAlertDto } from '../RSocketClient';
import { useSocialStore } from '../store/socialStore';
import type { ToastSeverity } from '../store/toastStore';
import { useToastStore } from '../store/toastStore';

function severityForAlert(alert: SocialAlertDto): ToastSeverity {
    const k = alert.kind ?? '';
    if (k === 'HIVE_ABORTED') {
        return 'error';
    }
    if (k === 'HIVE_DISPATCHED') {
        return 'warn';
    }
    if (k === 'HIVE_COMPLETED') {
        return 'info';
    }
    if (k.startsWith('GM_') || k === 'NOTICE_GM_BROADCAST') {
        return 'error';
    }
    if (k.startsWith('UNIQUE_')) {
        return 'warn';
    }
    return 'info';
}

/**
 * Global fan-in of {@code social.alerts} across all machines (empty params → observeAll on server).
 */
export function useSocialAlerts(enabled: boolean) {
    const rsocketService = useRSocketService();
    const pushAlert = useSocialStore((s) => s.pushAlert);
    const enqueue = useToastStore((s) => s.enqueue);

    useEffect(() => {
        if (!enabled) {
            return;
        }
        const sub = rsocketService.subscribeToSocialAlerts(
            {},
            (alert: SocialAlertDto) => {
                pushAlert(alert);
                enqueue({
                    kind: alert.kind,
                    title: titleForAlert(alert),
                    body: alert.subject,
                    severity: severityForAlert(alert),
                    timestamp: Date.now(),
                });
                if (shouldDesktopNotify(alert) && typeof document !== 'undefined' && document.hidden) {
                    maybeShowDesktopNotification(alert);
                }
            },
            (err) => console.error('social.alerts stream error', err)
        );
        return () => sub.unsubscribe();
    }, [enabled, rsocketService, pushAlert, enqueue]);
}
