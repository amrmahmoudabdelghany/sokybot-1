package org.sokybot.devtools;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.RuntimeMXBean;
import java.lang.management.ThreadMXBean;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for collecting runtime metrics.
 */
public class RuntimeMetricsService {
    
    private final MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    private final ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    private final RuntimeMXBean runtimeBean = ManagementFactory.getRuntimeMXBean();
    
    /**
     * Get memory metrics.
     */
    public Map<String, Object> getMemoryMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryBean.getNonHeapMemoryUsage();
        
        metrics.put("heapUsed", heap.getUsed());
        metrics.put("heapMax", heap.getMax());
        metrics.put("heapCommitted", heap.getCommitted());
        
        if (heap.getMax() > 0) {
            double heapPercent = ((double) heap.getUsed() / heap.getMax()) * 100.0;
            metrics.put("heapPercent", heapPercent);
        } else {
            metrics.put("heapPercent", 0.0);
        }
        
        metrics.put("nonHeapUsed", nonHeap.getUsed());
        metrics.put("nonHeapMax", nonHeap.getMax() > 0 ? nonHeap.getMax() : -1);
        metrics.put("nonHeapCommitted", nonHeap.getCommitted());
        
        return metrics;
    }
    
    /**
     * Get thread metrics.
     */
    public Map<String, Object> getThreadMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        metrics.put("threadCount", threadBean.getThreadCount());
        metrics.put("peakThreadCount", threadBean.getPeakThreadCount());
        metrics.put("daemonThreadCount", threadBean.getDaemonThreadCount());
        metrics.put("totalStartedThreadCount", threadBean.getTotalStartedThreadCount());
        
        return metrics;
    }
    
    /**
     * Get runtime info.
     */
    public Map<String, Object> getRuntimeInfo() {
        Map<String, Object> info = new HashMap<>();
        
        Runtime runtime = Runtime.getRuntime();
        info.put("availableProcessors", runtime.availableProcessors());
        info.put("uptime", runtimeBean.getUptime());
        info.put("startTime", runtimeBean.getStartTime());
        
        return info;
    }
    
    /**
     * Get all metrics combined.
     */
    public Map<String, Object> getAllMetrics() {
        Map<String, Object> all = new HashMap<>();
        all.put("memory", getMemoryMetrics());
        all.put("threads", getThreadMetrics());
        all.put("runtime", getRuntimeInfo());
        all.put("timestamp", System.currentTimeMillis());
        return all;
    }
}
