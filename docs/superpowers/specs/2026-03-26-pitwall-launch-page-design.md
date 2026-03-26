# Pitwall Launch Page — Design Specification

**Date:** 2026-03-26
**Status:** Approved for implementation

---

## 1. Overview

A static marketing launch page for **Pitwall** — an F1 race prediction league app for friends. The page serves two sequential purposes:

1. **Pre-beta:** Collect email waitlist signups to build an audience before the app launches
2. **Beta live:** Swap the waitlist form for App Store and Google Play download buttons (one config flag flip)

Hosted on GitHub Pages at `pitwall.huelin.dev` as a public repository. No backend required.

---

## 2. Repository

| Property | Value |
|---|---|
| Repo name | `pitwall-launch` |
| Visibility | Public |
| Deployed URL | `pitwall.huelin.dev` |
| Deploy target | GitHub Pages (`gh-pages` branch via GitHub Actions) |

---

## 3. Tech Stack

| Layer | Technology |
|---|---|
| Framework | Astro 4 (static output) |
| Styling | Tailwind CSS |
| Language | TypeScript |
| Email capture | Formspree (free tier, no backend) |
| Deployment | GitHub Actions → GitHub Pages |
| HTTPS | GitHub Pages automatic cert |

---

## 4. Two-State Configuration

A single config file controls whether the page is in pre-beta or beta mode. Flipping `isBetaLive` to `true` and providing store URLs is the only change needed to switch from waitlist to download mode.

**`src/config.ts`:**
```ts
export const config = {
  isBetaLive: false,       // flip to true when beta launches
  appStoreUrl: "",         // Apple App Store URL — fill in when ready
  playStoreUrl: "",        // Google Play Store URL — fill in when ready
  formspreeId: "",         // Formspree form ID — set up once on account creation
}
```

All CTAs on the page read from this config. No other files need to change for a beta launch.

---

## 5. Page Structure

Single long-scroll page. All sections stack vertically with consistent horizontal padding.

### 5.1 Hero

- Full viewport height
- Deep black background (`#0A0A0A`) with a horizontal speed-line sweep animation on load (CSS only, no JS)
- App name: **Pitwall** — large, bold, white
- Tagline: *"Call the grid. Back your bets. Win the weekend."*
- Primary CTA:
  - **Pre-beta:** "Join the waitlist" button (smooth scrolls to waitlist section)
  - **Beta:** "Download on App Store" and "Get it on Google Play" buttons

### 5.2 Hook

- 3 short punchy lines, centred layout, large text
- Copy:
  > No luck. Pure strategy.
  > Built for fans who actually watch qualifying.
  > Your race weekend starts before the lights go out.

### 5.3 How It Works

- 3-step horizontal layout (stacks vertically on mobile)
- Each step: icon + bold headline + 1–2 sentence description
- Steps:
  1. 🏁 **Call the Grid** — Drag drivers into your predicted finishing order before qualifying locks you out. The closer you are, the more points you earn.
  2. 🎲 **Back Your Bets** — Stake points on bonus bets: fastest lap, safety car deployments, DNFs. Win big or lose your stake.
  3. 🏆 **Win the Weekend** — Compete in private leagues with your own scoring rules. Watch your projected score update live as the race unfolds.

### 5.4 Feature Highlights

- 3 cards in a grid layout (stacks to single column on mobile)
- Cards use `#141414` background with `#262626` border
- No screenshots (app not yet built) — icon-based illustration per card
- Cards:
  1. **Predict the Grid** — Drag-to-rank finishing order, proximity-based scoring, fully configurable prediction depth
  2. **Bonus Bets** — Stake your points on fastest lap, DNFs, safety car deployments. High risk, high reward.
  3. **Compete in Leagues** — Private leagues with custom scoring rules. Invite friends, set your own multipliers, track the standings.

### 5.5 Waitlist / Download CTA

**Pre-beta:**
- Section heading: *"Be first on the grid"*
- Subheading: *"Pitwall is coming to iOS and Android. Drop your email and we'll let you know when beta opens."*
- Email input + "Join the waitlist" submit button
- Powered by Formspree — form posts to `https://formspree.io/f/{formspreeId}`
- Success state: inline confirmation message, no page reload

**Beta (when `isBetaLive: true`):**
- Section heading: *"Beta is open"*
- Two buttons side by side: "Download on the App Store" / "Get it on Google Play"
- Links to `appStoreUrl` and `playStoreUrl` from config

### 5.6 Footer

