# Pitwall Launch Page Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build and deploy a static Astro launch page for Pitwall at `pitwall.huelin.dev` that collects email waitlist signups pre-beta and can be switched to App Store/Play Store download links with a single config flag.

**Architecture:** A standalone Astro 4 static site in a new public GitHub repo (`pitwall-launch`). The page is a single long-scroll file assembling six Astro components. A `src/config.ts` module holds the two-state flag (`isBetaLive`) and all store/form IDs — every CTA reads from this one file. CI builds + lints on every push; deploy is manual (`workflow_dispatch`) per project rules.

**Tech Stack:** Astro 4, Tailwind CSS, TypeScript, `@fontsource/inter`, `@fontsource/space-grotesk`, ESLint + `eslint-plugin-astro`, Formspree (email), `peaceiris/actions-gh-pages@v3` (deploy)

**Spec:** `docs/superpowers/specs/2026-03-26-pitwall-launch-page-design.md`

---

## File Structure

```
pitwall-launch/
├── .github/
│   └── workflows/
│       ├── ci.yml                      # Build + lint on every push — no deploy
│       └── deploy.yml                  # Manual workflow_dispatch deploy to GitHub Pages
├── src/
│   ├── config.ts                       # Two-state config: isBetaLive, store URLs, formspreeId
│   ├── pages/
│   │   └── index.astro                 # Root page — imports all components, sets HTML meta
│   ├── components/
│   │   ├── Hero.astro                  # Full-viewport hero: name, tagline, speed-line animation, CTA
│   │   ├── Hook.astro                  # 3-line punchy copy section
│   │   ├── HowItWorks.astro            # 3-step grid: Call the Grid / Back Your Bets / Win the Weekend
│   │   ├── FeatureHighlights.astro     # 3 feature cards: Predict / Bonus Bets / Leagues
│   │   ├── WaitlistCta.astro           # Formspree email form (pre-beta) OR store buttons (beta)
│   │   └── Footer.astro               # Minimal footer with privacy note
│   └── styles/
│       └── global.css                  # CSS custom properties, speed-line keyframes, base resets
├── public/
│   └── favicon.svg                     # Simple Pitwall "P" favicon in brand red
├── astro.config.mjs                    # Astro config: output = 'static', site URL
├── tailwind.config.mjs                 # Tailwind config: extend with brand colour tokens
├── tsconfig.json                       # TypeScript strict mode
├── .eslintrc.cjs                       # ESLint config with astro + typescript plugins
├── package.json                        # Scripts: dev, build, lint, preview
└── README.md                           # Local dev setup, Formspree setup, beta transition guide
```

---

## GitHub Issues to Create (in `pitwall-launch` repo)

| # | Title | Label |
|---|---|---|
| 1 | Repo scaffold: Astro 4 + Tailwind + TypeScript + ESLint | `infrastructure` |
| 2 | Config module and design system tokens | `enhancement` |
| 3 | Hero section with speed-line animation | `enhancement` |
| 4 | Hook section | `enhancement` |
| 5 | How It Works section | `enhancement` |
| 6 | Feature Highlights section | `enhancement` |
| 7 | Waitlist / Download CTA section | `enhancement` |
| 8 | Footer | `enhancement` |
| 9 | Main page assembly and meta tags | `enhancement` |
| 10 | CI and deploy GitHub Actions workflows | `infrastructure` |
| 11 | README and beta transition documentation | `documentation` |

---

## Task 1: Create GitHub Repo and Project Scaffold

**Files:**
- Create: GitHub repo `pitwall-launch`
- Create: `package.json`, `astro.config.mjs`, `tailwind.config.mjs`, `tsconfig.json`, `.eslintrc.cjs`

- [ ] **Step 1: Create the GitHub repository**

  Go to GitHub → New repository → name: `pitwall-launch` → Public → No template → Create.

- [ ] **Step 2: Create labels**

  In GitHub → Issues → Labels → create:
  `enhancement` (blue), `bug` (red), `infrastructure` (grey), `documentation` (yellow)

- [ ] **Step 3: Create all 11 issues**

  Use the table above. No milestone needed for this repo.

- [ ] **Step 4: Initialize Astro project locally**

  ```bash
  npm create astro@latest pitwall-launch -- --template minimal --typescript strict --no-git
  cd pitwall-launch
  ```

  When prompted: TypeScript = strict, install dependencies = yes.

