import { describe, it, expect } from 'vitest';
import { applyStreamBatch } from './streamState';
import type { StreamEnvelope } from './streamState';

describe('streamState', () => {
  it('caps append data by maxCapacity', () => {
    const envelopes: StreamEnvelope[] = [
      {
        streamId: 'packets',
        binding: { streamId: 'packets', stateKey: 'trafficPackets', mode: 'append', maxCapacity: 3 },
        payload: [{ id: 1 }, { id: 2 }, { id: 3 }, { id: 4 }]
      }
    ];
    const next = applyStreamBatch({}, envelopes);
    expect(next.trafficPackets).toEqual([{ id: 2 }, { id: 3 }, { id: 4 }]);
  });

  it('handles delta delete by rowKey', () => {
    const prev = { trafficPackets: [{ id: 'a', value: 1 }, { id: 'b', value: 2 }] };
    const envelopes: StreamEnvelope[] = [
      {
        streamId: 'packets',
        binding: { streamId: 'packets', stateKey: 'trafficPackets', mode: 'delta', rowKey: 'id' },
        payload: [{ id: 'a', _delete: true }]
      }
    ];
    const next = applyStreamBatch(prev, envelopes);
    expect(next.trafficPackets).toEqual([{ id: 'b', value: 2 }]);
  });

  it('applies snapshot control payload', () => {
    const envelopes: StreamEnvelope[] = [
      {
        streamId: 'packets',
        binding: { streamId: 'packets', stateKey: 'trafficPackets', mode: 'append' },
        payload: { type: 'SNAPSHOT', items: [{ id: 10 }, { id: 11 }] }
      }
    ];
    const next = applyStreamBatch({}, envelopes);
    expect(next.trafficPackets).toEqual([{ id: 10 }, { id: 11 }]);
  });

  it('stores STATUS payload in streamHealth', () => {
    const envelopes: StreamEnvelope[] = [
      {
        streamId: 'packets',
        binding: { streamId: 'packets', stateKey: 'trafficPackets', mode: 'append' },
        payload: { type: 'STATUS', subscriptionGeneration: 2, isSubscribed: true, bindingState: 'subscribed' }
      }
    ];
    const next = applyStreamBatch({}, envelopes);
    expect(next.streamHealth.packets.subscriptionGeneration).toBe(2);
    expect(next.streamHealth.packets.status).toBe('subscribed');
  });

  it('drops stale generation packets', () => {
    const prev = {
      streamHealth: {
        packets: {
          subscriptionGeneration: 3
        }
      },
      trafficPackets: [{ id: 'current', subscriptionGeneration: 3 }]
    };
    const envelopes: StreamEnvelope[] = [
      {
        streamId: 'packets',
        binding: { streamId: 'packets', stateKey: 'trafficPackets', mode: 'append' },
        payload: [
          { id: 'old', subscriptionGeneration: 2 },
          { id: 'new', subscriptionGeneration: 3 }
        ]
      }
    ];
    const next = applyStreamBatch(prev, envelopes);
    expect(next.trafficPackets).toEqual([
      { id: 'current', subscriptionGeneration: 3 },
      { id: 'new', subscriptionGeneration: 3 }
    ]);
  });

  it('normalizes missing STATUS diagnostics to safe defaults', () => {
    const envelopes: StreamEnvelope[] = [
      {
        streamId: 'packets',
        binding: { streamId: 'packets', stateKey: 'trafficPackets', mode: 'append' },
        payload: { type: 'STATUS', bindingState: 'subscribed', isSubscribed: true }
      }
    ];
    const next = applyStreamBatch({}, envelopes);
    expect(next.totalPacketsSeen).toBe(0);
    expect(next.droppedIgnoredCount).toBe(0);
    expect(next.droppedFilterCount).toBe(0);
    expect(next.lastDropReason).toBe('');
  });
});
