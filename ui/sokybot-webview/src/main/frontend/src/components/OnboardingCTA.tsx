import React from 'react';
import { Button } from '@sokybot/frontend-shared';
import type { ActionIntent, UXPhaseModel } from '../machines/loginPhaseMapping';

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
    /** Whether a connect/login operation is in-flight. */
    inFlight?: boolean;
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
    aborting = false,
}) => {
    const { primaryAction, secondaryAction } = ux;

    if (!primaryAction && !secondaryAction) return null;

    const renderButton = (intent: NonNullable<ActionIntent>, isPrimary: boolean) => {
        const disabled = inFlight || (aborting && intent !== 'cancel');
        const variant = isPrimary
            ? (ACTION_VARIANTS[intent] ?? 'default')
            : 'outline';

        return (
            <Button
                key={intent}
                variant={variant}
                size="sm"
                className={isPrimary ? 'flex-1' : 'flex-shrink-0'}
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
