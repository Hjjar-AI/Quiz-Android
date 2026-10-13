# Review of the posted Android audits

Reviewed against current production source on 2026-10-10, including both attached pasted documents and the additional color, RTL, text-spacing, navigation and question-language claims. The attachments describe a source archive, so their historical file/line numbers and counts are not current runtime evidence. Current Android source has 161 Kotlin files, 111 under `ui/`, and 1,416 matched English/Arabic strings after this change. No Android build, automated suite, installation or device rendering was performed.

## Layout and shared components

| Posted claim | Current finding and disposition |
| --- | --- |
| Buttons/cards/shapes are centralized, but padding/gaps drift | Confirmed. Added named standard/dense card padding and content/tight/section spacing through `AppTheme.spacing` and `AppLayoutTokens`. Migrated card bodies and 5/6/8dp content gaps; kept deliberate media sizes, minimum targets and compact library dimensions. Counts such as 52 card sites and 103 literal gaps belonged to the archive and are not proof of a bug at every literal. |
| Section header 18dp/body 16dp misalignment | Confirmed and fixed: both use standard card padding (16dp normal, 12dp compact). Header/body text remains expandable and draft state remains composed when collapsed. |
| Home tiles subtract 12dp but use an 8dp row gap | Confirmed and fixed: tile width uses the same actual content-gap token as the wrapping row, including compact mode. |
| Compact only changes inter-section spacing | Confirmed and fixed for the standardized roles: section 16/10dp, standard card 16/12dp, dense card 12/10dp, content gap 8/6dp, library card 10/8dp. Touch targets remain at least 48dp. |
| Bookmarks/Mistakes/Fragile duplicate one card-body layout | Confirmed and consolidated into `AppCardBody`. History's expandable history record is a different job; its padding/gap now use the dense-card tokens rather than forcing it into a question-list card. |
| Seven toggle rows/four tappable rows vary | Confirmed. Added `AppToggleRow`/`AppListRow`; migrated the audited labeled toggle rows, dashboard link, profile action row and drawer section row. Caller-owned toggle callbacks, roles, enabled states and selections remain intact. Profile administration rows share the same named padding. |
| Status banners have 8dp gutters against a 20dp page | Confirmed and fixed with page-horizontal/content-vertical padding. |
| Quiz header/card interior start at different offsets | Confirmed and fixed with common card padding. The 12dp quiz/library outer gutter is an intentional reading layout, not a requirement to make every page 20dp. |
| Drawer header/theme control edges differ | Source geometry confirmed; theme/toggle/section gutters now use shared card padding. Exact dropdown/button text alignment still needs device inspection. |
| Profile adds a trailing 24dp spacer on top of page padding | Confirmed and removed. |
| Empty/error states add a second 24dp horizontal inset | Confirmed and removed from the shared states; standalone question-detail error supplies its own page gutter. Narrow/large-text and full-screen error layouts remain device checks. |
| Repeated page gutters/borders/48dp minima | Replaced the audited hardcoded page/auth gutters with named tokens, consolidated card border creation and removed duplicate minimum heights on the audited already-wrapped controls. Not every 48dp literal is redundant: bespoke selectable/tappable controls still need minima. |
| Filled/outlined padding differs; selected answer reflows | Confirmed. Filled/outlined wrappers share horizontal padding; selectable answers reserve the same check-icon slot in either state. Quiz/preview option numbers are separate localized elements. |
| Heading subtitle gaps differ by branch | Confirmed; heading text uses the shared tight gap. Distinct action-to-heading gaps remain deliberate. |
| Auth spinners use height rather than size | Confirmed; both use square 20dp sizes. Login's separate 20dp spacer remains a spacer. |
| 360dp is reused for unrelated purposes; pickers use 420/460dp | Added distinct image/menu/heading tokens and a common 420dp picker cap. Equal numerical defaults do not imply the same semantic token. |
| Auth pages should be 20dp or have named 24dp padding | Design choice resolved by naming and retaining 24dp auth padding; server setup uses the shared section rhythm. |
| Material You has medium 26dp and large 22dp | True, but not a demonstrated defect: the button/card shape treatment is theme-specific. Preserved it rather than silently redesigning this theme. |
| Literal-spacing CI ban; normalize all typography/dialogs/icons/motion | Proposals and coverage gaps, not established defects. No indiscriminate literal ban, title-style replacement or dialog redesign was added. Heading/body/label styles serve different hierarchy levels; profile emphasis is intentional. Actual wrapping, dialog buttons, motion and system insets still need runtime review. |

## Colors

