/**
 * Type definitions for declarative UI extension system.
 * Shared by DeclarativeExtensionView, ComponentRenderer, and dev kit.
 */
import type { ComponentType } from 'react';

export type StreamMergeMode = 'replace' | 'append' | 'delta' | 'snapshot';
export type OrphanDeltaPolicy = 'ignore' | 'upsert';
export type PauseQueuePolicy = 'bufferBounded' | 'dropIncoming';
export type FlushWhilePausedPolicy = 'applyImmediately' | 'deferUntilResume';
export type MissingRowKeyPolicy = 'reject' | 'generate';

export interface StreamControlConfig {
    flushSignalType?: string; // default: FLUSH
    snapshotSignalType?: string; // default: SNAPSHOT
    deleteField?: string; // default: _delete
}

export interface StreamBindingConfig {
    streamId: string;
    stateKey?: string;
    mode?: StreamMergeMode;
    streamParams?: Record<string, any>;
    maxCapacity?: number;
    rollingWindow?: number;
    maxQueueSize?: number;
    rowKey?: string;
    rowKeyExtractor?: string;
    orphanDeltaPolicy?: OrphanDeltaPolicy;
    pauseQueuePolicy?: PauseQueuePolicy;
    flushWhilePaused?: FlushWhilePausedPolicy;
    missingRowKeyPolicy?: MissingRowKeyPolicy;
    preFilterExpression?: string;
    initialSnapshot?: boolean;
    maxRowSize?: number;
    control?: StreamControlConfig;
}

export interface RSocketRequestBinding {
    kind: 'request';
    method: string;
    params?: Record<string, unknown>;
}

export interface RSocketStreamBinding {
    kind: 'stream';
    method: string;
    params?: Record<string, unknown>;
}

export interface RSocketFireAndForgetBinding {
    kind: 'fireAndForget';
    method: string;
    params?: Record<string, unknown>;
}

export interface RSocketChannelBinding {
    kind: 'channel';
    method: string;
    params?: Record<string, unknown>;
}

export type RSocketBinding =
    | RSocketRequestBinding
    | RSocketStreamBinding
    | RSocketFireAndForgetBinding
    | RSocketChannelBinding;

export interface UIComponent {
    type: string;  // Component type (shadcn name, HTML tag, or custom type)
    props?: Record<string, any>;
    children?: UIComponent[] | string;  // Support text children
    className?: string;  // Tailwind classes
    style?: Record<string, any>;
    key?: string;
    /** Declarative visibility via json-rules-engine (facts = render context). Omit to use props.hidden only. */
    visibilityRule?: unknown;
    /** When rules emit `disabled`, merged with props.disabled / readOnly (facts = render context). */
    disabledRule?: unknown;

    // Special props for library integration
    icon?: string;  // Lucide icon name
    iconProps?: Record<string, any>;
    variant?: string;  // For shadcn components with variants
    size?: string;     // For shadcn components with sizes
    stream?: StreamBindingConfig;
    rsocket?: RSocketBinding;
}

export interface UIPage {
    pageId: string;
    title: string;
    iconPath?: string;
    schema: UIComponent | UIComponent[];  // Root component(s)
    dataSource?: string;  // RSocket endpoint for dynamic data
    actions?: Record<string, string>;  // Action handlers
}

export type ExtensionComponentType =
    | 'dataTable'
    | 'form'
    | 'custom'
    | 'declarative'
    | 'inventory'
    | 'skills'
    | 'training'
    | string;  // Allow custom types

export interface ExtensionComponent {
    component: ComponentType<any>;
    defaultProps?: Record<string, any>;
}
