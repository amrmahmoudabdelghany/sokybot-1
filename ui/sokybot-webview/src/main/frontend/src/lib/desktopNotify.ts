import type { SocialAlertDto } from '../RSocketClient';

export function titleForAlert(alert: SocialAlertDto): string {
    const k = alert.kind ?? '';
    switch (k) {
        case 'GM_NEARBY':
            return 'GM nearby';
        case 'GM_WHISPER':
            return 'GM whisper';
        case 'NOTICE_GM_BROADCAST':
            return 'GM notice';
        case 'UNIQUE_SPAWNED':
            return 'Unique spawned';
        case 'UNIQUE_KILLED':
            return 'Unique killed';
        case 'HIVE_DISPATCHED':
            return 'Hive dispatched';
        case 'HIVE_COMPLETED':
            return 'Hive completed';
        case 'HIVE_ABORTED':
            return 'Hive aborted';
        default:
            return k ? `Alert: ${k}` : 'Sokybot alert';
    }
}

export function shouldDesktopNotify(alert: SocialAlertDto): boolean {
    const k = alert.kind;
    return k === 'GM_NEARBY' || k === 'GM_WHISPER' || k === 'UNIQUE_SPAWNED' || k === 'HIVE_ABORTED';
}

/**
 * Request browser notification permission (must be called from a user gesture for a prompt).
 */
export async function ensureNotificationPermission(): Promise<NotificationPermission> {
    if (typeof window === 'undefined' || !('Notification' in window)) {
        return 'denied';
    }
    if (Notification.permission === 'granted' || Notification.permission === 'denied') {
        return Notification.permission;
    }
    try {
        return await Notification.requestPermission();
    } catch {
        return Notification.permission;
    }
}

/**
 * Shows an OS notification when permission is granted. Caller should gate on {@code document.hidden}
 * for high-severity paths to avoid duplicate UX while the tab is focused.
 */
export function maybeShowDesktopNotification(alert: SocialAlertDto): void {
    if (typeof window === 'undefined' || !('Notification' in window)) {
        return;
    }
    if (Notification.permission !== 'granted') {
        return;
    }
    const title = titleForAlert(alert);
    const body = `${alert.subject} on ${alert.machineId}`;
    const tag = `${alert.machineId}:${alert.kind}`;
    try {
        new Notification(title, {
            body,
            tag,
            icon: '/icons/sokybot-192.png',
            silent: false,
        });
    } catch {
        // Older browsers / restrictive environments
    }
}
