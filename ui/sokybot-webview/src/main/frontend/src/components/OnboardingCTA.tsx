import React from 'react';
import { Button, cn } from '@sokybot/frontend-shared';
import type { ActionIntent, UXPhaseModel } from '../machines/loginPhaseMapping';

/**
 * Pure predicate for unit tests and consistent disable rules.
 * Cancel stays enabled during connect in-flight; all buttons disabled while offline or aborting.
 */
export function isOnboardingCtaButtonDisabled(
    isOffline: boolean,
    inFlight: boolean,
    aborting: boolean,
    intent: NonNullable<ActionIntent>
): boolean {
    return isOffline || (inFlight && intent !== 'cancel') || aborting;
}

/**
 * Contextual action buttons derived from the UX phase model.
 *
 * Blocker priority (highest first): Gateway > Credentials > Agent > Character.
 * The primary slot shows the highest-priority blocker CTA only.
 */

interface OnboardingCTAProps {
    /** Current UX phase model. */
    ux: UXPhaseModel;
    /** Handler map for each action intent. */
    onAction: (intent: ActionIntent) => void;
    /** Connect/login RSocket work in-flight (do not fold isOffline into this). */
    inFlight?: boolean;
    /** Browser offline — disables all actions until back online. */
    isOffline?: boolean;
    /** Whether the machine is in an aborting state (cancel-in-progress). */
    aborting?: boolean;
}

const ACTION_LABELS: Record<NonNullable<ActionIntent>, string> = {
    open_gateway: 'Configure Gateway',
    focus_credentials: 'Enter Credentials',
    select_agent: 'Select Server',
    choose_character: 'Select Character',
    retry_now: 'Retry Now',
    connect_bot: 'Connect',
    cancel: 'Cancel',
    check_settings: 'Check Settings',
    acknowledge: 'Acknowledge',
};

const ACTION_VARIANTS: Partial<Record<NonNullable<ActionIntent>, 'default' | 'destructive' | 'outline' | 'secondary' | 'ghost'>> = {
    cancel: 'outline',
    check_settings: 'secondary',
    acknowledge: 'secondary',
    retry_now: 'default',
    connect_bot: 'default',
};

export const OnboardingCTA: React.FC<OnboardingCTAProps> = ({
    ux,
    onAction,
    inFlight = false,
    isOffline = false,
    aborting = false,
}) => {
    const { primaryAction, secondaryAction } = ux;

    if (!primaryAction && !secondaryAction) return null;

    const emphasizeConnect =
        ux.rawPhase === 'PENDING_MANUAL_CONNECT' && primaryAction === 'connect_bot';

    const renderButton = (intent: NonNullable<ActionIntent>, isPrimary: boolean) => {
        const disabled = isOnboardingCtaButtonDisabled(isOffline, inFlight, aborting, intent);
        const variant = isPrimary
            ? (ACTION_VARIANTS[intent] ?? 'default')
            : 'outline';

        return (
            <Button
                key={intent}
                variant={variant}
                size="sm"
                className={cn(
                    isPrimary ? 'flex-1' : 'flex-shrink-0',
                    emphasizeConnect && isPrimary && intent === 'connect_bot'
                        && 'min-h-9 text-sm font-semibold shadow-md ring-2 ring-primary/35',
                )}
                disabled={disabled}
                onClick={() => onAction(intent)}
            >
                {aborting && intent === 'cancel' ? 'Cancelling…' : ACTION_LABELS[intent]}
            </Button>
        );
    };

    return (
        <div className="flex items-center gap-2 mt-2">
            {primaryAction && renderButton(primaryAction, true)}
            {secondaryAction && renderButton(secondaryAction, false)}
        </div>
    );
};