- [ ] **Step 5: Add Tailwind**

  ```bash
  npx astro add tailwind --yes
  ```

  This creates `tailwind.config.mjs` and updates `astro.config.mjs` automatically.

- [ ] **Step 6: Install font and ESLint packages**

  ```bash
  npm install @fontsource/inter @fontsource/space-grotesk
  npm install -D eslint @typescript-eslint/parser @typescript-eslint/eslint-plugin eslint-plugin-astro
  ```

- [ ] **Step 7: Create `.eslintrc.cjs`**

  ```js
  // .eslintrc.cjs
  module.exports = {
    extends: [
      'eslint:recommended',
      'plugin:@typescript-eslint/recommended',
      'plugin:astro/recommended',
    ],
    parser: '@typescript-eslint/parser',
    plugins: ['@typescript-eslint'],
    overrides: [
      {
        files: ['*.astro'],
        parser: 'astro-eslint-parser',
        parserOptions: {
          parser: '@typescript-eslint/parser',
          extraFileExtensions: ['.astro'],
        },
      },
    ],
    rules: {},
  }
  ```

- [ ] **Step 8: Add lint script to `package.json`**

  Add to the `scripts` section:
  ```json
  "lint": "eslint src --ext .ts,.astro"
  ```

- [ ] **Step 9: Update `astro.config.mjs` with site URL**

  ```js
  // astro.config.mjs
  import { defineConfig } from 'astro/config'
  import tailwind from '@astrojs/tailwind'

  export default defineConfig({
    site: 'https://pitwall.huelin.dev',
    output: 'static',
    integrations: [tailwind()],
  })
  ```

- [ ] **Step 10: Verify clean build and lint pass**

  ```bash
  npm run build
  npm run lint
  ```

  Expected: build outputs to `dist/`, lint reports no errors.

- [ ] **Step 11: Initialise git, push to GitHub**

  ```bash
  git init
  git add .
  git commit -m "[#1] Scaffold Astro 4 + Tailwind + TypeScript + ESLint"
  git branch -M main
  git remote add origin https://github.com/dhuelin/pitwall-launch.git
  git push -u origin main
  ```

---

## Task 2: Config Module and Design System

**Files:**
- Create: `src/config.ts`
- Create: `src/styles/global.css`
- Modify: `tailwind.config.mjs`

- [ ] **Step 1: Create `src/config.ts`**

  ```ts
  // src/config.ts
  export const config = {
    isBetaLive: false,       // flip to true when beta launches
    appStoreUrl: '',         // Apple App Store URL — fill in when ready
    playStoreUrl: '',        // Google Play Store URL — fill in when ready
    formspreeId: '',         // Formspree form ID — set up at formspree.io, paste here
  } as const
  ```

- [ ] **Step 2: Extend Tailwind with brand tokens**

  ```js
  // tailwind.config.mjs
  /** @type {import('tailwindcss').Config} */
  export default {
    content: ['./src/**/*.{astro,html,js,jsx,ts,tsx}'],
    theme: {
      extend: {
        colors: {
          'bg-primary':  '#0A0A0A',
          'bg-card':     '#141414',
          'accent-red':  '#E10600',
          'accent-orange': '#F97316',
          'text-primary': '#FFFFFF',
          'text-muted':  '#9CA3AF',
          'border-subtle': '#262626',
        },
        fontFamily: {
          sans:     ['Inter', 'sans-serif'],
          heading:  ['Space Grotesk', 'sans-serif'],
        },
      },
    },
    plugins: [],
  }
  ```

- [ ] **Step 3: Create `src/styles/global.css`**

  ```css
  /* src/styles/global.css */
  @import '@fontsource/inter/400.css';
  @import '@fontsource/inter/500.css';
  @import '@fontsource/space-grotesk/600.css';
  @import '@fontsource/space-grotesk/700.css';

  @tailwind base;
  @tailwind components;
  @tailwind utilities;

  :root {
    --bg-primary:    #0A0A0A;
    --bg-card:       #141414;
    --accent-red:    #E10600;
    --accent-orange: #F97316;
    --text-primary:  #FFFFFF;
    --text-muted:    #9CA3AF;
    --border:        #262626;
  }

  html {
    scroll-behavior: smooth;
  }

  body {
    background-color: var(--bg-primary);
    color: var(--text-primary);
    font-family: 'Inter', sans-serif;
    -webkit-font-smoothing: antialiased;
  }

  /* Speed-line sweep animation — used in Hero */
  @keyframes speedline-sweep {
    0%   { transform: translateX(-100%); opacity: 0; }
    20%  { opacity: 0.15; }
    80%  { opacity: 0.15; }
    100% { transform: translateX(200%); opacity: 0; }
  }

  .speedline {
    position: absolute;
    height: 1px;
    width: 60%;
    background: linear-gradient(to right, transparent, var(--accent-red), transparent);
    animation: speedline-sweep 1.8s ease-in-out forwards;
    pointer-events: none;
  }

  .speedline-1 { top: 30%; animation-delay: 0.1s; }
  .speedline-2 { top: 45%; width: 80%; animation-delay: 0.25s; }
  .speedline-3 { top: 55%; width: 50%; animation-delay: 0.4s; }
  .speedline-4 { top: 65%; width: 70%; animation-delay: 0.15s; }
  ```

