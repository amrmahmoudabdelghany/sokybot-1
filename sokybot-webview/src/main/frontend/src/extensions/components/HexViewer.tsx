import React, { useEffect, useRef, useState, useCallback } from 'react';
import { cn } from '@/lib/utils';

interface HexViewerProps {
  packets: any[];
  selectedHex?: string;
  selectedByteCount?: number;
  matchCount?: number;
  matches?: any[];
  onSelectHex?: (hex: string, startOffset: number, endOffset: number) => void;
  onDefineVariable?: (hex: string, packetName: string) => void;
  groupLen?: number;
  className?: string;
  style?: React.CSSProperties;
}

export const HexViewer: React.FC<HexViewerProps> = ({
  packets,
  // selectedHex = '',
  // selectedByteCount = 0,
  // matchCount = 0,
  matches = [],
  onSelectHex,
  onDefineVariable,
  // groupLen = 16,
  className,
  style
}) => {
  const lineRef = useRef<HTMLDivElement>(null);
  const hexRef = useRef<HTMLDivElement>(null);
  const asciiRef = useRef<HTMLDivElement>(null);
  const [contextMenu, setContextMenu] = useState<{ x: number; y: number; hex: string; packetName: string } | null>(null);

  const handleScroll = useCallback((source: 'line' | 'hex' | 'ascii') => {
    return (e: React.UIEvent<HTMLDivElement>) => {
      const scrollTop = e.currentTarget.scrollTop;
      const scrollLeft = e.currentTarget.scrollLeft;

      if (source !== 'line' && lineRef.current) {
        lineRef.current.scrollTop = scrollTop;
      }
      if (source !== 'hex' && hexRef.current) {
        hexRef.current.scrollTop = scrollTop;
        hexRef.current.scrollLeft = scrollLeft;
      }
      if (source !== 'ascii' && asciiRef.current) {
        asciiRef.current.scrollTop = scrollTop;
      }
    };
  }, []);

  const handleHexSelection = useCallback(() => {
    const selection = window.getSelection();
    if (selection && selection.toString().trim()) {
      const selectedText = selection.toString().trim();
      // Check if it's a valid hex string
      if (/^([A-Fa-f0-9]{2}\s*)+$/.test(selectedText)) {
        // const range = selection.getRangeAt(0);
        // Calculate offsets (simplified - would need more complex logic for exact byte positions)
        const startOffset = 0; // TODO: Calculate actual byte offset
        const endOffset = selectedText.replace(/\s+/g, '').length / 2;

        if (onSelectHex) {
          onSelectHex(selectedText, startOffset, endOffset);
        }
      }
    }
  }, [onSelectHex]);

  const handleContextMenu = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    e.preventDefault();
    const selection = window.getSelection();
    if (selection && selection.toString().trim()) {
      const selectedText = selection.toString().trim();
      if (/^([A-Fa-f0-9]{2}\s*)+$/.test(selectedText)) {
        // Find which packet this selection belongs to (simplified)
        const packetName = 'Unknown'; // TODO: Determine actual packet name
        setContextMenu({ x: e.clientX, y: e.clientY, hex: selectedText, packetName });
      }
    }
  }, []);

  useEffect(() => {
    const handleClickOutside = () => {
      setContextMenu(null);
    };

    if (contextMenu) {
      document.addEventListener('click', handleClickOutside);
      return () => document.removeEventListener('click', handleClickOutside);
    }
  }, [contextMenu]);

  const renderPacketRows = () => {
    const rows: JSX.Element[] = [];

    packets.forEach((packetRow: any, packetIndex: number) => {
      if (packetRow.type === 'header') {
        // Header row
        rows.push(
          <div key={`header-${packetIndex}`} className="flex border-b border-slate-300 dark:border-slate-600 py-1">
            <div className="w-24 px-2 text-xs text-slate-500 dark:text-slate-400"></div>
            <div className="flex-1 px-2">
              <span className={cn(
                "font-semibold",
                packetRow.source === 'SERVER' ? "text-orange-500" : "text-pink-600"
              )}>
                {packetRow.source}
              </span>
              {' '}
              <span className="italic text-green-600 dark:text-green-400">
                {packetRow.name}[{packetRow.opcode}]
              </span>
            </div>
            <div className="w-64 px-2"></div>
          </div>
        );
      } else if (packetRow.type === 'data') {
        // Data row
        const isHighlighted = matches?.some(m =>
          m.packetIndex === packetIndex &&
          m.byteOffset >= packetRow.startOffset &&
          m.byteOffset < packetRow.endOffset
        );

        rows.push(
          <div
            key={`data-${packetIndex}-${packetRow.startOffset}`}
            className={cn(
              "flex border-b border-slate-200 dark:border-slate-700 py-0.5",
              isHighlighted && "bg-blue-200 dark:bg-blue-900"
            )}
          >
            <div
              ref={packetIndex === 0 && packetRow.startOffset === 0 ? lineRef : undefined}
              className="w-24 px-2 text-xs text-slate-500 dark:text-slate-400 select-none"
              onScroll={handleScroll('line')}
            >
              {packetRow.lineNumber}
            </div>
            <div
              ref={packetIndex === 0 && packetRow.startOffset === 0 ? hexRef : undefined}
              className="flex-1 px-2 font-mono text-sm select-text"
              onScroll={handleScroll('hex')}
              onMouseUp={handleHexSelection}
              onContextMenu={handleContextMenu}
            >
              {packetRow.hex}
            </div>
            <div
              ref={packetIndex === 0 && packetRow.startOffset === 0 ? asciiRef : undefined}
              className="w-64 px-2 font-mono text-sm text-slate-400 dark:text-slate-500 select-none"
              onScroll={handleScroll('ascii')}
            >
              {packetRow.ascii}
            </div>
          </div>
        );
      } else if (packetRow.type === 'separator') {
        // Separator row
        rows.push(
          <div key={`separator-${packetIndex}`} className="h-2"></div>
        );
      }
    });

    return rows;
  };

  return (
    <div className={cn("h-full flex flex-col relative", className)} style={style}>
      <div className="flex-1 overflow-auto">
        <div className="flex">
          <div className="w-24 border-r border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900/50">
            <div className="sticky top-0 bg-slate-100 dark:bg-slate-800 px-2 py-1 text-xs font-semibold border-b">
              Offset
            </div>
            {renderPacketRows()}
          </div>
          <div className="flex-1 border-r border-slate-200 dark:border-slate-700">
            <div className="sticky top-0 bg-slate-100 dark:bg-slate-800 px-2 py-1 text-xs font-semibold border-b">
              Hex
            </div>
            {renderPacketRows()}
          </div>
          <div className="w-64 bg-slate-50 dark:bg-slate-900/50">
            <div className="sticky top-0 bg-slate-100 dark:bg-slate-800 px-2 py-1 text-xs font-semibold border-b">
              ASCII
            </div>
            {renderPacketRows()}
          </div>
        </div>
      </div>

      {contextMenu && (
        <div
          className="fixed z-50 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded shadow-lg py-1"
          style={{ left: contextMenu.x, top: contextMenu.y }}
        >
          <button
            className="w-full px-4 py-2 text-left text-sm hover:bg-slate-100 dark:hover:bg-slate-700"
            onClick={() => {
              if (onDefineVariable) {
                onDefineVariable(contextMenu.hex, contextMenu.packetName);
              }
              setContextMenu(null);
            }}
          >
            Define Variable...
          </button>
        </div>
      )}
    </div>
  );
};
