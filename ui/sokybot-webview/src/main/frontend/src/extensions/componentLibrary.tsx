import React from 'react';
import { Button } from '@sokybot/frontend-shared';
import { Card, CardHeader, CardTitle, CardContent, CardFooter } from '@sokybot/frontend-shared';
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@sokybot/frontend-shared';
import { Input } from '@sokybot/frontend-shared';
import { Label } from '@sokybot/frontend-shared';
import { Progress } from '@/components/ui/progress';
import * as LucideIcons from 'lucide-react';

/**
 * Component Library - Maps component names to React components
 * Supports shadcn/ui components, HTML elements, and Lucide icons
 */
export const UIComponentLibrary: Record<string, React.ComponentType<any>> = {
    // shadcn/ui components
    'Button': Button,
    'Card': Card,
    'CardHeader': CardHeader,
    'CardTitle': CardTitle,
    'CardContent': CardContent,
    'CardFooter': CardFooter,
    'Dialog': Dialog,
    'DialogContent': DialogContent,
    'DialogHeader': DialogHeader,
    'DialogTitle': DialogTitle,
    'DialogTrigger': DialogTrigger,
    'Input': Input,
    'Label': Label,
    'Progress': Progress,

    // HTML elements (as string tags)
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
