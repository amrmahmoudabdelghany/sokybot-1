# Declarative UI Dev Kit

This document describes the JSON-based declarative UI system and the component dev kit built on shadcn/ui. Use it to build and maintain extension pages by editing JSON schemas only.

## Schema shape

Every node in a declarative schema is a **UIComponent** with this shape:

| Field       | Type                    | Required | Description |
|------------|--------------------------|----------|-------------|
| `type`     | `string`                 | Yes      | Component key: dev kit primitive (e.g. `Stack`, `Button`), shadcn name (`Card`, `Dialog`), HTML tag (`div`, `span`), or special type (`layout`, `table`, `hex-viewer`). |
| `props`    | `Record<string, any>`    | No       | Props passed to the component. Strings can contain `${...}` expressions. |
| `children` | `UIComponent[]` or `string` | No  | Nested components or text. |
| `className`| `string`                 | No       | Tailwind (or other) CSS classes. Supports `${...}`. |
| `style`    | `Record<string, any>`    | No       | Inline styles. Values support `${...}`. |
| `key`      | `string`                 | No       | React key when in a list. |
| `icon`     | `string`                 | No       | Lucide icon name; rendered before children. |
| `iconProps`| `Record<string, any>`    | No       | Props for the icon (e.g. `size`). |
| `variant`  | `string`                 | No       | Shadcn variant (e.g. `default`, `outline`, `ghost`). |
| `size`     | `string`                 | No       | Shadcn size (e.g. `sm`, `lg`). |

- **PascalCase** types (`Button`, `Card`, `Stack`) map to React/shadcn components.
- **lowercase** types (`div`, `span`, `p`) map to HTML elements or special declarative types (`layout`, `table`, `hex-viewer`).

## Expression and data binding

### Template expressions

Any **string** prop (including `className`, `children`, `hidden`, `open`, `disabled`) can contain **`${...}`** expressions. The content inside the braces is evaluated against the current **context** (page state).

- Simple path: `"${trafficPackets.length}"`, `"${settings.username}"`
- Ternary: `"${connected ? 'Connected' : 'Disconnected'}"`
- Comparisons: `"${activeTab === 'traffic'}"`
- Function calls: `"${(currentHP / maxHP) * 100}"`, `"${item.name}"` (in `renderItem`)

Unresolvable or undefined values default to `0` for count-like keys and `''` otherwise, so the UI does not show literal `"${...}"`.

### Boolean coercion

For `hidden`, `disabled`, and `open`:

- Resolved value can be string or boolean.
- Strings `"true"` / `"false"` and expression results are coerced to boolean.

### Actions

Event props hold **action names** (strings). The host view calls the backend with that action and optional payload:

- **`onClick`**: action name; use **`actionData`** in `props` for payload.
- **`onChange`**: action name; handler receives `{ value, ...actionData }`.
- **`onBlur`**: same as `onChange`.
- **`onSubmit`**: action name (e.g. for `Form`); handler receives `actionData`.
- **`onOpenChange`** (Dialog): when closed, the given action is called.

Example:

```json
{
  "type": "Button",
  "variant": "default",
  "props": {
    "onClick": "saveSettings",
    "actionData": { "section": "connection" },
    "children": "Save"
  }
}
```

## Component catalog

### Dev kit layout primitives

Use these instead of raw `div` for layout so schemas stay consistent.

| Type    | Description              | Props (in addition to common) |
|---------|--------------------------|-------------------------------|
| **Stack** | Vertical stack (flex column + gap) | `gap?: number`, `padding?: number` |
| **Flex**  | Horizontal row + gap     | `gap?: number`, `padding?: number` |
| **Box**   | Wrapper div (semantic grouping) | `padding?: number` |
| **Form**  | Form element; `onSubmit` in props = action name | `padding?: number` |

Example:

```json
{
  "type": "Stack",
  "props": { "gap": 4 },
  "className": "p-4",
  "children": [
    { "type": "Label", "props": { "children": "Name" } },
    { "type": "Input", "props": { "name": "name" } }
  ]
}
```

### Layout (built-in)

| Type     | Description        | Props |
|----------|--------------------|-------|
| **layout** | Column/row/grid/stack | `layout?: "column" \| "row" \| "grid" \| "stack"`, `gap?: number`, `padding?: number`, `cols?: number` (for grid) |

### Structure and typography

- **Card**, **CardHeader**, **CardTitle**, **CardContent**, **CardFooter** – shadcn Card.
- **div**, **span**, **p**, **h1**–**h6**, **section**, **header**, **footer**, **nav**, **main**, **aside**, **ul**, **ol**, **li** – HTML elements.

### Inputs

- **Input**, **Label**, **Checkbox**, **Slider** – shadcn; use **`onChange`** / **`onCheckedChange`** / **`onValueChange`** as action names.
- **TextArea**, **Select** – dev kit styled native elements; **`value`**, **`onChange`** (action name).
- **select**, **option**, **textarea** – HTML; **`onChange`** as action name.

### Actions and feedback

- **Button** – variants: `default`, `outline`, `ghost`; sizes: `sm`, `lg`; **`onClick`** = action name.
- **Progress**, **Badge**, **Alert**, **AlertTitle**, **AlertDescription** – shadcn (Alert: **`variant?: "default" \| "destructive"`**).

### Overlays

- **Dialog**, **DialogContent**, **DialogHeader**, **DialogTitle**, **DialogTrigger**, **DialogFooter**, **DialogDescription** – shadcn Dialog. Use **`open`** (expression or boolean) and **`onOpenChange`** (action name when closing).

### Tabs