- [ ] **Step 4: Update `tailwind.config.mjs` to pick up global.css import path**

  Verify `content` array includes `'./src/**/*.{astro,html,js,jsx,ts,tsx}'` — already set in step 2.

- [ ] **Step 5: Verify build passes**

  ```bash
  npm run build && npm run lint
  ```

  Expected: clean build and lint, no errors.

- [ ] **Step 6: Commit**

  ```bash
  git add src/config.ts src/styles/global.css tailwind.config.mjs
  git commit -m "[#2] Add config module and design system tokens"
  ```

---

## Task 3: Hero Section

**Files:**
- Create: `src/components/Hero.astro`

- [ ] **Step 1: Create `src/components/Hero.astro`**

  ```astro
  ---
  // src/components/Hero.astro
  import { config } from '../config'
  ---

  <section class="relative flex flex-col items-center justify-center min-h-screen overflow-hidden bg-bg-primary px-6 text-center">
    <!-- Speed-line animation (CSS only) -->
    <div aria-hidden="true">
      <div class="speedline speedline-1"></div>
      <div class="speedline speedline-2"></div>
      <div class="speedline speedline-3"></div>
      <div class="speedline speedline-4"></div>
    </div>

    <!-- Content -->
    <div class="relative z-10 max-w-3xl mx-auto">
      <p class="text-accent-red font-heading font-semibold text-sm tracking-widest uppercase mb-4">
        Coming to iOS &amp; Android
      </p>
      <h1 class="font-heading font-bold text-6xl sm:text-7xl lg:text-8xl text-text-primary leading-none mb-6">
        Pitwall
      </h1>
      <p class="text-text-muted text-xl sm:text-2xl max-w-xl mx-auto mb-10 leading-relaxed">
        Call the grid. Back your bets. Win the weekend.
      </p>

      {config.isBetaLive ? (
        <div class="flex flex-col sm:flex-row gap-4 justify-center">
          <a
            href={config.appStoreUrl}
            class="inline-flex items-center justify-center px-8 py-4 bg-accent-red hover:bg-accent-orange text-white font-heading font-semibold rounded-lg transition-colors min-h-[44px]"
          >
            Download on the App Store
          </a>
          <a
            href={config.playStoreUrl}
            class="inline-flex items-center justify-center px-8 py-4 border border-border-subtle hover:border-accent-red text-text-primary font-heading font-semibold rounded-lg transition-colors min-h-[44px]"
          >
            Get it on Google Play
          </a>
        </div>
      ) : (
        <a
          href="#waitlist"
          class="inline-flex items-center justify-center px-8 py-4 bg-accent-red hover:bg-accent-orange text-white font-heading font-semibold rounded-lg transition-colors min-h-[44px]"
        >
          Join the waitlist
        </a>
      )}
    </div>

    <!-- Scroll indicator -->
    <div class="absolute bottom-8 left-1/2 -translate-x-1/2 text-text-muted text-sm animate-bounce">
      ↓
    </div>
  </section>
  ```

- [ ] **Step 2: Verify build passes**

  ```bash
  npm run build
  ```

  Expected: no errors.

- [ ] **Step 3: Preview locally and check speed lines animate**

  ```bash
  npm run preview
  ```

  Open browser at the local URL. Verify: speed lines sweep across on load, "Join the waitlist" button is visible, page fills viewport.

- [ ] **Step 4: Commit**

  ```bash
  git add src/components/Hero.astro
  git commit -m "[#3] Add Hero section with speed-line animation"
  ```

---

## Task 4: Hook Section

