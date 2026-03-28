/**
 * Declarative dev kit layout and composite components.
 * Use these in JSON schemas instead of raw divs for consistent layout.
 */
import React from 'react';
import * as LucideIcons from 'lucide-react';
import {
    Button,
    cn,
    Textarea as ShadTextarea,
    Select as ShadSelectRoot,
    SelectContent,
    SelectItem,
    SelectTrigger,
    SelectValue,
    encodeSelectItemValue,
    decodeSelectItemValue,
} from '@sokybot/frontend-shared';

export interface StackProps extends React.HTMLAttributes<HTMLDivElement> {
    gap?: number;
    padding?: number;
}

/** Vertical stack (flex column with gap). Use instead of div + flex flex-col. */
export const Stack: React.FC<StackProps> = ({
    className,
    style,
    gap = 4,
    padding,
    children,
    ...rest
}) => (
    <div
        className={cn(
            'flex flex-col',
            gap != null && `gap-${gap}`,
            padding != null && `p-${padding}`,
            className
        )}
        style={style}
        {...rest}
    >
        {children}
    </div>
);

export interface FlexProps extends React.HTMLAttributes<HTMLDivElement> {
    gap?: number;
    padding?: number;
}

/** Horizontal flex row with gap. Use instead of div + flex flex-row. */
export const Flex: React.FC<FlexProps> = ({
    className,
    style,
    gap = 4,
    padding,
    children,
    ...rest
}) => (
    <div
        className={cn(
            'flex flex-row',
            gap != null && `gap-${gap}`,
            padding != null && `p-${padding}`,
            className
        )}
        style={style}
        {...rest}
    >
        {children}
    </div>
);

export interface BoxProps extends React.HTMLAttributes<HTMLDivElement> {
    padding?: number;
}

/** Simple wrapper (div). Use for semantic grouping without layout. */
export const Box: React.FC<BoxProps> = ({
    className,
    style,
    padding,
    children,
    ...rest
}) => (
    <div
        className={cn(padding != null && `p-${padding}`, className)}
        style={style}
        {...rest}
    >
        {children}
    </div>
);

export interface FormProps extends React.FormHTMLAttributes<HTMLFormElement> {
    padding?: number;
}

/** Form wrapper. Use props.onSubmit as action name in schema; ComponentRenderer binds it. */
export const Form: React.FC<FormProps> = ({
    className,
    style,
    padding,
    children,
    ...rest
}) => (
    <form
        className={cn(padding != null && `p-${padding}`, className)}
        style={style}
        {...rest}
    >
        {children}
    </form>
);

// --- Layout: Grid, Separator, Center, Spacer ---

export interface GridProps extends React.HTMLAttributes<HTMLDivElement> {
    cols?: number;
    colsSm?: number;
    colsMd?: number;
    colsLg?: number;
    gap?: number;
    padding?: number;
}

/** Responsive grid. Use cols and optional colsSm/colsMd/colsLg for breakpoints. */
export const Grid: React.FC<GridProps> = ({
    className,
    style,
    cols = 2,
    colsSm,
    colsMd,
    colsLg,
    gap = 4,
    padding,
    children,
    ...rest
}) => {
    const gridClass = [
        'grid',
        `grid-cols-${cols}`,
        colsSm != null && `sm:grid-cols-${colsSm}`,
        colsMd != null && `md:grid-cols-${colsMd}`,
        colsLg != null && `lg:grid-cols-${colsLg}`,
        gap != null && `gap-${gap}`,
        padding != null && `p-${padding}`,
    ].filter(Boolean).join(' ');
    return (
        <div className={cn(gridClass, className)} style={style} {...rest}>
            {children}
        </div>
    );
};

export interface CenterProps extends React.HTMLAttributes<HTMLDivElement> {
    /** When true, also center vertically (flex items-center justify-center). */
    vertical?: boolean;
}

