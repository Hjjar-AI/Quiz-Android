# Remaining UI review proposals

Unfinished design options only. These are proposals, not confirmed defects or authorization to implement them. Current implementation guidance lives in the [native UI guide](docs/guides/native-ui.md); verification gates live in the [verification checklist](docs/verification/checklist.md).

## Optional follow-up

| Area | Consider only when needed |
| --- | --- |
| Repeated panels and page layouts | Consolidate titled-card wrappers, page insets and card padding where their structure and behavior actually match. Preserve collapsible form state and caller-owned actions. |
| Async, form and filter patterns | Extract shared presentation for loading/error/empty states, validation summaries and filter controls when concrete screens benefit. Keep stale content, retries, draft recovery and request ownership with the screen/controller. |
| Feedback and metadata | Add inline/card/page feedback variants, read-only status tags or a shared dropdown field when an existing screen needs them. Preserve explicit labels, live regions and selection semantics. |
| Reading and numeric roles | Consider clearer UI/reading/Markdown/numeric role mappings if rendered comparisons expose inconsistent hierarchy or formatting. Preserve reading preferences, Arabic metrics and supported font weights. |
| Comfortable density and border refinement | Compare screenshots first. An optional Comfortable mode or native border adjustment needs evidence of clipping, crowding or poor contrast; preserve current default/Compact choices and coordinate shared palette changes. |
| Theme architecture | A registry or independent palette/appearance/contrast/shape settings remains a larger feature decision. Preserve existing theme keys, saved preferences and web/PDF palette contracts; avoid speculative preference migrations. |

Common future chart, progress-size, motion and haptic needs are maintained once in the [future consolidation shortlist](docs/guides/native-ui.md#future-consolidation-shortlist).

## Verification still pending

The supplied code-13 debug build passed; rendered checks remain: Arabic/English, narrow windows, 200% text, keyboard/TalkBack, sheet actions, navigation, learning/recovery flows and installed launcher/file-explorer icons. Follow the [verification checklist](docs/verification/checklist.md#remaining-checks) rather than treating completed source edits as runtime evidence.

Builds and extensive tests require explicit user confirmation under [agent policy](Agents.md#efficient-verification). New automated test files also require approval.
