# Design QA

## Scope

- Viewport: 390 × 844 dp portrait phone.
- Reference: `design-reference-option-3.png`.
- Implemented render: Compose host-side screenshot generated from the production `ExperimentScreen`.
- Side-by-side evidence: `qa-reference-vs-render.png`（左：方案 3 参考图；右：真实 Compose 渲染）。

## Visual review

| Check | Result | Notes |
|---|---|---|
| Full-screen map and calm veil | Pass | Map remains legible while the UI layer has sufficient contrast. The veil opacity was increased after the first comparison. |
| Top AI status hierarchy | Pass | Stage title, three semantic checkpoints and cancellation affordance are readable and remain above scene content. |
| Prompt bubble | Pass | Does not overlap the status card or route destination. |
| Route semantics | Pass | Recommended route uses a solid green stroke; alternative/uncertain route uses a dashed neutral stroke. |
| Bottom result panel | Pass | Warning, three evidence metrics, primary CTA and secondary actions fit without clipping at 390 dp. |
| Visual task | Pass | Real photo fills the camera area; result sheet, confidence warning and actions fit without overflow. |
| Voice task | Pass | Listening state, semantic steps, cancellation and pulsing microphone remain centered and unobstructed. |
| Typography and touch targets | Pass | Core text is 14–22sp; primary buttons are at least 52dp high. |
| Safe areas | Pass | Status and navigation bar insets are applied to all core screens. |

## Functional review

- Researcher setup supports participant IDs and all six Latin square orders.
- Baseline, Semantic Motion and SAGE Full share the same task skeleton and deterministic timing.
- A environment, B1 visual and B2 voice tasks all reach a recoverable result state.
- Cancellation, restart, route switch, evidence dialog, result adoption and CSV export are wired.
- Long-pressing the top status card opens the researcher console without exposing condition labels to participants.

## Verification commands

- `testDebugUnitTest`: passed.
- `assembleDebug`: passed.
- `updateDebugScreenshotTest`: generated three production Compose references.
- `validateDebugScreenshotTest`: passed.

Screenshot regression report: `app/build/reports/screenshotTest/preview/debug/index.html`.

## Iterations made from QA

1. Restored completed semantic checkpoints in the result state so the process remains inspectable.
2. Replaced compact metric chips with three evidence cards matching the selected design.
3. Removed the extra participant action from SAGE Full to keep the intended two-action hierarchy.
4. Increased the map veil to reduce visual competition.
5. Replaced the temporary location glyph in the voice orb with the official Material microphone icon.

final result: passed
