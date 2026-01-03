# Nuxt UI Component Guide

This project uses **Nuxt UI v3/v4** (compatible with Tailwind CSS).
Documentation source: [Nuxt UI](https://ui.nuxt.com) and Context7.

## Key Components

### Forms
The project uses `UForm` with Zod schema validation.
**Important**: All form inputs must have a `name` attribute matching the Zod schema key for validation errors to map correctly.

```vue
<template>
  <UForm :schema="schema" :state="state" @submit="onSubmit">
    <UFormField label="Username" name="username">
      <UInput v-model="state.username" name="username" />
    </UFormField>
    <UButton type="submit">Submit</UButton>
  </UForm>
</template>
```

### Inputs
- `UInput`: Standard text input.
- `UInputNumber`: Numeric input.
- `UInputTags`: Tags input.
- `USelectMenu`: Select dropdown.

### Validation
We use **Zod** for schema validation.
Errors are displayed automatically by `UFormField` if the `name` attribute matches the schema field.

## Testing
Playwright E2E tests rely on:
- `input[name="fieldname"]` selectors.
- Button roles: `getByRole('button', { name: /Label/i })`.
- Validation error text visibility.

## Reference
- [Nuxt UI Docs](https://ui.nuxt.com)
- [Tailwind CSS](https://tailwindcss.com)