**Files:**
- Create: `src/components/Hook.astro`

- [ ] **Step 1: Create `src/components/Hook.astro`**

  ```astro
  ---
  // src/components/Hook.astro
  ---

  <section class="py-24 px-6 bg-bg-primary">
    <div class="max-w-3xl mx-auto text-center space-y-4">
      <p class="font-heading font-bold text-3xl sm:text-4xl text-text-primary">
        No luck. Pure strategy.
      </p>
      <p class="font-heading font-bold text-3xl sm:text-4xl text-text-primary">
        Built for fans who actually watch qualifying.
      </p>
      <p class="font-heading font-bold text-3xl sm:text-4xl text-accent-red">
        Your race weekend starts before the lights go out.
      </p>
    </div>
  </section>
  ```

- [ ] **Step 2: Build and commit**

  ```bash
  npm run build
  git add src/components/Hook.astro
  git commit -m "[#4] Add Hook section"
  ```

---

## Task 5: How It Works Section

**Files:**
- Create: `src/components/HowItWorks.astro`

- [ ] **Step 1: Create `src/components/HowItWorks.astro`**

  ```astro
  ---
  // src/components/HowItWorks.astro
  const steps = [
    {
      icon: '🏁',
      title: 'Call the Grid',
      description:
        'Drag drivers into your predicted finishing order before qualifying locks you out. The closer you are, the more points you earn.',
    },
    {
      icon: '🎲',
      title: 'Back Your Bets',
      description:
        'Stake points on bonus bets: fastest lap, safety car deployments, DNFs. Win big or lose your stake.',
    },
    {
      icon: '🏆',
      title: 'Win the Weekend',
      description:
        'Compete in private leagues with your own scoring rules. Watch your projected score update live as the race unfolds.',
    },
  ]
  ---

  <section class="py-24 px-6 bg-bg-card border-y border-border-subtle">
    <div class="max-w-5xl mx-auto">
      <h2 class="font-heading font-bold text-3xl sm:text-4xl text-text-primary text-center mb-16">
        How it works
      </h2>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-12">
        {steps.map((step, i) => (
          <div class="flex flex-col items-center text-center">
            <div class="text-5xl mb-6" aria-hidden="true">{step.icon}</div>
            <div class="text-accent-red font-heading font-semibold text-sm tracking-widest uppercase mb-2">
              Step {i + 1}
            </div>
            <h3 class="font-heading font-bold text-xl text-text-primary mb-3">
              {step.title}
            </h3>
            <p class="text-text-muted leading-relaxed">{step.description}</p>
          </div>
        ))}
      </div>
    </div>
  </section>
  ```

- [ ] **Step 2: Build and commit**

  ```bash
  npm run build
  git add src/components/HowItWorks.astro
  git commit -m "[#5] Add How It Works section"
  ```

---

## Task 6: Feature Highlights Section

**Files:**
- Create: `src/components/FeatureHighlights.astro`

- [ ] **Step 1: Create `src/components/FeatureHighlights.astro`**

  ```astro
  ---
  // src/components/FeatureHighlights.astro
  const features = [
    {
      icon: '📋',
      title: 'Predict the Grid',
      description:
        'Drag-to-rank finishing order with proximity-based scoring. The closer your prediction, the more points you earn. Fully configurable prediction depth per league.',
    },
    {
      icon: '💰',
      title: 'Bonus Bets',
      description:
        'Stake your points on fastest lap, DNFs, and safety car deployments. High risk, high reward — wrong bet and you lose your stake.',
    },
    {
      icon: '🏟️',
      title: 'Compete in Leagues',
      description:
        'Private leagues with custom scoring rules. Invite friends, set your own multipliers, and track the standings across every race weekend.',
    },
  ]
  ---

  <section class="py-24 px-6 bg-bg-primary">
    <div class="max-w-5xl mx-auto">
      <h2 class="font-heading font-bold text-3xl sm:text-4xl text-text-primary text-center mb-16">
        Everything you need for race weekend
      </h2>
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        {features.map((feature) => (
          <div class="bg-bg-card border border-border-subtle rounded-xl p-8 hover:border-accent-red transition-colors">
            <div class="text-4xl mb-5" aria-hidden="true">{feature.icon}</div>
            <h3 class="font-heading font-bold text-lg text-text-primary mb-3">
              {feature.title}
            </h3>
            <p class="text-text-muted leading-relaxed text-sm">{feature.description}</p>
          </div>
        ))}
      </div>
    </div>
  </section>
  ```