| Posted claim | Current finding and disposition |
| --- | --- |
| Named themes inherit Stone's accent containers and surface tint | Confirmed. All ten named themes now derive primary/secondary/tertiary/error containers and paired text roles from their own palette; tint follows primary. Neutral/inverse/outline-variant roles are coherent with the selected surface. This changes inherited accent appearance, including the selected navigation pill. |
| Every used text/background pair passes 4.5:1 | Rejected as a blanket claim. Apprentice rank used outline as text: Food Delivery was 4.468:1 against its surface, Material You Light 3.961:1. Changed that rank text to `onSurfaceVariant`. Outline remains usable for decorative borders. |
| Admin error tint resembles Authority primary in red themes | Source roles and similar colors confirmed. Administrator rank now uses neutral foreground rather than the error role; labels remain explicit. The stated ΔE values were not accepted without their measurement method. Primary colors of the selected themes were retained. |
| Food tertiary resembles success; feedback is fixed across dark palettes; warning/info are unused | True palette/design observations, not defects when labels/roles remain explicit and foreground contrast is adequate. Kept the shared semantic feedback registry and Food Delivery's palette. No need to use every defined role or fill every hue band. |
| Navigation-bar dark color is hardcoded | Confirmed and fixed. Edge-to-edge bar setup reads the resolved Material surface inside the theme. Gesture/three-button navigation still needs device verification. |
| Weather gradient was not checked | Current authenticated scaffold applies `LocalAppBackground` and uses transparent container color for the gradient. Startup surfaces intentionally use the scheme background. |
| Launcher art cannot be checked; teal branding differs from themes | Archive limitation, not the current repository: launcher artwork/resources exist. Kept the user's Gemini icon/diacritic and fixed branding; one launcher need not recolor itself to match every theme. |

Source numerical check after changes: 17 resolved palettes × 17 declared text/background pairs = 289 ratios, minimum 5.603:1. This is a check of opaque source pairs, not certification of every rendered disabled/alpha/span/image state or accessibility.

## Arabic, RTL and native typography

| Posted claim | Current finding and disposition |
| --- | --- |
| Mixed names/usernames/IDs lack bidi isolation | Confirmed and fixed at the cited account/content/permissions labels, with `BidiFormatter` fragment isolation and localized display IDs. Protocol IDs remain unchanged. |
| Machine-like fields do not force LTR | Confirmed. URL, credentials, numeric-ID/count/day fields use shared LTR input text style; prose, names and Arabic searches retain content direction. Keyboard/value validation and draft policies remain with their owners. |
| Canvas charts and calendar Rows disagree on chronology direction | Confirmed and fixed: chart positions follow logical layout direction like the calendar. Data order remains unchanged. |
| Choice/setup/default-setting counts use raw digits | Confirmed and fixed at cited paths. Existing resource formatting and protocol input values are not rewritten as localized wire data. |
| Role keys appear raw while permissions map roles | Confirmed and fixed via a shared bilingual role registry; unknown/custom roles can retain server labels. |
| Back/chevron mirroring is manual and implemented differently | Manual mirroring already worked. Consolidated both through a shared direction helper without a dependency/version upgrade. Lack of `Icons.AutoMirrored` alone is not a functional RTL defect; decorative tag/list icon silhouettes are not navigation directions. |
| Week starts Monday regardless of locale | True and intentional for the existing shared server/planner calendar-week contract. Did not change week attribution just for a device locale. |
| Heatmap accessibility dates are raw ISO | Confirmed and fixed with localized calendar dates, also used in chart detail rows. The accessible activity list remains separate from decorative cells. |
| Inter/Outfit can be selected with Arabic and fall back for Arabic glyphs | True font choice/fallback behavior. Preserved the user's selected font rather than replacing it silently; actual mixed-font rendering needs device checks. |
| Native Markdown has no content direction/alignment | Confirmed and fixed: native reading text resolves content direction, ignoring a leading option-letter prefix, and uses text-start alignment. App layout direction is independent. |
| Native text uses 1.4/1.6 font-height multipliers and ignores style line height | Confirmed and fixed in Markdown and policy HTML through shared `NativeTextStyle`: reset multiplier, apply the Compose size/requested line height with `TextViewCompat`, disable extra font padding. Native font metrics can still require a larger minimum; no exact device height is claimed. |
| Arabic headings also use flat 1.6× spacing | Confirmed and adjusted: heading styles preserve their base height with a 1.3× Arabic minimum, body/label retain their base height with a 1.6× minimum. |
| Arabic gaps are 35–60% larger; fonts absent; choices ignore a 20sp button style | Estimates/incorrect generalization, not measured device findings. Actual TTFs exist. `hhea` heights range from Inter ≈1.21em to Noto Arabic 2.112em; these support removing the compounded multiplier but are not Android measured line heights. Button label style is 14sp here, not a universal 20sp. |
| Superscript/subscript and paragraphs enlarge lines; case header is ~60dp | Possible/rendering-dependent. Kept necessary medical spans, Markdown paragraph semantics and an accessible labeled case header; did not force span lines smaller or remove case context. Verify diacritics, scripts, paragraphs, tables and first/last lines on device. |
| Strings have complete parity; no left/right padding | Current source/resource checks support bilingual parity and logical positioning, not complete rendered RTL correctness. Counts in the post belonged to its snapshot. |

