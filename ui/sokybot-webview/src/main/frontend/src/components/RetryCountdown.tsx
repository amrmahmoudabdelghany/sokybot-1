import React from 'react';
import { Button } from '@sokybot/frontend-shared';
import { cn } from '@sokybot/frontend-shared';
import type { RetryCountdownState } from '../machines/useRetryCountdown';

interface RetryCountdownProps {
    countdown: RetryCountdownState;
}

/**
 * Live countdown display for RETRY_DELAY phases.
 * Shows remaining time and a "Retry now" button with boundary dedupe guard.
 */
export const RetryCountdown: React.FC<RetryCountdownProps> = ({ countdown }) => {
    if (!countdown.isActive) return null;
    const ringStateClass = countdown.isStalled ? 'opacity-70' : 'opacity-100';

    return (
        <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3 transition-all duration-200">
            <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
                Retry
            </div>

            {countdown.remainingSeconds > 0 && (
                <div className="flex items-center gap-2">
                    {/* Animated countdown ring */}
                    <div className={cn('relative h-10 w-10 flex items-center justify-center transition-all duration-200', ringStateClass)}>
                        <svg className="h-10 w-10 -rotate-90 transition-all duration-200" viewBox="0 0 36 36">
                            <circle
                                cx="18" cy="18" r="15"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                className="text-muted-foreground/20"
                            />
                            <circle
                                cx="18" cy="18" r="15"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                strokeDasharray="94.25"
                                strokeDashoffset={94.25 * (1 - Math.min(countdown.remainingSeconds / 60, 1))}
                                strokeLinecap="round"
                                className="text-primary transition-all duration-200"
                            />
                        </svg>
                        <span className="absolute text-xs font-mono font-bold text-foreground">
                            {countdown.remainingSeconds}s
                        </span>
                    </div>
                    <span className="text-[11px] text-muted-foreground leading-snug">
                        Retrying in {countdown.remainingSeconds} second{countdown.remainingSeconds !== 1 ? 's' : ''}…
                    </span>
                </div>
            )}

            {/* Stall warning */}
            {countdown.isStalled && (
                <div className={cn(
                    'text-[11px] leading-snug px-3 py-2 rounded-md border',
                    'text-amber-600/90 dark:text-amber-400/90 bg-amber-500/5 border-amber-500/20'
                )}>
                    Automatic retry may have stalled. You can retry manually.
                </div>
            )}

            <Button
                className="w-full"
                size="sm"
                disabled={!countdown.canRetryNow}
                onClick={countdown.triggerRetry}
            >
                Retry Now
            </Button>
        </div>
    );
};