/** Center children horizontally (and optionally vertically). */
export const Center: React.FC<CenterProps> = ({
    className,
    style,
    vertical,
    children,
    ...rest
}) => (
    <div
        className={cn(
            'flex justify-center',
            vertical ? 'items-center' : '',
            className
        )}
        style={style}
        {...rest}
    >
        {children}
    </div>
);

/** Flex spacer (grows to fill). Use inside Flex to push items apart. */
export const Spacer: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
    className,
    ...rest
}) => <div className={cn('flex-1 min-w-0', className)} {...rest} />;

// --- Content: KeyValue, SectionTitle, EmptyState, Text ---

export interface KeyValueProps extends React.HTMLAttributes<HTMLDivElement> {
    label?: React.ReactNode;
    value?: React.ReactNode;
    labelClassName?: string;
    valueClassName?: string;
}

/** Single row: label (left) + value (right). Supports template expressions in label/value. */
export const KeyValue: React.FC<KeyValueProps> = ({
    className,
    label,
    value,
    labelClassName = 'text-muted-foreground',
    valueClassName = 'text-foreground font-mono',
    children,
    ...rest
}) => {
    const resolvedValue = value !== undefined ? value : children;
    return (
        <div className={cn('flex justify-between gap-2', className)} {...rest}>
            <span className={cn('shrink-0', labelClassName)}>{label}</span>
            <span className={cn('text-right', valueClassName)}>{resolvedValue}</span>
        </div>
    );
};

export interface SectionTitleProps extends React.HTMLAttributes<HTMLHeadingElement> {
    variant?: 'default' | 'muted';
}

/** Section heading (h4) with consistent styling. */
export const SectionTitle: React.FC<SectionTitleProps> = ({
    className,
    variant = 'default',
    children,
    ...rest
}) => (
    <h4
        className={cn(
            'text-sm font-semibold uppercase tracking-wider mb-3',
            variant === 'muted' && 'text-muted-foreground',
            className
        )}
        {...rest}
    >
        {children}
    </h4>
);

export interface EmptyStateProps extends React.HTMLAttributes<HTMLDivElement> {
    message: React.ReactNode;
    icon?: string;
    action?: string;
    actionLabel?: string;
    /** Injected by ComponentRenderer when action is a string. */
    onActionClick?: () => void;
}

/** "No data" block: optional icon, message, optional action button (action = action name; ComponentRenderer binds onActionClick). */
export const EmptyState: React.FC<EmptyStateProps> = ({
    className,
    message,
    icon: iconName,
    action,
    actionLabel = 'Action',
    onActionClick,
    children,
    ...rest
}) => {
    const IconComponent = iconName ? (LucideIcons as unknown as Record<string, React.ComponentType<{ className?: string; size?: number }>>)[iconName] : null;
    const showButton = !!(action && onActionClick);
    return (
        <div className={cn('flex flex-col items-center justify-center gap-4 py-8', className)} {...rest}>
            {IconComponent && <IconComponent className="h-10 w-10 text-muted-foreground" size={40} />}
            <p className="text-center text-muted-foreground italic">{message}</p>
            {showButton && (
                <Button type="button" variant="outline" onClick={onActionClick}>
                    {actionLabel}
                </Button>
            )}
            {children}
        </div>
    );
};

export interface TextProps extends React.HTMLAttributes<HTMLSpanElement> {
    variant?: 'default' | 'muted' | 'small' | 'mono';
    as?: 'span' | 'p';
}

/** Typography wrapper with common variants. */
export const Text: React.FC<TextProps> = ({
    className,
    variant = 'default',
    as: As = 'span',
    children,
    ...rest
}) => (
    <As
        className={cn(
            variant === 'muted' && 'text-muted-foreground',
            variant === 'small' && 'text-sm text-muted-foreground',
            variant === 'mono' && 'font-mono text-foreground',
            className
        )}
        {...rest}
    >
        {children}
    </As>
);

// --- Inputs: TextArea, Select (shadcn/Radix) ---

const inputLikeClass =
    'flex w-full rounded-md border border-input bg-background px-3 py-2 text-sm ring-offset-background placeholder:text-muted-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2 disabled:cursor-not-allowed disabled:opacity-50';

