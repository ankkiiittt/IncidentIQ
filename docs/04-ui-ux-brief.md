# 04 - UI/UX Design Brief

## 1. Experience goal
- **Desired feeling**: A calm, trustworthy operations console. Dense but readable; the AI feels like a careful assistant, never an oracle.
- **Three adjectives**: Focused, transparent, dependable.
- **Must avoid**: Gradients, glassmorphism, decorative animation, alarmist red everywhere, chatbot bubbles, hiding evidence behind confident text, anything implying the AI acted.

## 2. Users and context
- **Primary user**: Engineers working under time pressure, often with several tabs open.
- **Device**: Desktop first at 1024 px and wider; tablet usable.
- **Accessibility**: WCAG 2.1 AA, keyboard operable, status never conveyed by colour alone.

## 3. Visual direction
| Role | Token | Hex | Notes |
|---|---|---|---|
| Page background | `bg` | `#F8FAFC` | |
| Surface | `surface` | `#FFFFFF` | |
| Border | `border` | `#E2E8F0` | |
| Text | `text` | `#0F172A` | |
| Muted text | `muted` | `#64748B` | |
| Primary | `primary` | `#4F46E5` | |
| P1 / Critical | `p1` | `#B91C1C` on `#FEE2E2` | text on background tint |
| P2 / High | `p2` | `#C2410C` on `#FFEDD5` | text on background tint |
| P3 / Medium | `p3` | `#A16207` on `#FEF9C3` | text on background tint |
| P4 / Low | `p4` | `#475569` on `#F1F5F9` | text on background tint |
| Success | `success` | `#15803D` on `#DCFCE7` | text on background tint |
| AI accent | `ai` | `#6D28D9` on `#EDE9FE` | used ONLY for AI-generated content |
| Chart series | `series 1-6` | `#4F46E5`, `#0EA5E9`, `#F59E0B`, `#10B981`, `#EC4899`, `#64748B` | |

- **Typography**: Inter for UI, JetBrains Mono for ids, logs and queries. Sizes: page title 24/32, section 18/26, body 14/20, caption 12/16, table 13/18. Weights 400, 500, 600. Tabular numerals in tables.
- **Icons**: One outline set (`lucide-react`). No illustrations.
- **Surfaces**: White cards, 1 px border, 8 px radius, no shadows except menus and dialogs.

## 4. Layout system
- **App shell**: Fixed left sidebar (240 px, collapsible to icons), top bar with global search and user menu, content area max width 1440 px.
- **Grid**: 12 columns, 24 px gutters; KPI cards span 3 columns; detail screens use 8 plus 4 column splits.
- **Spacing scale**: 4, 8, 12, 16, 24, 32. Section rhythm 24 px. Breakpoints 1024 and 1440; below 1024 the sidebar becomes a drawer and tables scroll horizontally.

## 5. Component language
| Component | Rules | States |
|---|---|---|
| Buttons | One primary action per view region; secondary outlined; destructive in red text with confirmation. | default, hover, focus ring 2 px, active, disabled, loading |
| Inputs | Label above, helper below, error text with icon. | default, focus, error, disabled |
| Navigation | Sidebar items with icon and label; active item filled; Admin shown to ADMIN only. | default, active, focus |
| Priority and status chips | Text plus icon plus tint. P1 to P4 use tokens above. Status chips use neutral tints. | static |
| Tables | Sticky header, row hover, sortable columns with aria-sort, density compact, SLA column shows countdown text and an icon when at risk or breached. | default, loading, empty, error |
| AI badge | Violet chip *AI-generated* on every AI output with model and prompt version in a tooltip. | static |
| Confidence | Percent plus horizontal bar plus word (High, Medium, Low). Never a bare colour. | static |
| Evidence card | Source type chip, title, similarity percent, 3-line snippet, link to source, id in mono. | default, hover, expanded |
| Recommendation row | Action text, rationale, status chip (Suggested, Accepted, Rejected, Done), caption: *Recommended, not executed*. | default, updating |
| Timeline | Vertical list with icon, actor, time, payload summary; AI events use AI accent. | default, loading |
| Toasts and banners | Toast for success; banner for persistent states (connection lost, data stale). | info, success, warning, error |
| Dialogs | Confirm resolve (root cause and resolution fields), delete, reset password. | open, submitting, error |

## 6. Screen direction
- **S2 Dashboard**: Row of KPI cards, then trend chart (8 columns) beside top services and predictions (4 columns), then recent incidents table.
- **S3 List**: Filter bar above a dense table; primary New incident button top right.
- **S5 Detail**: Two columns: left 8 (header, description, timeline, comments), right 4 (fields, workflow actions, AI panel, similar incidents).
- **S6 Analysis**: Left: causes and recommendations; right: evidence cards. Evidence is always visible next to the claim it supports.
- **S7 Knowledge**: Tabs with a wide search box; results as evidence-style cards.
- **S8 Service Health**: Metric small multiples (2 by 3) with deployment markers.
- **S9 Analytics**: KPI cards then 2 by 2 charts and the recurring-causes table.
- **Responsive**: Below 1024 px columns stack and the sidebar becomes a drawer.

## 7. Interaction and motion
- Motion only for state feedback: spinners, skeletons, 150 ms fade for toasts. No decorative animation.
- Feedback moments: toast on create, assign, resolve, feedback saved; chip transitions when analysis status changes.
- Reduced motion: respect prefers-reduced-motion by disabling fades and skeleton shimmer.

## 8. Accessibility
- Contrast at least 4.5:1 for text and 3:1 for UI borders and chart lines.
- Every chart has a caption and a data-table toggle. Status always includes text.
- Keyboard: logical tab order, visible focus ring, dialogs trap focus, Escape closes, table rows reachable and actionable.
- Live regions announce analysis status changes. Targets at least 40 px high for primary actions. Text scales to 200 percent.

## 9. Consistency rules
| Always use | Never use |
|---|---|
| Priority and status chips exactly as defined; AI badge on all AI content; units and relative times with absolute time on hover. | Colour alone for state; AI wording that implies action was taken (for example Fixed, Restarted); unexplained scores. |
| Plain language for confidence and evidence; phrase *Recommended, not executed* beside actions. | Chat-style bubbles; gradients; more than one primary button in a region. |
| Skeletons for loading and explicit empty states. | Blocking full-page spinners. |

**Tailwind setup**: Extend theme colors with tokens above, `fontFamily: sans = Inter`, `mono = JetBrains Mono`, container max width 1440. `darkMode` class reserved for later phase (not built in v1).
