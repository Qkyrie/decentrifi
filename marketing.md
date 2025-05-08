# Decentrifi — Marketing Overview

---

## What we’re building

**Decentrifi** is an open‑source, chain‑agnostic analytics platform that turns raw EVM smart‑contract events into real‑time, human‑readable metrics. Paste a contract address + ABI and instantly see:

* Function‑call volume & hourly trends
* Active wallet counts & cohort growth
* Gas‑usage distributions & cost outliers
* Error/revert rates and stack‑traces
* Oracle & cross‑chain activity (Chainlink, CCIP, VRF, Automation)

All data streams continuously, no SQL required.

---

## Why we’re building it

1. **Transparency is broken.** Most on‑chain analytics live behind paywalls or proprietary dashboards. Builders can’t easily prove traction; users can’t audit protocol health.
2. **DIY costs time & money.** Spinning up archive nodes, decoding logs, and scaling databases distracts founders from shipping product.
3. **Data ≠ insight.** Raw events are noisy; founders need clear KPIs to iterate fast and raise capital.
4. **Public good flywheel.** Open metrics improve security, foster academic research, and spotlight ecosystem growth—benefiting everyone, not just paying customers.

---

## Technology

| Layer                  | Stack                              | Highlights                                                              |
| ---------------------- | ---------------------------------- | ----------------------------------------------------------------------- |
| **Ingestion**          | Kotlin + Alchemy WebSocket streams | Lossless capture of Transfer & custom events across any EVM chain       |
| **Storage**            | PostgreSQL 14 + TimescaleDB        | Time‑series compression & hyperfunctions for blazing‑fast roll‑ups      |
| **Aggregation**        | Kotlin jobs (hourly)               | Call counts, gas buckets, error histograms, active‑wallet deduplication |
| **API**                | REST & GraphQL (Ktor)              | Public endpoints for programmatic access                                |
| **Dashboard**          | Kotlin + Thymeleaf SSR, Chart.js   | Lightweight, SEO‑friendly, no client‑side framework bloat               |
| **Alerting (roadmap)** | Webhooks + Chainlink Functions     | Threshold & ML‑based anomaly detection pushed on‑chain or off‑chain     |

---

## Public‑good value proposition

* **MIT‑licensed code** — fork it, audit it, improve it.
* **Free hosted explorer** — community dashboards for major protocols & Chains.
* **Data export** — CSV/Parquet downloads for researchers and journalists.
* **Grant‑funded** — infrastructure subsidies keep core features free for everyone.

---

## Self‑hosting & open source

* One‑command Docker Compose deploy (Postgres, TimescaleDB, Decentrifi services).
* Helm chart for Kubernetes.
* Configurable chains: Ethereum, Polygon, Arbitrum, Optimism, Base, Gnosis.
* Bring‑your‑own Alchemy/Infura/RPC keys.
* Built‑in migration scripts & seed data.
* Community‑maintained plug‑ins for non‑EVM chains coming soon.

---

## Competitor comparison

| Feature                    | **Decentrifi**                 | Tenderly                     | Dune                    | Nansen            | Google Analytics (web) |
| -------------------------- | ------------------------------ | ---------------------------- | ----------------------- | ----------------- | ---------------------- |
| **Open source**            | ✅ MIT                          | ❌                            | ❌ (proprietary queries) | ❌                 | ✅                      |
| **Self‑hostable**          | ✅                              | ❌                            | ❌                       | ❌                 | ✅                      |
| **Chain coverage (EVM)**   | All (plug‑in)                  | 15+                          | 15+                     | 8                 | N/A                    |
| **Near‑real‑time metrics** | ✅ (<1 min)                     | ✅                            | ❌ (batch)               | ✅                 | ✅                      |
| **No‑SQL required**        | ✅                              | ❌ (requires writing queries) | ❌                       | ✅                 | ✅                      |
| **Pricing**                | Free / self‑host; cost = infra | SaaS \$                      | Freemium \$             | Subscription \$\$ | Free                   |
| **Public‑good focus**      | 🎯 Primary mission             | Tooling business             | Data marketplace        | Token analytics   | Web only               |

---

## 1‑minute pitch

> “Decentrifi is the open‑source ‘Google Analytics’ for smart contracts. Drop in any EVM address & ABI and watch real‑time dashboards light up with call counts, active wallets, gas spikes, and error traces—no SQL, no node maintenance. By putting these insights in the public domain, we give every builder, DAO, and researcher the data they need to ship safer, faster, and fairer Web3 products.”

---

## Long‑form pitch

The blockchain promised radical transparency, yet most usage data sits locked behind proprietary analytics dashboards or expensive nodes. Founders are forced to choose between flying blind or burning precious runway on data infrastructure that doesn’t move the product forward.

**Decentrifi fixes this.** Built as a public good from day one, our Kotlin‑powered pipeline streams contract events straight into a time‑series database, auto‑rolls them into product‑grade KPIs, and serves them through an SSR dashboard that even non‑technical stakeholders understand. It’s chain‑agnostic, self‑hostable, and MIT‑licensed, so no vendor can ever yank the rug. Grant funding covers the heavy lifting—archive‑node bandwidth, TimescaleDB storage—allowing us to keep the core hosted explorer free for the ecosystem.

Our roadmap goes beyond pretty charts: anomaly alerts piped through Chainlink Functions, segmentation for wallet cohorts, cross‑chain comparatives, AI‑driven anomaly detection. Every feature feeds into the same goal—**turn raw events into actionable insights for everyone, not just whales.**

---

## Taglines

* “Analytics for every address, insight for every builder.”
* “Turn on‑chain noise into product‑grade signal.”
* “Open dashboards, open data, open future.”
* “See your contract like users do—live.”

---

## Oneliners

* “Paste an address, get instant on‑chain analytics.”
* “The public good analytics layer for EVM contracts.”
* “Google Analytics, but for smart contracts. Open source.”
* “Real‑time insights that fit on a README badge.”