- [ ] **Step 2: Build and commit**

  ```bash
  npm run build
  git add src/components/FeatureHighlights.astro
  git commit -m "[#6] Add Feature Highlights section"
  ```

---

## Task 7: Waitlist / Download CTA Section

**Files:**
- Create: `src/components/WaitlistCta.astro`

- [ ] **Step 1: Create `src/components/WaitlistCta.astro`**

  ```astro
  ---
  // src/components/WaitlistCta.astro
  import { config } from '../config'
  ---

  <section id="waitlist" class="py-24 px-6 bg-bg-card border-t border-border-subtle">
    <div class="max-w-xl mx-auto text-center">
      {config.isBetaLive ? (
        <>
          <h2 class="font-heading font-bold text-3xl sm:text-4xl text-text-primary mb-4">
            Beta is open
          </h2>
          <p class="text-text-muted mb-10">
            Download Pitwall now and start predicting.
          </p>
          <div class="flex flex-col sm:flex-row gap-4 justify-center">
            <a
              href={config.appStoreUrl}
              class="inline-flex items-center justify-center px-8 py-4 bg-accent-red hover:bg-accent-orange text-white font-heading font-semibold rounded-lg transition-colors min-h-[44px]"
            >
              Download on the App Store
            </a>
            <a
              href={config.playStoreUrl}
              class="inline-flex items-center justify-center px-8 py-4 border border-border-subtle hover:border-accent-red text-text-primary font-heading font-semibold rounded-lg transition-colors min-h-[44px]"
            >
              Get it on Google Play
            </a>
          </div>
        </>
      ) : (
        <>
          <h2 class="font-heading font-bold text-3xl sm:text-4xl text-text-primary mb-4">
            Be first on the grid
          </h2>
          <p class="text-text-muted mb-10">
            Pitwall is coming to iOS and Android. Drop your email and we'll let you know when beta opens.
          </p>

          <form
            id="waitlist-form"
            action={`https://formspree.io/f/${config.formspreeId}`}
            method="POST"
            class="flex flex-col sm:flex-row gap-3"
          >
            <input
              type="email"
              name="email"
              required
              placeholder="your@email.com"
              class="flex-1 px-4 py-3 bg-bg-primary border border-border-subtle rounded-lg text-text-primary placeholder-text-muted focus:outline-none focus:border-accent-red transition-colors min-h-[44px]"
            />
            <button
              type="submit"
              class="px-8 py-3 bg-accent-red hover:bg-accent-orange text-white font-heading font-semibold rounded-lg transition-colors min-h-[44px] whitespace-nowrap"
            >
              Join the waitlist
            </button>
          </form>

          <!-- Inline success/error state — no page reload -->
          <p id="form-success" class="hidden mt-4 text-accent-orange font-medium">
            ✓ You're on the list. We'll be in touch.
          </p>
          <p id="form-error" class="hidden mt-4 text-red-400 text-sm">
            Something went wrong. Please try again or email us directly.
          </p>
        </>
      )}
    </div>
  </section>

  {!config.isBetaLive && (
    <script>
      const form = document.getElementById('waitlist-form') as HTMLFormElement | null
      const success = document.getElementById('form-success')
      const error = document.getElementById('form-error')

      form?.addEventListener('submit', async (e) => {
        e.preventDefault()
        const data = new FormData(form)
        try {
          const res = await fetch(form.action, {
            method: 'POST',
            body: data,
            headers: { Accept: 'application/json' },
          })
          if (res.ok) {
            form.style.display = 'none'
            success?.classList.remove('hidden')
          } else {
            error?.classList.remove('hidden')
          }
        } catch {
          error?.classList.remove('hidden')
        }
      })
    </script>
  )}
  ```

- [ ] **Step 2: Build and verify no TypeScript errors**

  ```bash
  npm run build && npm run lint
  ```

  Expected: clean output. The `<script>` block is client-side JS — Astro handles it correctly.

- [ ] **Step 3: Commit**

  ```bash
  git add src/components/WaitlistCta.astro
  git commit -m "[#7] Add Waitlist / Download CTA section with inline Formspree submission"
  ```

---

## Task 8: Footer

**Files:**
- Create: `src/components/Footer.astro`

- [ ] **Step 1: Create `src/components/Footer.astro`**

  ```astro
  ---
  // src/components/Footer.astro
  ---

  <footer class="py-10 px-6 bg-bg-primary border-t border-border-subtle">
    <div class="max-w-5xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4 text-text-muted text-sm">
      <p>Pitwall — built by a fan, for fans.</p>
      <p class="text-center text-xs">
        We only use your email to notify you about Pitwall. No spam.
      </p>
      <a
        href="https://github.com/dhuelin"
        target="_blank"
        rel="noopener noreferrer"
        class="hover:text-text-primary transition-colors"
      >
        GitHub ↗
      </a>
    </div>
  </footer>
  ```

- [ ] **Step 2: Build and commit**

  ```bash
  npm run build
  git add src/components/Footer.astro
  git commit -m "[#8] Add Footer"
  ```

---

## Task 9: Main Page Assembly and Meta Tags

**Files:**
- Modify: `src/pages/index.astro`

- [ ] **Step 1: Replace `src/pages/index.astro` with full page assembly**

  ```astro
  ---
  // src/pages/index.astro
  import '../styles/global.css'
  import Hero from '../components/Hero.astro'
  import Hook from '../components/Hook.astro'
  import HowItWorks from '../components/HowItWorks.astro'
  import FeatureHighlights from '../components/FeatureHighlights.astro'
  import WaitlistCta from '../components/WaitlistCta.astro'
  import Footer from '../components/Footer.astro'

  const title = 'Pitwall — Call the grid. Back your bets. Win the weekend.'
  const description =
    'A prediction league for race fans. Pick your finishing order, place bonus bets, and compete with friends across every race weekend.'
  const url = 'https://pitwall.huelin.dev'
  const ogImage = `${url}/og.png`
  ---

  <!doctype html>
  <html lang="en">
    <head>
      <meta charset="UTF-8" />
      <meta name="viewport" content="width=device-width, initial-scale=1.0" />
      <title>{title}</title>
      <meta name="description" content={description} />
      <link rel="canonical" href={url} />

      <!-- Open Graph / social sharing -->
      <meta property="og:type" content="website" />
      <meta property="og:url" content={url} />
      <meta property="og:title" content={title} />
      <meta property="og:description" content={description} />

      <!-- Twitter card -->
      <meta name="twitter:card" content="summary_large_image" />
      <meta name="twitter:title" content={title} />
      <meta name="twitter:description" content={description} />

      <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
    </head>
    <body>
      <Hero />
      <Hook />
      <HowItWorks />
      <FeatureHighlights />
      <WaitlistCta />
      <Footer />
    </body>
  </html>
  ```

- [ ] **Step 2: Create `public/favicon.svg`**

  ```svg
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 32 32">
    <rect width="32" height="32" rx="6" fill="#0A0A0A"/>
    <text x="16" y="23" font-family="sans-serif" font-weight="bold" font-size="18" fill="#E10600" text-anchor="middle">P</text>
  </svg>
  ```

- [ ] **Step 3: Full build + lint**

  ```bash
  npm run build && npm run lint
  ```

  Expected: `dist/index.html` exists, no errors.

- [ ] **Step 4: Preview and smoke-test the full page**

  ```bash
  npm run preview
  ```

  Check in browser:
  - Hero fills viewport, speed lines animate
  - Smooth scroll works when clicking "Join the waitlist"
  - All 6 sections render correctly
  - Mobile layout: resize to 375px width — How It Works and Feature Highlights stack to single column
  - Footer links are correct

- [ ] **Step 5: Commit**

  ```bash
  git add src/pages/index.astro public/favicon.svg
  git commit -m "[#9] Assemble main page with all sections and meta tags"
  ```

---

## Task 10: CI and Deploy Workflows

**Files:**
- Create: `.github/workflows/ci.yml`
- Create: `.github/workflows/deploy.yml`

- [ ] **Step 1: Create `.github/workflows/ci.yml`**

  ```yaml
  # .github/workflows/ci.yml
  name: CI

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
            cache: npm
        - run: npm ci
        - run: npm run build
        - run: npm run lint
  ```

- [ ] **Step 2: Create `.github/workflows/deploy.yml`**

  ```yaml
  # .github/workflows/deploy.yml
  name: Deploy

  on:
    workflow_dispatch:

  jobs:
    deploy:
      runs-on: ubuntu-latest
      permissions:
        contents: write
      steps:
        - uses: actions/checkout@v4
        - uses: actions/setup-node@v4
          with:
            node-version: 20
            cache: npm
        - run: npm ci
        - run: npm run build
        - uses: peaceiris/actions-gh-pages@v3
          with:
            github_token: ${{ secrets.GITHUB_TOKEN }}
            publish_dir: ./dist
            cname: pitwall.huelin.dev
  ```

  Note: `permissions: contents: write` is required for `peaceiris/actions-gh-pages` to push to the `gh-pages` branch.

- [ ] **Step 3: Commit and push**

  ```bash
  git add .github/
  git commit -m "[#10] Add CI and manual deploy GitHub Actions workflows"
  git push origin main
  ```

- [ ] **Step 4: Verify CI passes on GitHub**

  Go to GitHub → Actions → CI run triggered by the push. Expected: green build + lint.

---

## Task 11: README and Beta Transition Documentation

**Files:**
- Modify: `README.md`

- [ ] **Step 1: Write `README.md`**

  ```markdown
  # Pitwall — Launch Page

  Static marketing site for Pitwall, a race prediction league app.

  **Live:** [pitwall.huelin.dev](https://pitwall.huelin.dev)

  ## Local Development

  \`\`\`bash
  npm install
  npm run dev      # http://localhost:4321
  \`\`\`

  ## Build

  \`\`\`bash
  npm run build    # outputs to dist/
  npm run preview  # preview the build locally
  \`\`\`

  ## Deploy

  Deployments are **manual only**. Go to GitHub → Actions → Deploy → Run workflow.

  The `gh-pages` branch is managed automatically by the deploy workflow.

  ## Formspree Setup

  1. Create a free account at [formspree.io](https://formspree.io)
  2. Create a new form and note the form ID
  3. Add the ID to `src/config.ts` → `formspreeId`
  4. Free tier: 50 submissions/month — submissions beyond this limit are silently dropped.
     Monitor your Formspree dashboard and upgrade before hitting the cap.

  ## Switching to Beta Mode

  When the app beta is ready:

  1. Fill in `appStoreUrl` in `src/config.ts`
  2. Fill in `playStoreUrl` in `src/config.ts`
  3. Set `isBetaLive: true` in `src/config.ts`
  4. Commit and push to `main`
  5. Go to GitHub → Actions → Deploy → Run workflow
  6. Verify both store links work on the live page
  7. Email waitlist subscribers from the Formspree dashboard

  ## Custom Domain

  DNS record at your registrar:
  - Type: `CNAME`
  - Name: `pitwall`
  - Value: `dhuelin.github.io`

  GitHub Pages provisions HTTPS automatically after the first deploy.
  ```

- [ ] **Step 2: Commit and push**

  ```bash
  git add README.md
  git commit -m "[#11] Add README with dev, deploy, and beta transition docs"
  git push origin main
  ```

---

## Task 12: DNS and First Deploy

> This task is manual — it requires access to your domain registrar and GitHub.

- [ ] **Step 1: Add DNS record at your domain registrar**

  - Type: `CNAME`
  - Name: `pitwall`
  - Value: `dhuelin.github.io`

  DNS propagation can take a few minutes to a few hours.

- [ ] **Step 2: Set up Formspree**

  1. Go to [formspree.io](https://formspree.io) → create free account
  2. Create a new form
  3. Copy the form ID (the part after `/f/` in the form endpoint URL)
  4. Open `src/config.ts`, set `formspreeId: 'your-id-here'`
  5. Commit:
     ```bash
     git add src/config.ts
     git commit -m "Set Formspree form ID"
     git push origin main
     ```

- [ ] **Step 3: Enable GitHub Pages on the repo**

  Go to GitHub → `pitwall-launch` → Settings → Pages → Source: Deploy from branch → Branch: `gh-pages` → Save.

  (The `gh-pages` branch will be created automatically on first deploy.)

- [ ] **Step 4: Trigger first deploy**

  Go to GitHub → Actions → Deploy → Run workflow → Run workflow.

  Wait for the workflow to complete (green check).

- [ ] **Step 5: Verify the live page**

  Open `https://pitwall.huelin.dev` in a browser.

  Check:
  - Page loads over HTTPS (cert may take a few minutes after first deploy)
  - Speed lines animate in the hero
  - Waitlist form is visible
  - Footer links work
  - Mobile view is correct (test on phone or Chrome DevTools)

- [ ] **Step 6: Test waitlist form end-to-end**

  Submit a test email on the live page. Verify you receive it at your Formspree-registered email address.
```