export interface TextAreaProps extends React.TextareaHTMLAttributes<HTMLTextAreaElement> {
    padding?: number;
}

/** Styled textarea (shadcn); use value and onChange (action name) in schema; ComponentRenderer binds onChange. */
export const TextArea = React.forwardRef<HTMLTextAreaElement, TextAreaProps>(
    ({ className, padding, ...rest }, ref) => (
        <ShadTextarea
            ref={ref}
            className={cn('min-h-[80px] resize-y', padding != null && `p-${padding}`, className)}
            {...rest}
        />
    )
);
TextArea.displayName = 'TextArea';

export interface SelectProps
    extends Omit<React.ComponentPropsWithoutRef<'select'>, 'onChange' | 'children' | 'size'> {
    padding?: number;
    children?: React.ReactNode;
    onChange?: React.ChangeEventHandler<HTMLSelectElement>;
    placeholder?: string;
}

function collectOptionsFromChildren(
    children: React.ReactNode
): Array<{ radixValue: string; label: React.ReactNode; disabled?: boolean }> {
    const out: Array<{ radixValue: string; label: React.ReactNode; disabled?: boolean }> = [];
    React.Children.forEach(children, (child) => {
        if (!React.isValidElement(child)) return;
        if (child.type === React.Fragment) {
            const frag = child.props as { children?: React.ReactNode };
            out.push(...collectOptionsFromChildren(frag.children));
            return;
        }
        if (child.type === 'option') {
            const p = child.props as React.OptionHTMLAttributes<HTMLOptionElement> & {
                children?: React.ReactNode;
            };
            const valueStr = p.value == null ? '' : String(p.value);
            out.push({
                radixValue: encodeSelectItemValue(valueStr),
                label: p.children,
                disabled: Boolean(p.disabled),
            });
        }
    });
    return out;
}

/**
 * Radix/shadcn select. JSON `option` children are mapped to SelectItem.
 * Empty string values use an internal sentinel so Radix constraints are satisfied.
 * `onChange` receives a synthetic event with `target.value` (decoded) for ComponentRenderer.
 */
export const Select: React.FC<SelectProps> = ({
    className,
    padding,
    children,
    value,
    defaultValue,
    onChange,
    disabled,
    id,
    name,
    placeholder = 'Select…',
    style,
}) => {
    const options = React.useMemo(() => collectOptionsFromChildren(children), [children]);
    const strValue = value !== undefined && value !== null ? String(value) : undefined;
    const encodedValue = strValue !== undefined ? encodeSelectItemValue(strValue) : undefined;
    const strDefault = defaultValue !== undefined && defaultValue !== null ? String(defaultValue) : undefined;
    const encodedDefault = strDefault !== undefined ? encodeSelectItemValue(strDefault) : undefined;

    const handleValueChange = (radixVal: string) => {
        const decoded = decodeSelectItemValue(radixVal);
        const synthetic = {
            target: { value: decoded, name: name ?? '' },
            currentTarget: { value: decoded, name: name ?? '' },
        } as React.ChangeEvent<HTMLSelectElement>;
        onChange?.(synthetic);
    };

    return (
        <ShadSelectRoot
            value={encodedValue}
            defaultValue={encodedDefault}
            onValueChange={handleValueChange}
            disabled={disabled}
            name={name}
        >
            <SelectTrigger
                id={id}
                style={style}
                className={cn(
                    inputLikeClass,
                    'cursor-pointer',
                    padding != null && `p-${padding}`,
                    className
                )}
            >
                <SelectValue placeholder={placeholder} />
            </SelectTrigger>
            <SelectContent>
                {options.map((opt, i) => (
                    <SelectItem key={`${opt.radixValue}-${i}`} value={opt.radixValue} disabled={opt.disabled}>
                        {opt.label}
                    </SelectItem>
                ))}
            </SelectContent>
        </ShadSelectRoot>
    );
};
