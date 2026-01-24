import { rsocketService } from '../lib/rsocket-client';

export interface Bundle {
    id: number;
    symbolicName: string;
    version: string;
    state: string;
    location: string;
    lastModified: number;
    isWatchingForChanges?: boolean;
    needsReload?: boolean;
    fileExists?: boolean;
}

export interface Service {
    interfaces: string[];
    serviceId: number;
    bundleId: number;
    bundleSymbolicName: string;
    properties: Record<string, any>;
}

export interface Metrics {
    memory: {
        heapUsed: number;
        heapMax: number;
        heapCommitted: number;
        heapPercent: number;
        nonHeapUsed: number;
        nonHeapMax: number;
        nonHeapCommitted: number;
    };
    threads: {
        threadCount: number;
        peakThreadCount: number;
        daemonThreadCount: number;
        totalStartedThreadCount: number;
    };
    runtime: {
        availableProcessors: number;
        uptime: number;
        startTime: number;
    };
    timestamp: number;
}

export interface DevToolsResponse<T = any> {
    success: boolean;
    data?: T;
    error?: string;
    message?: string;
}

/**
 * Service for interacting with dev-tools backend via RSocket.
 */
export class DevToolsService {
    
    /**
     * Get all bundles.
     */
    async getBundles(): Promise<Bundle[]> {
        const response = await rsocketService.requestResponse("devtools:bundles.list");
        const result: DevToolsResponse<Bundle[]> = JSON.parse(response);
        if (result.success && result.data) {
            return result.data;
        }
        throw new Error(result.error || "Failed to get bundles");
    }
    
    /**
     * Get bundle by symbolic name.
     */
    async getBundle(symbolicName: string): Promise<Bundle | null> {
        const response = await rsocketService.requestResponse(`devtools:bundles.get:${symbolicName}`);
        const result: DevToolsResponse<Bundle> = JSON.parse(response);
        if (result.success && result.data) {
            return result.data;
        }
        return null;
    }
    
    /**
     * Reload bundle from filesystem.
     */
    async reloadBundle(symbolicName: string): Promise<void> {
        const response = await rsocketService.requestResponse(`devtools:bundles.reload:${symbolicName}`);
        const result: DevToolsResponse = JSON.parse(response);
        if (!result.success) {
            throw new Error(result.error || "Failed to reload bundle");
        }
    }
    
    /**
     * Start bundle.
     */
    async startBundle(bundleId: number): Promise<void> {
        const response = await rsocketService.requestResponse(`devtools:bundles.start:${bundleId}`);
        const result: DevToolsResponse = JSON.parse(response);
        if (!result.success) {
            throw new Error(result.error || "Failed to start bundle");
        }
    }
    
    /**
     * Stop bundle.
     */
    async stopBundle(bundleId: number): Promise<void> {
        const response = await rsocketService.requestResponse(`devtools:bundles.stop:${bundleId}`);
        const result: DevToolsResponse = JSON.parse(response);
        if (!result.success) {
            throw new Error(result.error || "Failed to stop bundle");
        }
    }
    
    /**
     * Restart bundle.
     */
    async restartBundle(bundleId: number): Promise<void> {
        const response = await rsocketService.requestResponse(`devtools:bundles.restart:${bundleId}`);
        const result: DevToolsResponse = JSON.parse(response);
        if (!result.success) {
            throw new Error(result.error || "Failed to restart bundle");
        }
    }
    
    /**
     * Get bundle dependencies.
     */
    async getBundleDependencies(bundleId: number): Promise<any> {
        const response = await rsocketService.requestResponse(`devtools:bundles.dependencies:${bundleId}`);
        const result: DevToolsResponse = JSON.parse(response);
        if (result.success && result.data) {
            return result.data;
        }
        throw new Error(result.error || "Failed to get dependencies");
    }
    
    /**
     * Get development status for bundle.
     */
    async getDevStatus(symbolicName: string): Promise<any> {
        const response = await rsocketService.requestResponse(`devtools:bundles.devStatus:${symbolicName}`);
        const result: DevToolsResponse = JSON.parse(response);
        if (result.success && result.data) {
            return result.data;
        }
        return null;
    }
    
    /**
     * Get all services.
     */
    async getServices(): Promise<Service[]> {
        const response = await rsocketService.requestResponse("devtools:services.list");
        const result: DevToolsResponse<Service[]> = JSON.parse(response);
        if (result.success && result.data) {
            return result.data;
        }
        throw new Error(result.error || "Failed to get services");
    }
    
    /**
     * Find services by interface.
     */
    async findServicesByInterface(interfaceName: string): Promise<Service[]> {
        const response = await rsocketService.requestResponse(`devtools:services.find:${interfaceName}`);
        const result: DevToolsResponse<Service[]> = JSON.parse(response);
        if (result.success && result.data) {
            return result.data;
        }
        return [];
    }
    
    /**
     * Get metrics.
     */
    async getMetrics(): Promise<Metrics> {
        const response = await rsocketService.requestResponse("devtools:metrics");
        const result: DevToolsResponse<Metrics> = JSON.parse(response);
        if (result.success && result.data) {
            return result.data;
        }
        throw new Error(result.error || "Failed to get metrics");
    }
    
    /**
     * Subscribe to bundle events stream.
     */
    subscribeToBundles(onEvent: (event: any) => void, onError?: (error: any) => void) {
        return rsocketService.requestStream("devtools:stream:bundles", onEvent, onError);
    }
    
    /**
     * Subscribe to metrics stream.
     */
    subscribeToMetrics(onEvent: (event: any) => void, onError?: (error: any) => void) {
        return rsocketService.requestStream("devtools:stream:metrics", onEvent, onError);
    }
    
    /**
     * Get list of log files.
     */
    async getLogFiles(): Promise<any[]> {
        const response = await rsocketService.requestResponse("devtools:logs.files");
        const result: DevToolsResponse<any[]> = JSON.parse(response);
        if (result.success && result.data) {
            return result.data;
        }
        return [];
    }
    
    /**
     * Read log entries from a file.
     */
    async readLogEntries(filePath: string, maxLines: number = 100, level: string = "ALL", searchTerm?: string): Promise<any[]> {
        const search = searchTerm ? encodeURIComponent(searchTerm) : "";
        const response = await rsocketService.requestResponse(
            `devtools:logs.read:${filePath}:${maxLines}:${level}:${search}`
        );
        const result: DevToolsResponse<any[]> = JSON.parse(response);
        if (result.success && result.data) {
            return result.data;
        }
        throw new Error(result.error || "Failed to read logs");
    }
    
    /**
     * Subscribe to logs stream.
     */
    subscribeToLogs(onEvent: (event: any) => void, onError?: (error: any) => void) {
        return rsocketService.requestStream("devtools:stream:logs", onEvent, onError);
    }
}

export const devToolsService = new DevToolsService();
