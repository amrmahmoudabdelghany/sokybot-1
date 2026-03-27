import { describe, it, expect, vi } from 'vitest';
import { fireEvent, render, screen } from '@testing-library/react';
import { StreamTablePanel } from './StreamTablePanel';

describe('StreamTablePanel empty-state warning', () => {
  const binding = { streamId: 'packets', stateKey: 'trafficPackets', mode: 'append' } as const;

  it('shows ignored warning and unignore action when ignored drops exist', () => {
    const onAction = vi.fn().mockResolvedValue({});
    render(
      <StreamTablePanel
        binding={binding}
        context={{
          trafficPackets: [],
          totalPacketsSeen: 10,
          tracerCount: 2,
          droppedIgnoredCount: 3,
          droppedFilterCount: 0
        }}
        onAction={onAction}
      />
    );

    expect(screen.getByText(/hidden because one or more tracers are ignored/i)).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: /unignore all/i }));
    expect(onAction).toHaveBeenCalledWith('unignoreAllTracers', {});
  });

  it('shows filter warning and clear action when filter drops exist', () => {
    const onAction = vi.fn().mockResolvedValue({});
    render(
      <StreamTablePanel
        binding={binding}
        context={{
          trafficPackets: [],
          totalPacketsSeen: 8,
          tracerCount: 1,
          droppedIgnoredCount: 0,
          droppedFilterCount: 4
        }}
        onAction={onAction}
      />
    );

    expect(screen.getByText(/hidden by the active monitor filter/i)).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: /clear filter/i }));
    expect(onAction).toHaveBeenCalledWith('filterMonitor', { value: '' });
  });

  it('does not show warning when there are no active drop causes', () => {
    const { queryByText } = render(
      <StreamTablePanel
        binding={binding}
        context={{
          trafficPackets: [],
          totalPacketsSeen: 20,
          tracerCount: 5,
          droppedIgnoredCount: 0,
          droppedFilterCount: 0
        }}
      />
    );

    expect(queryByText(/Previously hidden packets are not replayed/i)).toBeNull();
  });
});
