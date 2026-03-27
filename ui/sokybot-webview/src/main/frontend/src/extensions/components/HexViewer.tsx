import React, { useEffect, useRef, useState, useCallback, useMemo } from 'react';
// Force Vite cache invalidation
import { cn } from '@sokybot/frontend-shared';

interface HexViewerProps {
  packets: any[];
  selectedHex?: string;
  selectedByteCount?: number;
  matchCount?: number;
  matches?: any[];
  onSelectHex?: (hex: string, startOffset: number, endOffset: number) => void;
  onDefineVariable?: (hex: string, packetName: string) => void;
  onDefineField?: (data: { name: string; type: string; offset: number; length: number; endian: string; opcode?: string }) => void;
  editable?: boolean;
  onInjectPacket?: (data: { hexPayload: string; direction: 'C2S' | 'S2C' }) => void;
  defaultDirection?: 'C2S' | 'S2C';
  structDefinitions?: Array<{ opcode?: string; opcodeValue?: number; fields?: Array<{ name?: string; offset?: number; length?: number; type?: string }> }>;
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
  onDefineField,
  editable = false,
  onInjectPacket,
  defaultDirection = 'C2S',
  structDefinitions = [],
  // groupLen = 16,
  className,
  style
}) => {
  const lineRef = useRef<HTMLDivElement>(null);
  const hexRef = useRef<HTMLDivElement>(null);
  const asciiRef = useRef<HTMLDivElement>(null);
  const [contextMenu, setContextMenu] = useState<{ x: number; y: number; hex: string; packetName: string; offset: number; opcode?: string } | null>(null);
  const [editableHex, setEditableHex] = useState('');
  const [focusedRange, setFocusedRange] = useState<{ start: number; end: number } | null>(null);
  const [fieldDialog, setFieldDialog] = useState<{
    open: boolean;
    hex: string;
    offset: number;
    opcode?: string;
    name: string;
    type: string;
    length: number;
    endian: 'little' | 'big';
  }>({
    open: false,
    hex: '',
    offset: 0,
    name: 'field',
    type: 'uint16',
    length: 1,
    endian: 'little'
  });

  const getOffsetFromNode = useCallback((node: Node | null): number | null => {
    if (!node) return null;
    const element = node instanceof HTMLElement ? node : node.parentElement;
    const byteElement = element?.closest?.('[data-byte-offset]') as HTMLElement | null;
    if (!byteElement) return null;
    const value = Number(byteElement.dataset.byteOffset);
    return Number.isFinite(value) ? value : null;
  }, []);

  const getOpcodeFromNode = useCallback((node: Node | null): string | undefined => {
    if (!node) return undefined;
    const element = node instanceof HTMLElement ? node : node.parentElement;
    const byteElement = element?.closest?.('[data-opcode]') as HTMLElement | null;
    const value = byteElement?.dataset.opcode;
    return value && value.length > 0 ? value : undefined;
  }, []);

  const COLOR_CLASSES = useMemo(
    () => [
      'bg-sky-500/30 text-sky-100',
      'bg-violet-500/30 text-violet-100',
      'bg-emerald-500/30 text-emerald-100',
      'bg-amber-500/30 text-amber-100',
      'bg-fuchsia-500/30 text-fuchsia-100',
      'bg-cyan-500/30 text-cyan-100',
      'bg-lime-500/30 text-lime-100',
      'bg-rose-500/30 text-rose-100'
    ],
    []
  );

  const normalizeOpcode = useCallback((opcode: string | number | undefined): string => {
    if (opcode === undefined || opcode === null) return '';
    if (typeof opcode === 'number') return `0x${opcode.toString(16)}`;
    return opcode.toLowerCase();
  }, []);

  const getFieldsForOpcode = useCallback((opcode: string | number | undefined) => {
    const normalized = normalizeOpcode(opcode);
    if (!normalized) return [];
    const definition = structDefinitions.find((def) => {
      const a = normalizeOpcode(def.opcode);
      const b = normalizeOpcode(def.opcodeValue);
      return normalized === a || normalized === b;
    });
    return Array.isArray(definition?.fields) ? definition!.fields! : [];
  }, [normalizeOpcode, structDefinitions]);

  const fieldColorClass = useCallback((field: { name?: string; offset?: number }) => {
    const key = `${field.name || ''}:${field.offset || 0}`;
    let hash = 0;
    for (let i = 0; i < key.length; i++) {
      hash = (hash * 31 + key.charCodeAt(i)) >>> 0;
    }
    return COLOR_CLASSES[hash % COLOR_CLASSES.length];
  }, [COLOR_CLASSES]);

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
        const selectedBytes = selectedText.replace(/\s+/g, '').length / 2;
        const anchorOffset = getOffsetFromNode(selection.anchorNode);
        const focusOffset = getOffsetFromNode(selection.focusNode);
        const offsetCandidates = [anchorOffset, focusOffset].filter((v): v is number => v !== null);
        const startOffset = offsetCandidates.length > 0 ? Math.min(...offsetCandidates) : 0;
        const endOffset = startOffset + selectedBytes;

        if (onSelectHex) {
          onSelectHex(selectedText, startOffset, endOffset);
        }
      }
    }
  }, [getOffsetFromNode, onSelectHex]);

  const handleContextMenu = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    e.preventDefault();
    const selection = window.getSelection();
    if (selection && selection.toString().trim()) {
      const selectedText = selection.toString().trim();
      if (/^([A-Fa-f0-9]{2}\s*)+$/.test(selectedText)) {
        const clickedOffset = getOffsetFromNode(e.target as Node) ?? 0;
        const opcode = getOpcodeFromNode(e.target as Node);
        const packetName = 'Unknown'; // TODO: Determine actual packet name
        setContextMenu({ x: e.clientX, y: e.clientY, hex: selectedText, packetName, offset: clickedOffset, opcode });
      }
    }
  }, [getOffsetFromNode, getOpcodeFromNode]);

  useEffect(() => {
    const handleClickOutside = () => {
      setContextMenu(null);
    };

    if (contextMenu) {
      document.addEventListener('click', handleClickOutside);
      return () => document.removeEventListener('click', handleClickOutside);
    }
  }, [contextMenu]);

  const renderHexLine = (packetRow: any, opcodeForRow?: string) => {
    const changed = new Set<number>(Array.isArray(packetRow.changedOffsets) ? packetRow.changedOffsets : []);
    const fields = getFieldsForOpcode(opcodeForRow || packetRow.opcode);
    const bytes = String(packetRow.hex || '').split(' ').filter(Boolean);
    return (
      <div className="flex flex-wrap gap-x-1">
        {bytes.map((value, idx) => {
          const offset = Number(packetRow.startOffset || 0) + idx;
          const matchedField = fields.find((field: any) => {
            const start = Number(field?.offset ?? -1);
            const len = Number(field?.length ?? 0);
            return start >= 0 && len > 0 && offset >= start && offset < start + len;
          });
          const isFocused = focusedRange != null && offset >= focusedRange.start && offset < focusedRange.end;
          const fieldClass = matchedField ? fieldColorClass(matchedField) : '';
          return (
            <span
              key={`${packetRow.startOffset}-${idx}`}
              id={`hex-byte-${offset}`}
              data-byte-offset={offset}
              data-opcode={opcodeForRow || packetRow.opcode}
              className={cn(
                "rounded px-0.5",
                fieldClass,
                changed.has(offset) && "ring-1 ring-yellow-300/80",
                isFocused && "ring-2 ring-primary animate-pulse"
              )}
              title={matchedField ? `offset ${offset} • ${matchedField.name || 'field'} (${matchedField.type || 'bytes'})` : `offset ${offset}`}
            >
              {value}
            </span>
          );
        })}
      </div>
    );
  };

  const decodePreview = useMemo(() => {
    if (!fieldDialog.open || !fieldDialog.hex) return '';
    const hex = fieldDialog.hex.replace(/\s+/g, '');
    if (hex.length < 2) return '';
    const byteLen = Math.floor(hex.length / 2);
    const bytes = new Uint8Array(byteLen);
    for (let i = 0; i < byteLen; i++) {
      bytes[i] = parseInt(hex.slice(i * 2, i * 2 + 2), 16);
    }
    const size = Math.min(Math.max(fieldDialog.length, 1), bytes.length);
    const view = new DataView(bytes.buffer, 0, Math.max(size, 1));
    const little = fieldDialog.endian === 'little';
    try {
      switch (fieldDialog.type) {
        case 'uint8':
          return String(view.getUint8(0));
        case 'uint16':
          return String(size >= 2 ? view.getUint16(0, little) : view.getUint8(0));
        case 'uint32':
          return String(size >= 4 ? view.getUint32(0, little) : view.getUint8(0));
        case 'int16':
          return String(size >= 2 ? view.getInt16(0, little) : view.getInt8(0));
        case 'int32':
          return String(size >= 4 ? view.getInt32(0, little) : view.getInt8(0));
        case 'float':
          return String(size >= 4 ? view.getFloat32(0, little) : view.getInt8(0));
        case 'string': {
          const decoder = new TextDecoder();
          return decoder.decode(bytes.slice(0, size)).replace(/\u0000/g, '');
        }
        default:
          return hex.slice(0, size * 2).toUpperCase();
      }
    } catch {
      return '';
    }
  }, [fieldDialog]);

  const renderPacketRows = () => {
    const rows: React.ReactElement[] = [];

    let currentOpcode: string | undefined;
    packets.forEach((packetRow: any, packetIndex: number) => {
      if (packetRow.type === 'header') {
        currentOpcode = packetRow.opcode;
        const opcodeFields = getFieldsForOpcode(packetRow.opcode);
        // Header row
        rows.push(
          <div key={`header-${packetIndex}`} className="flex border-b border-border py-1">
            <div className="w-24 px-2 text-xs text-muted-foreground"></div>
            <div className="flex-1 px-2">
              <span className={cn(
                "font-semibold",
                packetRow.source === 'SERVER' ? "text-orange-500" : "text-pink-600"
              )}>
                {packetRow.source}
              </span>
              {' '}
              <span className="italic text-emerald-600 dark:text-emerald-400">
                {packetRow.name}[{packetRow.opcode}]
              </span>
              {opcodeFields.length > 0 && (
                <div className="mt-1 flex flex-wrap gap-1">
                  {opcodeFields.map((field: any, idx: number) => (
                    <span
                      key={`${packetRow.opcode}-${idx}-${field.name || 'field'}`}
                      className={cn('text-[10px] px-1.5 py-0.5 rounded border border-border/40 cursor-pointer hover:brightness-110', fieldColorClass(field))}
                      title={`offset ${field.offset ?? 0}, len ${field.length ?? 1}`}
                      onClick={() => {
                        const start = Number(field?.offset ?? 0);
                        const length = Math.max(1, Number(field?.length ?? 1));
                        const end = start + length;
                        setFocusedRange({ start, end });
                        const target = document.getElementById(`hex-byte-${start}`);
                        if (target) {
                          target.scrollIntoView({ block: 'center', inline: 'nearest', behavior: 'smooth' });
                        }
                        window.setTimeout(() => {
                          setFocusedRange(prev => (prev?.start === start && prev?.end === end ? null : prev));
                        }, 1800);
                      }}
                    >
                      {field.name || `field_${idx}`}
                    </span>
                  ))}
                </div>
              )}
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
              "flex border-b border-border/40 py-0.5 hover:bg-muted/20 transition-colors",
              isHighlighted && "bg-primary/20"
            )}
          >
            <div
              ref={packetIndex === 0 && packetRow.startOffset === 0 ? lineRef : undefined}
              className="w-24 px-2 text-xs text-muted-foreground select-none border-r border-border/40"
              onScroll={handleScroll('line')}
            >
              {packetRow.lineNumber}
            </div>
            <div
              ref={packetIndex === 0 && packetRow.startOffset === 0 ? hexRef : undefined}
              className="flex-1 px-2 font-mono text-sm select-text text-foreground border-r border-border/40"
              onScroll={handleScroll('hex')}
              onMouseUp={handleHexSelection}
              onContextMenu={handleContextMenu}
            >
              {renderHexLine(packetRow, currentOpcode)}
            </div>
            <div
              ref={packetIndex === 0 && packetRow.startOffset === 0 ? asciiRef : undefined}
              className="w-64 px-2 font-mono text-[13px] text-muted-foreground select-text"
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

  const hasData = Array.isArray(packets) && packets.length > 0;

  const initialEditableHex = useMemo(() => {
    if (!editable || !packets.length) {
      return '';
    }
    const firstData = packets.find((row: any) => row.type === 'data');
    return firstData ? String(firstData.hex || '').replace(/\s+/g, ' ').trim() : '';
  }, [editable, packets]);

  return (
    <div className={cn("h-full flex flex-col relative min-h-0", className)} style={style}>
      <div className="flex-1 overflow-auto min-h-0 bg-background">
        {hasData ? (
          <div className="flex flex-col min-w-max">
            {/* Header Row */}
            <div className="flex sticky top-0 z-10 bg-muted/80 backdrop-blur-sm border-b border-border shadow-sm">
              <div className="w-24 border-r border-border/60 px-2 py-1 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                Offset
              </div>
              <div className="flex-1 border-r border-border/60 px-2 py-1 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                Hex
              </div>
              <div className="w-64 px-2 py-1 text-xs font-semibold text-muted-foreground uppercase tracking-wider">
                ASCII
              </div>
            </div>
            {/* Data Rows */}
            <div className="flex-1">
              {renderPacketRows()}
            </div>
          </div>
        ) : (
          <div className="flex items-center justify-center h-full min-h-[200px] text-muted-foreground text-sm p-4">
            No packet data to display
          </div>
        )}
      </div>

      {contextMenu && (
        <div
          className="fixed z-50 bg-popover border border-border rounded shadow-lg py-1"
          style={{ left: contextMenu.x, top: contextMenu.y }}
        >
          <button
            className="w-full px-4 py-2 text-left text-sm hover:bg-muted text-popover-foreground"
            onClick={() => {
              if (onDefineVariable) {
                onDefineVariable(contextMenu.hex, contextMenu.packetName);
              }
              setContextMenu(null);
            }}
          >
            Define Variable...
          </button>
          <button
            className="w-full px-4 py-2 text-left text-sm hover:bg-muted text-popover-foreground"
            onClick={() => {
              const selectionLen = Math.max(1, Math.floor(contextMenu.hex.replace(/\s+/g, '').length / 2));
              setFieldDialog({
                open: true,
                hex: contextMenu.hex,
                offset: contextMenu.offset,
                opcode: contextMenu.opcode,
                name: 'field',
                type: 'uint16',
                length: selectionLen,
                endian: 'little'
              });
              setContextMenu(null);
            }}
          >
            Define Struct Field...
          </button>
        </div>
      )}
      {fieldDialog.open && (
        <div className="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4">
          <div className="w-full max-w-md rounded border border-border bg-background p-4 space-y-3 shadow-xl">
            <div className="text-sm font-semibold">Define Struct Field</div>
            <div className="grid grid-cols-2 gap-2">
              <label className="text-xs text-muted-foreground col-span-2">
                Name
                <input
                  className="mt-1 w-full border border-border rounded px-2 py-1 text-sm"
                  value={fieldDialog.name}
                  onChange={(e) => setFieldDialog(prev => ({ ...prev, name: e.target.value }))}
                />
              </label>
              <label className="text-xs text-muted-foreground">
                Type
                <select
                  className="mt-1 w-full border border-border rounded px-2 py-1 text-sm bg-background"
                  value={fieldDialog.type}
                  onChange={(e) => setFieldDialog(prev => ({ ...prev, type: e.target.value }))}
                >
                  {['uint8', 'uint16', 'uint32', 'int16', 'int32', 'float', 'string', 'bytes'].map(type => (
                    <option key={type} value={type}>{type}</option>
                  ))}
                </select>
              </label>
              <label className="text-xs text-muted-foreground">
                Endian
                <select
                  className="mt-1 w-full border border-border rounded px-2 py-1 text-sm bg-background"
                  value={fieldDialog.endian}
                  onChange={(e) => setFieldDialog(prev => ({ ...prev, endian: (e.target.value === 'big' ? 'big' : 'little') }))}
                >
                  <option value="little">little</option>
                  <option value="big">big</option>
                </select>
              </label>
              <label className="text-xs text-muted-foreground">
                Offset
                <input
                  type="number"
                  className="mt-1 w-full border border-border rounded px-2 py-1 text-sm"
                  value={fieldDialog.offset}
                  onChange={(e) => setFieldDialog(prev => ({ ...prev, offset: Number(e.target.value) || 0 }))}
                />
              </label>
              <label className="text-xs text-muted-foreground">
                Length
                <input
                  type="number"
                  min={1}
                  className="mt-1 w-full border border-border rounded px-2 py-1 text-sm"
                  value={fieldDialog.length}
                  onChange={(e) => setFieldDialog(prev => ({ ...prev, length: Math.max(1, Number(e.target.value) || 1) }))}
                />
              </label>
            </div>
            <div className="text-xs text-muted-foreground">
              Preview: <span className="font-mono text-foreground">{decodePreview || '-'}</span>
            </div>
            <div className="flex justify-end gap-2">
              <button
                className="px-3 py-1 text-sm rounded border border-border hover:bg-muted"
                onClick={() => setFieldDialog(prev => ({ ...prev, open: false }))}
              >
                Cancel
              </button>
              <button
                className="px-3 py-1 text-sm rounded border border-border hover:bg-muted"
                onClick={() => {
                  if (!fieldDialog.name.trim()) return;
                  onDefineField?.({
                    name: fieldDialog.name.trim(),
                    type: fieldDialog.type,
                    offset: fieldDialog.offset,
                    length: fieldDialog.length,
                    endian: fieldDialog.endian,
                    opcode: fieldDialog.opcode
                  });
                  setFieldDialog(prev => ({ ...prev, open: false }));
                }}
              >
                Save Field
              </button>
            </div>
          </div>
        </div>
      )}
      {editable && (
        <div className="border-t border-border p-2 space-y-2">
          <textarea
            className="w-full min-h-[90px] bg-background border border-border rounded p-2 font-mono text-xs"
            value={editableHex || initialEditableHex}
            onChange={(e) => setEditableHex(e.target.value)}
            placeholder="Edit packet hex bytes..."
          />
          <div className="flex gap-2 justify-end">
            <button
              className="px-3 py-1 text-sm rounded border border-border hover:bg-muted"
              onClick={() => onInjectPacket?.({ hexPayload: editableHex || initialEditableHex, direction: defaultDirection })}
            >
              Send {defaultDirection}
            </button>
            <button
              className="px-3 py-1 text-sm rounded border border-border hover:bg-muted"
              onClick={() => onInjectPacket?.({ hexPayload: editableHex || initialEditableHex, direction: defaultDirection === 'C2S' ? 'S2C' : 'C2S' })}
            >
              Send {defaultDirection === 'C2S' ? 'S2C' : 'C2S'}
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