## Navigation and question reading

| Posted claim | Current finding and disposition |
| --- | --- |
| Drawer pushes roots while tabs/Home use different policies | Confirmed and fixed with one root-navigation helper for drawer and shortcuts. Existing root is reused when present; otherwise the previous root is removed back to Home. Sensitive/focused routes and session/write gates remain independent. |
| Tab restores old detail stacks/re-taps do not return to list | Source risk confirmed. Removed root state replay; an existing tab root can be popped back to without resetting its live list state. Restored/paused sessions still use their own recovery path. |
| Question/knowledge/case links can repeatedly stack the same targets | Confirmed and fixed: read-only links reuse an existing concrete target; create/edit success paths keep their explicit pop/replacement policy. Deliberately visiting different content remains navigable. |
| Ten empty busy BackHandlers silently consume presses | Confirmed. Replaced them with a shared guarded handler that explains the pending operation when Back is pressed. The guard is necessary during writes/recovery; it was not removed, and unknown writes are never replayed. Screens already provide loading/disabled command state. |
| Toolbar/system/back buttons disagree for Quizzes/Bookmarks/History | Confirmed and fixed: completion callbacks pop one entry, toolbar dispatch still invokes the screen's own guard/pause handler. Completion callbacks do not redispatch Back into themselves. |
| Menu command moves sides; non-tab screens select removed More tab | Confirmed and fixed: drawer command stays at logical start, with Back beside it on detail pages; nested routes select Library/Quizzes/Home parents and question detail keeps its origin. |
| More route should be deleted; in-content Back controls should all disappear | Not blanket defects. Retained the explicitly supported legacy More restoration route; its actions use root navigation. Embedded/public/guarded workflows can need their own callbacks. No compatibility route or guarded exit was removed solely for cosmetic duplication. |
| No independent question-language control | Confirmed and added in ordinary sessions, question detail, offline practice, master attempts and author preview. Selection is scoped to question/practice/attempt UI state, not a global app-language change. |
| Question resolvers and locale matching disagree; malformed choices produce mixed language | Confirmed and fixed with one read-only resolver and case-insensitive regional/base lookup (also reused by knowledge content). A translated stem requires a complete same-length nonblank choice array; incomplete options fall back together to Original. Only complete stem/choice languages get chips. |
| Explanations may silently fall back to original | Fixed: a selected translation does not borrow the original explanation; missing translated explanation has a visible Original hint on full reading screens. Ordinary exams explicitly keep explanations/correctness hidden until completed; learning feedback can use returned translations. |
| Separate case/source/category/tags have no translation data; base language unnamed | The original case API limit is superseded by the explicitly requested [optional case-translation extension](../../../backend/docs/contracts/caseTranslations.md): existing question-language selection applies available case title/stem translations with per-field original fallback. Matching schema/backend and device checks remain pending. Original remains the label because base language is unspecified; source/reference/category/tag identities remain as supplied. Supplied book data was not changed. |
| Completed-session review should also switch languages | Existing ordinary result/history DTOs carry frozen text without translation maps. Kept those frozen snapshots rather than fetching live questions that could have changed. A snapshot contract extension is needed before safe language selection there. |
| Quiz feedback is far below choices and choices shift when selected | Confirmed and fixed: stable icon/number slots and padding; correctness label beside the submitted learning answer, feedback before confidence. The API does not provide another correct-choice index in ordinary learning feedback, so Android does not invent or fetch it. |
| Library detail puts choices below provenance/admin blocks | Confirmed and fixed: stem, image and choices precede provenance/verification/management metadata; permissions and hidden-answer checks remain unchanged. |

## Verification and remaining gates

Production Kotlin lexical delimiters, duplicate imports, changed declarations/callers and `R.string` references checked across 161 files. All 1,416 English/Arabic keys, XML and positional placeholders match; repository diff whitespace passes. The 289-pair source color calculation and local font-table inspection are numerical/static checks, not Android execution. No builds/compilation, automated-suite inspection/work, migrations, database writes, backend/frontend edits, spreadsheet changes or dependency/version changes.

Use the [manual matrix](../verification/manualVerification.md) for source-complete changes: theme/3-button bars, native Arabic/Latin content and scripts, Arabic/RTL/200% text, density, toggle touch/TalkBack, navigation roots/detail reuse/back guards, language changes with pending writes, recall hidden choices and exam reveal restrictions, missing/incomplete translations, separate cases, and account/server/process restoration. The API limitations above remain explicit feature scope, not claimed fixes.
