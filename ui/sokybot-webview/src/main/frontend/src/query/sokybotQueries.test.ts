import { describe, it, expect, vi, beforeEach } from 'vitest';
import { rsocketService } from '../RSocketClient';
import { createTestQueryClient } from '../test/queryWrapper';
import { fetchExtensionSchema } from './sokybotQueries';
import { queryKeys } from './queryKeys';

vi.mock('../RSocketClient', () => ({
    rsocketService: {
        request: vi.fn(),
    },
}));

describe('fetchExtensionSchema', () => {
    it('calls extension.schema with pageId and machineId', async () => {
        vi.mocked(rsocketService.request).mockResolvedValue({ schema: { type: 'Stack' } });
        await fetchExtensionSchema('Log', 'g.bot');
        expect(rsocketService.request).toHaveBeenCalledWith('extension.schema', {
            pageId: 'Log',
            machineId: 'g.bot',
        });
    });
});

describe('extension schema query key and fetch integration', () => {
    beforeEach(() => {
        vi.mocked(rsocketService.request).mockReset();
    });

    it('prefetchQuery with scoped key stores data and invokes rsocket once', async () => {
        vi.mocked(rsocketService.request).mockResolvedValue({
            schema: { type: 'Box' },
            data: { x: 1 },
        });
        const client = createTestQueryClient();
        const key = queryKeys.sokybot.extensionSchema('PacketSniffer', 'group.machine');
        await client.prefetchQuery({
            queryKey: key,
            queryFn: () => fetchExtensionSchema('PacketSniffer', 'group.machine'),
        });
        expect(rsocketService.request).toHaveBeenCalledTimes(1);
        expect(client.getQueryData(key)).toMatchObject({ schema: { type: 'Box' }, data: { x: 1 } });
    });

    it('different machineId yields different cache entries', async () => {
        vi.mocked(rsocketService.request).mockImplementation(async (_method, params: any) => ({
            data: { id: params.machineId },
        }));
        const client = createTestQueryClient();
        const keyA = queryKeys.sokybot.extensionSchema('Log', 'g.a');
        const keyB = queryKeys.sokybot.extensionSchema('Log', 'g.b');
        await client.prefetchQuery({
            queryKey: keyA,
            queryFn: () => fetchExtensionSchema('Log', 'g.a'),
        });
        await client.prefetchQuery({
            queryKey: keyB,
            queryFn: () => fetchExtensionSchema('Log', 'g.b'),
        });
        expect(rsocketService.request).toHaveBeenCalledTimes(2);
        expect(client.getQueryData(keyA)).toMatchObject({ data: { id: 'g.a' } });
        expect(client.getQueryData(keyB)).toMatchObject({ data: { id: 'g.b' } });
    });

    it('key normalizes undefined machineId to empty string segment', () => {
        expect(queryKeys.sokybot.extensionSchema('P', undefined)).toEqual([
            'sokybot',
            'extensionSchema',
            'P',
            '',
        ]);
    });
});
