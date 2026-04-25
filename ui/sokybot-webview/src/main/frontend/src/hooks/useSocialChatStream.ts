import { useEffect } from 'react';
import type { SocialChannel } from '../RSocketClient';
import { useRSocketService } from '../RSocketProvider';
import { useSocialStore } from '../store/socialStore';

/**
 * Subscribes to {@code social.chat} for one machine; re-subscribes when filters change (server-side filtering).
 */
export function useSocialChatStream(machineId: string | undefined) {
    const rsocketService = useRSocketService();
    const pushChat = useSocialStore((s) => s.pushChat);
    const channelsKey = useSocialStore((s) =>
        Array.from(s.filters.channels)
            .sort()
            .join(',')
    );
    const includeSelf = useSocialStore((s) => s.filters.includeSelf);
    const includeGm = useSocialStore((s) => s.filters.includeGm);

    useEffect(() => {
        if (!machineId) {
            return;
        }
        const { filters } = useSocialStore.getState();
        const channels: SocialChannel[] | undefined =
            filters.channels.size === 0 ? undefined : Array.from(filters.channels);

        const sub = rsocketService.subscribeToSocialChat(
            {
                machineId,
                channels,
                includeSelf: filters.includeSelf,
                includeGm: filters.includeGm,
            },
            (line) => pushChat(machineId, line),
            (err) => console.error('social.chat stream error', err)
        );
        return () => sub.unsubscribe();
    }, [machineId, channelsKey, includeSelf, includeGm, rsocketService, pushChat]);
}