- Minimal: "Pitwall — built by a fan, for fans"
- Link to GitHub profile (`huelin.dev` or GitHub handle)
- Privacy note: "We only use your email to notify you about Pitwall. No spam."

---

## 6. Visual Design

### 6.1 Colour Palette

| Token | Value | Usage |
|---|---|---|
| `--bg-primary` | `#0A0A0A` | Page background |
| `--bg-card` | `#141414` | Cards, section backgrounds |
| `--accent-red` | `#E10600` | Primary buttons, key highlights |
| `--accent-orange` | `#F97316` | Hover states, live/alert accents |
| `--text-primary` | `#FFFFFF` | Headings, prominent text |
| `--text-muted` | `#9CA3AF` | Body copy, labels, footer |
| `--border` | `#262626` | Card borders, section dividers |

### 6.2 Typography

- **Headings:** Space Grotesk (Google Fonts) — bold, technical feel
- **Body:** Inter (Google Fonts) — clean, readable
- Both loaded via `@fontsource` packages (no external CDN requests)

### 6.3 Animation

- One CSS-only speed-line sweep animation in the hero on page load
- No JavaScript animations
- All other transitions: subtle CSS hover states only (button colour shift, card border brightening)

### 6.4 Responsive

- Mobile-first layout
- Breakpoints: `sm` (640px), `md` (768px), `lg` (1024px) via Tailwind defaults
- How It Works and Feature Highlights sections collapse from horizontal grid to single column on mobile
- CTA buttons and form inputs are thumb-friendly (min 44px tap target)

---

## 7. Trademark & Copyright

- No use of "Formula 1", "F1", or the F1 logo
- No use of official team names, team colours, or driver likenesses
- Racing vocabulary only: grid, circuit, qualifying, race weekend, podium, sector, safety car, DNF
- All copy and visuals are original

---

## 8. GitHub Actions Pipelines

Two separate workflow files per the project rule: production deployments must always be manually triggered (`workflow_dispatch`), never automatic.

**CI — runs on every push (build + lint only, no deploy):**

```yaml
# .github/workflows/ci.yml
on:
  push:
    branches: ["**"]
  pull_request:
    branches: [main]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
      - run: npm ci
      - run: npm run build
```

**Deploy — manual trigger only:**

```yaml
# .github/workflows/deploy.yml
on:
  workflow_dispatch:

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
      - run: npm ci
      - run: npm run build
      - uses: peaceiris/actions-gh-pages@v3
        with:
          github_token: ${{ secrets.GITHUB_TOKEN }}
          publish_dir: ./dist
          cname: pitwall.huelin.dev
```

The `cname` parameter in the deploy action automatically writes the `CNAME` file to the `gh-pages` branch on each deploy. No manual `CNAME` file in the repo root is needed.

---

## 9. Custom Domain Setup

1. Add DNS record at domain registrar:
   - Type: `CNAME`
   - Name: `pitwall`
   - Value: `dhuelin.github.io`
2. The `cname: pitwall.huelin.dev` parameter in the deploy workflow automatically writes the `CNAME` file to the `gh-pages` branch — no manual file needed in the repo root.
3. GitHub Pages detects the CNAME and provisions HTTPS automatically after the first deploy.

---

## 10. Formspree Setup

1. Create free account at formspree.io
2. Create a new form — note the form ID
3. Add form ID to `src/config.ts`
4. Submissions are emailed to your registered address
5. Free tier: 50 submissions/month. **Important:** submissions beyond the monthly limit are silently dropped — not queued or deferred. Monitor the Formspree dashboard regularly as signups grow and upgrade the plan before hitting the cap.

---

## 11. Beta Transition Checklist

When beta is ready:

- [ ] Fill in `appStoreUrl` in `src/config.ts`
- [ ] Fill in `playStoreUrl` in `src/config.ts`
- [ ] Set `isBetaLive: true` in `src/config.ts`
- [ ] Trigger the deploy workflow manually via GitHub Actions → `deploy.yml` → "Run workflow"
- [ ] Verify both store links work on the live page
- [ ] Email waitlist subscribers (manual, from Formspree dashboard)

---

## 12. Out of Scope

- Blog or dev updates section
- Analytics or tracking scripts
- App screenshots or demo video (added later when app is built)
- Dark/light mode toggle — the launch page is intentionally dark-only as a deliberate design decision for a high-impact first impression. Note: the main Pitwall app does support adaptive dark/light mode following device system preference; this is a launch page exception, not a brand contradiction.
