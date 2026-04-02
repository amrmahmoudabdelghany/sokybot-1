import React, { useState } from 'react';
import { Button } from '@sokybot/frontend-shared';

/**
 * In-panel challenge input for interactive blocker intents
 * (e.g., captcha/passcode challenges during MANUAL_VERIFICATION_REQUIRED).
 */

interface ChallengeInputProps {
    /** Challenge type label (e.g., "Captcha", "Security Code"). */
    challengeType: string;
    /** Placeholder text for the input. */
    placeholder?: string;
    /** Maximum length for the input value. */
    maxLength?: number;
    /** Whether the challenge has timed out / expired. */
    expired?: boolean;
    /** Submit handler. */
    onSubmit: (value: string) => void;
    /** Cancel handler. */
    onCancel: () => void;
}

export const ChallengeInput: React.FC<ChallengeInputProps> = ({
    challengeType,
    placeholder = 'Enter response…',
    maxLength = 64,
    expired = false,
    onSubmit,
    onCancel,
}) => {
    const [value, setValue] = useState('');
    const isValid = value.trim().length > 0;

    return (
        <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3">
            <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">
                {challengeType}
            </div>

            {expired ? (
                <div className="text-[11px] text-amber-600/90 dark:text-amber-400/90 leading-snug">
                    This challenge has expired. Please retry to receive a new one.
                </div>
            ) : (
                <>
                    <input
                        className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm"
                        placeholder={placeholder}
                        maxLength={maxLength}
                        value={value}
                        onChange={(e) => setValue(e.target.value)}
                        onKeyDown={(e) => {
                            if (e.key === 'Enter' && isValid) {
                                onSubmit(value.trim());
                            }
                        }}
                        autoFocus
                    />
                    {!isValid && value.length > 0 && (
                        <p className="text-[11px] text-destructive">Response cannot be empty</p>
                    )}
                </>
            )}

            <div className="flex items-center gap-2">
                <Button
                    className="flex-1"
                    size="sm"
                    disabled={!isValid || expired}
                    onClick={() => onSubmit(value.trim())}
                >
                    Submit
                </Button>
                <Button
                    variant="outline"
                    size="sm"
                    onClick={onCancel}
                >
                    Cancel
                </Button>
            </div>
        </div>
    );
};
