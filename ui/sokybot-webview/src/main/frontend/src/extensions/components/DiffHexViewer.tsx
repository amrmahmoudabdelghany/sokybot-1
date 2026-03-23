import React from 'react';
import { HexViewer } from './HexViewer';

interface DiffHexViewerProps {
  packetsA: any[];
  packetsB: any[];
  className?: string;
  style?: React.CSSProperties;
}

export const DiffHexViewer: React.FC<DiffHexViewerProps> = ({ packetsA, packetsB, className, style }) => {
  return (
    <div className={className} style={style}>
      <div className="grid grid-cols-2 gap-3 h-full">
        <div className="border border-border rounded overflow-hidden min-h-[260px]">
          <div className="px-2 py-1 text-xs font-semibold border-b border-border bg-muted/60">Packet A</div>
          <HexViewer packets={packetsA || []} />
        </div>
        <div className="border border-border rounded overflow-hidden min-h-[260px]">
          <div className="px-2 py-1 text-xs font-semibold border-b border-border bg-muted/60">Packet B</div>
          <HexViewer packets={packetsB || []} />
        </div>
      </div>
    </div>
  );
};
