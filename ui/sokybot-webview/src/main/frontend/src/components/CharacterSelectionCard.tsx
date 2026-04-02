import React, { useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import {
    Button,
    Select as SelectRoot,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    encodeSelectItemValue,
    decodeSelectItemValue,
} from '@sokybot/frontend-shared';

const characterSchema = z.object({
    selectedCharacter: z.string().min(1, 'Select a character'),
});

type CharacterValues = z.infer<typeof characterSchema>;

interface CharacterSelectionCardProps {
    initialCharacter: string;
    availableCharacters: string[];
    isOffline: boolean;
    onSubmit: (values: CharacterValues) => Promise<void>;
}

export const CharacterSelectionCard: React.FC<CharacterSelectionCardProps> = ({
    initialCharacter,
    availableCharacters,
    isOffline,
    onSubmit,
}) => {
    const form = useForm<CharacterValues>({
        resolver: zodResolver(characterSchema),
        defaultValues: { selectedCharacter: initialCharacter },
    });

    useEffect(() => {
        if (!form.formState.isDirty) {
            form.reset({ selectedCharacter: initialCharacter });
        }
    }, [initialCharacter, form]);

    return (
        <div className="bg-card text-card-foreground border border-border p-4 shadow-sm rounded-lg space-y-3">
            <div className="text-xs font-bold uppercase tracking-wider text-muted-foreground">Character List</div>
            <SelectRoot
                value={encodeSelectItemValue(form.watch('selectedCharacter') ?? '')}
                onValueChange={(v) =>
                    form.setValue('selectedCharacter', decodeSelectItemValue(v), {
                        shouldValidate: true,
                        shouldTouch: true,
                    })
                }
            >
                <SelectTrigger className="w-full h-9 rounded-md border border-input bg-background px-3 text-sm">
                    <SelectValue placeholder="Select character" />
                </SelectTrigger>
                <SelectContent>
                    <SelectItem value={encodeSelectItemValue('')}>Select character</SelectItem>
                    {availableCharacters.map((name) => (
                        <SelectItem key={name} value={encodeSelectItemValue(name)}>
                            {name}
                        </SelectItem>
                    ))}
                </SelectContent>
            </SelectRoot>
            {form.formState.errors.selectedCharacter && (
                <p className="text-[11px] text-destructive">
                    {form.formState.errors.selectedCharacter.message}
                </p>
            )}
            <Button className="w-full" disabled={isOffline} onClick={form.handleSubmit(onSubmit)}>
                Save Character
            </Button>
        </div>
    );
};
