import React from 'react';
import { cn } from '@sokybot/frontend-shared';
import { STEP_LABELS } from '../machines/loginPhaseMapping';

/**
 * 4-step progress tracker for the login onboarding flow.
 *
 * Steps:
 *   0 – Connect gateway
 *   1 – Discover server list
 *   2 – Sign in
 *   3 – Enter game
 */

interface OnboardingStepperProps {
    /** 0-based index of the current step. */
    currentStep: number;
    /** 0-based indices of completed steps. */
    completedSteps: number[];
    /** 0-based indices of blocked steps. */
    blockedSteps: number[];
}

export const OnboardingStepper: React.FC<OnboardingStepperProps> = ({
    currentStep,
    completedSteps,
    blockedSteps,
}) => {
    return (
        <ol className="flex items-center gap-0 list-none p-0 m-0" aria-label="Login progress">
            {STEP_LABELS.map((label, i) => {
                const isCompleted = completedSteps.includes(i);
                const isCurrent = i === currentStep;
                const isBlocked = blockedSteps.includes(i);

                // Determine dot / line colour
                let dotClass = 'bg-muted-foreground/30 border-muted-foreground/20';
                let lineClass = 'bg-muted-foreground/20';
                let textClass = 'text-muted-foreground/50';

                if (isBlocked) {
                    dotClass = 'bg-red-500/20 border-red-500/40';
                    textClass = 'text-red-400/80';
                } else if (isCompleted) {
                    dotClass = 'bg-emerald-500 border-emerald-400';
                    lineClass = 'bg-emerald-500/60';
                    textClass = 'text-emerald-400';
                } else if (isCurrent) {
                    dotClass = 'bg-primary border-primary/80 ring-2 ring-primary/30';
                    textClass = 'text-foreground font-semibold';
                }

                return (
                    <React.Fragment key={i}>
                        {/* Connector line (not before first step) */}
                        {i > 0 && (
                            <div className={cn('flex-1 h-[2px] rounded-full min-w-2', lineClass)} />
                        )}

                        {/* Step indicator */}
                        <li
                            className="flex flex-col items-center gap-1 min-w-0"
                            aria-current={isCurrent ? 'step' : undefined}
                            aria-disabled={!isCurrent && !isCompleted}
                        >
                            <div
                                className={cn(
                                    'h-2.5 w-2.5 rounded-full border transition-all duration-200',
                                    dotClass,
                                )}
                            />
                            <span className={cn(
                                'text-[9px] leading-tight text-center whitespace-nowrap',
                                textClass,
                            )}>
                                {label}
                            </span>
                        </li>
                    </React.Fragment>
                );
            })}
        </ol>
    );
};
