import { describe, it, expect } from 'vitest';
import { render } from '@testing-library/react';
import { StreamTablePanel } from './StreamTablePanel';

/**
 * Regression: StreamTablePanel must not use h-full; that starves the sibling flex-1
 * TableRenderer in stream-table-panel (jsdom does not compute layout heights).
 *
 * Manual QA: 0xA102 shows in Traffic Monitor / Packet Tracer only after a successful
 * gateway login (0x6102), not because of this component.
 */
describe('StreamTablePanel layout contract', () => {
  const binding = { streamId: 'packets', stateKey: 'trafficPackets', mode: 'append' } as const;

  it('root card uses flex-shrink-0 and omits h-full', () => {
    const { container: root } = render(
      <StreamTablePanel binding={binding} context={{ trafficPackets: [] }} />
    );
    const card = root.firstElementChild as HTMLElement | null;
    expect(card).toBeTruthy();
    const cls = card?.className ?? '';
    expect(cls).toMatch(/flex-shrink-0/);
    expect(cls).not.toMatch(/\bh-full\b/);
  });
});
