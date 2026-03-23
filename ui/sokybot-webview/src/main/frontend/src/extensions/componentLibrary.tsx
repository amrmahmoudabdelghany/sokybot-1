import React from 'react';
import {
    Button,
    Card,
    CardHeader,
    CardTitle,
    CardContent,
    CardFooter,
    Dialog,
    DialogContent,
    DialogHeader,
    DialogTitle,
    DialogTrigger,
    DialogFooter,
    DialogDescription,
    Input,
    Label,
    Tabs,
    TabsList,
    TabsTrigger,
    TabsContent,
    Badge,
} from '@sokybot/frontend-shared';
import { Progress } from '@/components/ui/progress';
import { Slider } from '@/components/ui/slider';
import { Checkbox } from '@/components/ui/checkbox';
import {
    Stack,
    Flex,
    Box,
    Form,
    Grid,
    Separator,
    Center,
    Spacer,
    KeyValue,
    SectionTitle,
    EmptyState,
    Text,
    TextArea,
    Select,
} from './devKitComponents';
import { Alert, AlertTitle, AlertDescription } from '@sokybot/frontend-shared';
import * as LucideIcons from 'lucide-react';

/**
 * Dev kit component library – maps declarative JSON `type` values to React components.
 * Use these names in schema files for consistent, shadcn-based UI.
 */
export const UIComponentLibrary: Record<string, React.ComponentType<any>> = {
    // --- Dev kit layout primitives ---
    'Stack': Stack,
    'Flex': Flex,
    'Box': Box,
    'Form': Form,
    'Grid': Grid,
    'Separator': Separator,
    'Center': Center,
    'Spacer': Spacer,
    // --- Dev kit content ---
    'KeyValue': KeyValue,
    'SectionTitle': SectionTitle,
    'EmptyState': EmptyState,
    'Text': Text,

    // --- Layout (structure) ---
    'Card': Card,
    'CardHeader': CardHeader,
    'CardTitle': CardTitle,
    'CardContent': CardContent,
    'CardFooter': CardFooter,

    // --- Inputs ---
    'Input': Input,
    'Label': Label,
    'Checkbox': Checkbox,
    'Slider': Slider,
    'TextArea': TextArea,
    'Select': Select,

    // --- Actions ---
    'Button': Button,

    // --- Feedback ---
    'Progress': Progress,
    'Badge': Badge,
    'Alert': Alert,
    'AlertTitle': AlertTitle,
    'AlertDescription': AlertDescription,

    // --- Overlays ---
    'Dialog': Dialog,
    'DialogContent': DialogContent,
    'DialogHeader': DialogHeader,
    'DialogTitle': DialogTitle,
    'DialogTrigger': DialogTrigger,
    'DialogFooter': DialogFooter,
    'DialogDescription': DialogDescription,

    // --- Tabs ---
    'Tabs': Tabs,
    'TabsList': TabsList,
    'TabsTrigger': TabsTrigger,
    'TabsContent': TabsContent,

    // --- HTML elements (lowercase) ---
    'div': 'div' as any,
    'span': 'span' as any,
    'p': 'p' as any,
    'h1': 'h1' as any,
    'h2': 'h2' as any,
    'h3': 'h3' as any,
    'h4': 'h4' as any,
    'h5': 'h5' as any,
    'h6': 'h6' as any,
    'section': 'section' as any,
    'article': 'article' as any,
    'header': 'header' as any,
    'footer': 'footer' as any,
    'nav': 'nav' as any,
    'main': 'main' as any,
    'aside': 'aside' as any,
    'ul': 'ul' as any,
    'ol': 'ol' as any,
    'li': 'li' as any,
    'table': 'table' as any,
    'thead': 'thead' as any,
    'tbody': 'tbody' as any,
    'tr': 'tr' as any,
    'td': 'td' as any,
    'th': 'th' as any,
    'form': 'form' as any,
    'input': 'input' as any,
    'textarea': 'textarea' as any,
    'select': 'select' as any,
    'option': 'option' as any,
    'img': 'img' as any,
    'a': 'a' as any,
    'button': 'button' as any,
};

/**
 * Icon Library - Maps icon names to Lucide React icons
 */
export const IconLibrary: Record<string, any> = {
    ...LucideIcons,
};

/**
 * Get a component from the library by name
 */
export function getComponentFromLibrary(name: string): React.ComponentType<any> | undefined {
    return UIComponentLibrary[name] || IconLibrary[name];
}

/**
 * Render an icon by name
 */
export function renderIcon(iconName: string, props?: any): React.ReactElement | null {
    const Icon = IconLibrary[iconName];
    if (!Icon) {
        console.warn(`Icon not found: ${iconName}`);
        return null;
    }
    return React.createElement(Icon, props || {});
}
