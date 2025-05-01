# decentrifi MVP

## What We’re Building

`decentrifi` is a lightweight, end-to-end analytics platform for smart contracts (starting with a single ERC‑20 token). The MVP includes:

- **Data Pipeline** (`decentrifi` repo)
    - **Ingestion** of `Transfer` events from an ERC‑20 contract (DAI on Ethereum mainnet)
    - **Raw Storage** of decoded invocations in PostgreSQL+TimescaleDB
    - **Hourly Aggregation** into summary metrics
        - Function call counts (per selector)
        - Transaction error/revert rate
        - Gas usage distribution
        - Active wallets count
- **Web Application** (`analytics-api`)
    - Server-side rendered dashboard (Kotlin + Thymeleaf)
    - Four interactive charts using Chart.js:
        1. Calls per function over time
        2. Error/revert rate over time
        3. Gas usage histogram
        4. Daily active wallets
    - REST endpoints for metrics
    - Responsive UI with date-range controls

The system is designed to support **any EVM chain** and **arbitrary ABIs** in future iterations.

## 3-Week MVP Roadmap

### Week 1 – Core Ingestion & Storage

| Day   | Focus & Deliverables                                                                                     |
|-------|----------------------------------------------------------------------------------------------------------|
| **1** | **Kickoff & Scope**  
- Choose DAI/mainnet
- Define metrics and data model
- Sketch infra diagram                                     |
  | **2** | **Project Setup & Local Stack**
- Initialize Git & Gradle modules (`ingestor`, `api`)
- Docker Compose for Postgres, Redis, ingestor, api
- Helm boilerplate                                      |
  | **3–4** | **Ingestion PoC → Worker**
- Kotlin client for `eth_getLogs` (Transfer events)
- Ktor service polls new blocks and writes to `raw_invocations` |
  | **5** | **Aggregation Logic**
- SQL/Kotlin code to roll up hourly metrics into `metrics_agg`
- Backfill ~1,000 blocks                                     |
  | **6–7** | **Error Handling & Tests**
- Implement retry/backoff on rate limits
- Unit/integration tests (Testcontainers)
- Sanity-check vs. Etherscan                                 |

### Week 2 – API, SSR & Charts

| Day       | Focus & Deliverables                                                                                   |
|-----------|--------------------------------------------------------------------------------------------------------|
| **8**     | **API Endpoints**  
- `GET /api/metrics/{metric}?from=&to=`
- `GET /api/overview`                                     |
  | **9**     | **SSR Template**
- Thymeleaf layout and `/dashboard/{contract}` template
- Embed initial JSON data                     |
  | **10–11** | **Chart Integration**
- Chart.js for calls, errors, gas, active wallets
- AJAX date-range picker                       |
  | **12**    | **UI Polish & Responsiveness**
- Mobile layout tweaks
- Loading states and error handling                    |
  | **13–14** | **Optional Auth & Cleanup**
- (Beta only) GitHub OAuth
- Validate inputs and finalize docs                   |

### Week 3 – Deployment, Observability & Beta

| Day        | Focus & Deliverables                                                                                      |
|------------|-----------------------------------------------------------------------------------------------------------|
| **15**     | **Docker & K8s Deployment**  
- Multi-stage Dockerfiles
- Helm manifests for CronJob (ingestor) and Deployment (api) |
  | **16**     | **Cluster Setup & Release**
- Deploy to managed or local k3d cluster
- Managed Postgres/TimescaleDB
- TLS via cert-manager      |
  | **17**     | **Observability**
- Prometheus metrics & Grafana dashboards
- Alerts for ingestion failures & API errors    |
  | **18**     | **End-to-End QA & Backfill**
- Verify data accuracy
- Run full backfill (30 days)                             |
  | **19**     | **Beta Launch Prep**
- Open sign-up or remove auth
- Add lightweight analytics (Plausible/PostHog)           |
  | **20**     | **Public Beta Go-Live**
- Announce on forums/Discord/Twitter
- Monitor first users                          |
  | **21**     | **Retrospective & Next Steps**
- Review KPIs (users, contracts, errors)
- Prioritize v2 features (alerts, multi-token)    |

---

_By the end of Week 3, you’ll have a live ERC‑20 analytics dashboard, showcasing core developer insights and a modular pipeline ready for future expansion._