- **Tabs**, **TabsList**, **TabsTrigger**, **TabsContent** – shadcn Tabs.

### Data and special

- **table** (lowercase) – declarative table: **`dataSource`** (context key or expression), **`columns`** (array of `{ field, label, className?, ... }`), **`onRowClick`** (action name), **`rowClassName`**.
- **hex-viewer** – packet hex viewer: **`dataSource`**, **`onSelectHex`**, **`onDefineVariable`** (action names).
- **icon** – Lucide icon by **`name`** (in props); **`size`** optional.

### Lists with dataSource + renderItem

Any component type can use:

- **`dataSource`**: context key or expression that evaluates to an array.
- **`renderItem`**: a **UIComponent** subtree (single object). It is rendered once per item with context extended by **`item`** and **`index`**.

The rendered list is passed as **children** to the component (e.g. a `div` or `Stack`).

## Example snippets

### Stack and Flex

```json
{
  "type": "Flex",
  "className": "justify-between items-center border-b pb-2",
  "props": { "gap": 2 },
  "children": [
    { "type": "h3", "props": { "children": "Title" } },
    {
      "type": "Flex",
      "props": { "gap": 2 },
      "children": [
        { "type": "Button", "variant": "outline", "size": "sm", "props": { "onClick": "refresh", "children": "Refresh" } },
        { "type": "Button", "variant": "default", "size": "sm", "props": { "onClick": "save", "children": "Save" } }
      ]
    }
  ]
}
```

### Form with submit action

```json
{
  "type": "Form",
  "props": { "onSubmit": "submitExampleForm", "actionData": {} },
  "children": [
    {
      "type": "Stack",
      "props": { "gap": 4 },
      "children": [
        { "type": "Label", "props": { "htmlFor": "name", "children": "Name" } },
        { "type": "Input", "props": { "id": "name", "name": "name" } },
        { "type": "Button", "variant": "default", "props": { "type": "submit", "children": "Submit" } }
      ]
    }
  ]
}
```

### Dialog with open from state

```json
{
  "type": "Dialog",
  "props": {
    "open": "${analyzerOpen}",
    "onOpenChange": "closeAnalyzer"
  },
  "children": [
    {
      "type": "DialogContent",
      "children": [
        { "type": "DialogHeader", "children": [{ "type": "DialogTitle", "props": { "children": "Title" } }] },
        { "type": "Button", "variant": "outline", "props": { "onClick": "closeAnalyzer", "children": "Close" } }
      ]
    }
  ]
}
```

### Conditional visibility

```json
{
  "type": "Box",
  "props": { "id": "trafficContent", "hidden": "${activeTab !== 'traffic'}" },
  "children": [{ "$ref": "traffic-monitor.json" }]
}
```

### Grid

```json
{
  "type": "Grid",
  "props": { "cols": 1, "colsMd": 2, "gap": 6 },
  "children": [
    { "type": "Card", "children": [...] },
    { "type": "Card", "children": [...] }
  ]
}
```

### KeyValue and SectionTitle

```json
{
  "type": "Stack",
  "props": { "gap": 2 },
  "children": [
    { "type": "SectionTitle", "props": { "children": "Active Area" } },
    { "type": "KeyValue", "props": { "label": "Name", "value": "${activeArea.name}" } },
    { "type": "KeyValue", "props": { "label": "Center (X, Y)", "value": "${activeArea.x}, ${activeArea.y}" } }
  ]
}
```

### EmptyState

```json
{
  "type": "EmptyState",
  "props": {
    "message": "No skills learnt",
    "icon": "BookOpen",
    "action": "refresh",
    "actionLabel": "Refresh"
  }
}
```

### Text and Separator

```json
{
  "type": "Stack",
  "props": { "gap": 2 },
  "children": [
    { "type": "Text", "variant": "muted", "props": { "children": "Hint text" } },
    { "type": "Separator", "props": { "orientation": "horizontal" } },
    { "type": "Text", "variant": "mono", "props": { "children": "${someValue}" } }
  ]
}
```

## Migration from raw div/span

- Replace layout-only **`div`** (e.g. `flex`, `flex-col`, `gap-*`) with **Stack** (column) or **Flex** (row), and set **`props.gap`** (and optional **`props.padding`**) instead of only `className` where it fits.
- Replace wrapper/grouping **`div`** (no semantic layout) with **Box**.
- Keep **`span`** for inline text; keep **`div`** when you need a specific Tailwind grid or one-off layout (e.g. `grid grid-cols-1 lg:grid-cols-2`) and pass that in **`className`** on **Box** or **Stack**/ **Flex**.
- Keep **Button**, **Card**, **Input**, **Dialog**, etc. as-is; ensure **onClick** / **onChange** / **onOpenChange** / **onSubmit** are action names and **actionData** is used where needed.

## File locations

- **Types**: `extensions/ui-types.ts` (`UIComponent`, etc.)
- **Component library**: `extensions/componentLibrary.tsx` (`UIComponentLibrary`, `getComponentFromLibrary`, `renderIcon`)
- **Dev kit primitives**: `extensions/devKitComponents.tsx` (`Stack`, `Flex`, `Box`, `Form`, `Grid`, `Separator`, `Center`, `Spacer`, `KeyValue`, `SectionTitle`, `EmptyState`, `Text`)
- **Renderer**: `extensions/renderer/ComponentRenderer.tsx`
- **Expressions**: `extensions/renderer/expressionUtils.ts` (`resolveTemplate`, `safeEval`, `resolveTemplateInObject`)
- **Example schemas**: `scripts/pages/*.json`, `src/main/resources/ui/example-page.json`, `example-form.json`, `example-tabs.json`
