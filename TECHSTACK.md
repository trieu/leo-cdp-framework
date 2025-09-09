# LEO CDP Tech Stack - version 2.0

## **Slide 1 – LEO CDP tech stack**

**LEO CDP Tech Stack Overview**
Introduction to the core technologies powering **LEO CDP** for Customer 360 and AI-driven personalization.

---

## **Slide 2 – NoSQL Database: ArangoDB 3.11**

**Introduction:** Multi-model database (Document + Graph + Search).

**Purpose:**

* Store customer events, relationships, and unstructured data.
* Power graph-based identity resolution and journey mapping.

---

## **Slide 3 – RDBMS + Vector DB: PostgreSQL 16 + pgvector**

**Introduction:** Relational database extended with vector similarity search.

**Purpose:**

* Serve as **golden record storage** (master profiles).
* Enable AI-driven **semantic search & recommendations** with pgvector.

---

## **Slide 4 – Data Cache: Redis**

**Introduction:** In-memory data store for caching and fast lookups.

**Purpose:**

* Accelerate identity resolution (e.g., visitor → profile).
* Store **real-time persona profiles** for fast API responses.

---

## **Slide 5 – Web API: FastAPI + OpenID Connect 1.0**

**Introduction:** Python-based web API with secure authentication.

**Purpose:**

* Provide API endpoints for data ingestion and retrieval.
* Secure access with **OIDC-based Single Sign-On**.

---

## **Slide 6 – Data Workflow: Apache Airflow**

**Introduction:** Workflow orchestration platform.

**Purpose:**

* Automate ETL/ELT between ArangoDB, PostgreSQL, and external sources.
* Schedule **data enrichment, segmentation, and scoring** jobs.

---

## **Slide 7 – Data Report: Apache Superset**

**Introduction:** Open-source BI and visualization tool.

**Purpose:**

* Build dashboards for **Customer 360 insights**.
* Enable business teams to track KPIs, segments, and campaign results.

---

## **Slide 8 – AI Agent Framework: LangChain & LangGraph**

**Introduction:** AI agent orchestration frameworks.

**Purpose:**

* Build reasoning workflows for customer queries and marketing decisions.
* Chain together **LLMs, APIs, and databases** for advanced automation.

---

## **Slide 9 – Generative AI: Gemini, OpenAI, OSS Models**

**Introduction:** Large Language Models from cloud + open source.

**Purpose:**

* Summarize customer interactions.
* Generate campaign content and personalized recommendations.
* Run on cloud (Gemini/OpenAI) or **quantized models** for cost efficiency.

---

## **Slide 10 – API Testing: Python + Pytest**

**Introduction:** Automated API test framework.

**Purpose:**

* Ensure **stability and reliability** of ingestion and profile APIs.
* Run regression tests in CI/CD pipeline.

---

## **Slide 11 – Markdown and MkDocs**

**Introduction:** Automated documentation pipelines for modern AI-driven development.

**Purpose:**

* Auto-generate project documentation straight from source code, tasks, and specs.
* Ensure AI agent specifications, API contracts, and workflows are always synchronized with the codebase.
* Provide a living knowledge base for engineers, product owners, and AI agents.
* Enable continuous publishing with MkDocs → fast, searchable docs for both humans and machines.

---

## **Slide 12 – Docker & Docker Compose**

**Introduction:** Containerization platform.

**Purpose:**

* Package and deploy AI agents and data services consistently.
* Simplify **multi-service orchestration** for local dev and cloud deployment.
