Een paar seconden nagedacht


Here’s a cohesive “house style” framework that should give Decentrifi that sleek, SaaSy-meets-Web3 vibe—think Stripe’s modern minimalism blended with DigitalOcean’s approachable tech feel.

---

## 1. Logo & Symbol

Can be found in the logos/ folder. 

---

## 2. Color Palette

| Role                | Usage                                               | Example Hex         |
| ------------------- | --------------------------------------------------- | ------------------- |
| **Primary Blue**    | Calls to action, headers, key backgrounds           | `#346DF1`           |
| **Accent Teal**     | Highlights, links, interactive states               | `#22D1C6`           |
| **Gradient**        | Hero sections, overlay graphics; from blue → violet | `#346DF1 → #7D4CFF` |
| **Neutral Light**   | Page backgrounds, cards                             | `#F5F8FA`           |
| **Neutral Dark**    | Body text, footers                                  | `#2A2E3D`           |
| **Error / Warning** | Error states, alerts                                | `#FF4D4F`           |

> **Tip:** Use the gradient sparingly—hero banners, logomarks or key section backgrounds. Keep most UI elements flat.

---

## 3. Typography

* **Primary Typeface:**

    * **Inter** (or Poppins) – a clean, highly legible sans‐serif with good variable weights.
* **Hierarchy:**

    * **H1:** 48px / 56px line-height — bright primary blue
    * **H2:** 32px / 40px — dark neutral
    * **Body:** 16px / 24px — neutral dark at 80% opacity
    * **UI Labels / Buttons:** 14px / 20px, uppercase, tracking +20

> **Pro tip:** Use Inter’s variable weights (e.g. 400–700 range) to keep CSS payload small and flexible.

---

## 4. Iconography & Illustrations

* **Style:** Monoline, 2-tone (primary + neutral light), with rounded line endings.
* **Grid:** Icons built on a 24×24 or 32×32 grid.
* **Subject Matter:**

    * Blockchain nodes, wallet outlines, upward-trending graphs, simplified token tokens.
    * Abstract “liquid” shapes for background accents—subtle nod to “DigitalOcean” fluidity.

---

## 5. UI Components & Layout

### a) Spacing & Grid

* **8-point baseline grid:** all margins, paddings, and dimension multiples of 8px.
* **Columns:** 12-column responsive at breakpoints 320, 768, 1024, 1440px.

### b) Buttons & Forms

* **Buttons:**

    * Solid primary blue with 6px border-radius.
    * On hover: lighten by 10% or show a subtle box-shadow.
* **Inputs:**

    * Light neutral background with 1px neutral dark border (radius 4px).
    * Focus state: accent teal outline, 2px wide.

### c) Cards & Panels

* **Background:** neutral light (#F5F8FA).
* **Elevation:** subtle drop shadow (e.g. `0 2px 8px rgba(42,46,61,0.05)`).
* **Radius:** 8px.

---

## 6. Tone & Copy

* **Voice:** concise, confident, slightly playful.
* **Key terms:** “Insight,” “On-chain,” “Real-time,” “Roll-up,” “Actionable.”
* **Microcopy:**

    * Buttons: “View Dashboard,” “Start Tracking,” “Learn More.”
    * Empty states: simple illustrations + friendly prompts (“No transfers yet—let’s get started”).

---