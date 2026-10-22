# Android UI review

This is a compact disposition of validated findings, not a running history. Reusable feature/source guidance is in the [native UI guide](../guides/native-ui.md); actual evidence and unresolved verification are in the [checklist](checklist.md).

## Review validation and corrections

| Area | Current source behavior |
| --- | --- |
| Quiz timer and lifecycle | Separate clock flow collected by chrome; deadline guards and encrypted local-finish snapshots retain ViewModel ownership. UI flow collection uses the installed lifecycle runtime. Confidence/reflection leaves take values/callbacks. |
| Fields and passwords | Shared String/TextFieldValue overloads preserve Markdown cursor/composition, validation, multiline Enter and caller keyboard actions. Single-line fields default to Next; passwords to Done with form overrides. LTR/password keyboards, memory-only reveal/reset and error icons are shared. |
| Accessibility | Ordinary/master/offline/reading-preview answer groups and confidence/reflection groups are marked. Actual headings and four reported click roles are corrected; timer ticks are not live regions. |
| Fonts | Static 400-only Amiri/Tajawal/IBM Plex use real regular weight without synthesis. Variable Noto/Cairo/Inter/Outfit declare 400/500/600/700 on API 26+; Markdown selects 400. Android 6/7 variable selections fall back to bundled Amiri. Outfit's bundled default was 100. |
| Results | Practice/filter/category/breakdown derivations and counts are cached. Titles/numbers/difficulty/reflection labels are localized. Wide list/reader have bounded independent scroll; selected-row change resets reader scroll. Frozen/withheld review gates remain. |
| Choice/status rendering | ChoiceVisualState normalizes presentation flags; HSV border work is remembered. One mapping drives sync kind/detail. Inline info/success/warning/error messages have icons and polite/error semantics; existing recovery actions retain guards. |
| Contrast and controls | Native derived feedback/error ink targets 4.5:1 on checked surfaces; unselected question/choice borders target 3:1. Clickable cards use stronger outlines; shared progress bars clamp finite ranges and associate admin labels. Base palette seeds/keys and web/PDF exports remain unchanged. |
| Home and notebooks | One primary-action model; secondary counts/links below hero, with recovery/Retry retained. Mistakes/Fragile share presentation while keeping request cancellation, account-scoped pages and practice ownership. |
| Hygiene and layout | Eighteen component dimensions moved to private geometry; duplicate spacing aliases/four unused wrappers removed; explicit shared imports. Metadata/labels have a 13sp floor, real headings are marked, dialogs use large shape, horizontal action gaps widen one step. Final question-map review uses a bounded sheet. |
| Motion and previews | One system-motion helper and duration tokens retain existing transitions. Shared previews include new fields/status/progress under light/dark, Arabic RTL and 200% text. |

## Claims corrected or not established

Material progress bars already provided range semantics; Markdown already avoided reparsing unchanged content and used a regular native Typeface. Missing stability annotations, route length, mixed icons or absence of skeletons/extra transitions do not prove defects. Generic AppSurface/AppStat/AppIcons layers, wholesale route rewrites and new collection/compiler/lifecycle dependencies were not justified by these findings. Reading-comfort settings target reading content; metadata also follows system text scaling.

These are source corrections. No frame-rate, rendered contrast, TalkBack, font/device or concurrency outcome is inferred. The [verification checklist](checklist.md) owns remaining checks and current evidence; shrink this disposition as details become established guide content instead of appending history.
