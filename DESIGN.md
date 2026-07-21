# Design

## Foundation

RecruitAgent is a Vue 3 + TypeScript + Element Plus product UI. The visual system should preserve the existing restrained OKLCH token approach while tightening hierarchy, density, and state clarity.

## Color

Use a restrained product palette:

- Background: `--color-bg`
- Raised surfaces: `--color-surface`, `--color-surface-raised`
- Borders: `--color-border`, `--color-border-strong`
- Text: `--color-text-primary`, `--color-text-regular`, `--color-text-secondary`, `--color-text-muted`
- Accent: `--color-accent` for primary actions, active navigation, and selected controls
- Semantic state: success, warning, danger backgrounds and foregrounds for screening results, task status, and review state

Color should not be used as decoration. Every colored element must communicate action, selection, or state.

## Typography

Use one system sans stack across UI. Keep type sizes fixed and compact:

- Page title: 1.25rem, 600
- Section title: 1rem, 600
- Table/body: 0.9rem to 1rem
- Metadata and helper text: 0.8rem to 0.85rem

Avoid display typography and fluid heading scales in product surfaces.

## Layout

Use a classic operations shell: sidebar navigation, top header, content viewport. Pages should use predictable grids and tables. Related controls belong in tight groups; separate workflow sections with clear whitespace.

Spacing follows a 4pt rhythm: 4, 8, 12, 16, 20, 24, 32. Use cards for distinct tools, repeated items, and panels only. Do not nest cards.

## Components

Primary actions use Element Plus primary buttons with icons when the command benefits from recognition. Tables should expose the main object name, state, score or progress, and one clear next action. Empty states should tell users how to create data or continue the workflow.

## Interaction

Every async action needs loading feedback and a success or error message. Long-running backend work should use Agent task progress instead of front-end loops. Destructive actions use confirmation when deleting persisted recruiting data.

## Motion

Motion should be minimal, 150-250ms, and state-driven. No decorative page-load choreography.
