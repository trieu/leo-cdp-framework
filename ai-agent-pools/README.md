# AI Agent Pools

🚀 **AI Agent Pools** is an event-driven orchestration framework for running autonomous AI agents on top of **LEO CDP** using **Dagster**. It transforms customer interactions, behavioral events, and business signals into intelligent workflows that enrich profiles, generate insights, make decisions, and automate actions in real time or batch.

Unlike traditional ETL pipelines, AI Agent Pools treats every event as an opportunity for AI reasoning and orchestration.

---

# Architecture

```
                +---------------------+
                |   Mobile / Web App  |
                +----------+----------+
                           |
                    Customer Events
                           |
                    +------+------+
                    |   LEO CDP    |
                    +------+------+
                           |
          Redis Pub/Sub / Kafka / Webhooks
                           |
          +----------------+----------------+
          |                                 |
          |        Dagster Orchestrator     |
          |                                 |
          +----------------+----------------+
                           |
          +----------------+----------------+
          |                |                |
   AI Classification  AI Scoring   AI Recommendation
          |                |                |
          +----------------+----------------+
                           |
                   AI Agent Pools
                           |
     +----------+----------+-----------+-----------+
     |          |          |           |           |
 Customer360  PostgreSQL  pgvector   ArangoDB   LLM APIs
```

---

# Core Capabilities

* **Event-Driven AI Orchestration**

  * Trigger AI workflows from customer events, business events, scheduled jobs, or external APIs.
  * Support real-time, asynchronous, and batch execution.

* **AI Agent Execution**

  * Execute multiple specialized AI agents in parallel or sequentially.
  * Chain reasoning across multiple agents using Dagster dependency graphs.

* **Customer 360 Intelligence**

  * Load unified customer profiles from LEO CDP.
  * Perform identity resolution and profile enrichment.
  * Update customer attributes and predictive scores.

* **Semantic Search**

  * Store and retrieve embeddings with PostgreSQL 16 + pgvector.
  * Enable Retrieval-Augmented Generation (RAG) over customer knowledge.

* **Graph Intelligence**

  * Traverse customer relationships and journeys using ArangoDB.
  * Analyze communities, influence networks, and behavioral paths.

* **LLM Integration**

  * Google Gemini
  * OpenAI GPT
  * Anthropic Claude
  * Open-source models through Ollama or vLLM

* **Workflow Orchestration**

  * Dagster Jobs
  * Assets
  * Sensors
  * Schedules
  * Dynamic Pipelines

---

# AI Agent Types

AI Agent Pools can host many specialized agents, including:

| Agent                         | Responsibility                       |
| ----------------------------- | ------------------------------------ |
| Customer Profile Agent        | Enrich Customer 360 profiles         |
| Segmentation Agent            | Build dynamic audience segments      |
| Recommendation Agent          | Recommend products or content        |
| Lead Scoring Agent            | Predict conversion probability       |
| Customer Lifetime Value Agent | Estimate CLV                         |
| Churn Prediction Agent        | Detect churn risk                    |
| Journey Analysis Agent        | Analyze customer journeys            |
| Next Best Action Agent        | Recommend optimal actions            |
| Campaign Optimization Agent   | Improve campaign performance         |
| Data Quality Agent            | Validate and clean incoming data     |
| Knowledge Agent               | Search enterprise knowledge with RAG |

---

# Event Sources

AI Agent Pools can consume events from:

* LEO CDP Event Stream
* Redis Pub/Sub
* Kafka
* REST APIs
* Webhooks
* CSV / Excel Imports
* Amazon S3
* Google Cloud Storage
* Database Change Data Capture (CDC)
* Cron/Scheduled Jobs

---

# Data Platform

The framework is designed around a modern AI-native data stack.

| Component      | Purpose                                 |
| -------------- | --------------------------------------- |
| PostgreSQL 16  | Customer 360 and operational data       |
| pgvector       | Vector embeddings and semantic search   |
| ArangoDB       | Graph relationships and knowledge graph |
| Redis          | Event queue, Pub/Sub, cache             |
| DuckDB         | Local analytics and feature engineering |
| Apache Iceberg | Data lake tables (optional)             |
| Dagster        | Workflow orchestration                  |

---

# AI Providers

Pluggable AI providers allow the same workflow to run on different models.

* Google Gemini
* OpenAI
* Anthropic Claude
* Ollama
* Hugging Face
* vLLM

---

# Why Dagster?

Dagster provides capabilities that align well with AI workflows:

* Asset-based data orchestration
* Event-driven sensors
* Strong typing and validation
* Retry and failure recovery
* Dynamic pipelines
* Software-defined assets
* Observability and lineage
* Local development with `execute_in_process()`
* Scalable deployment on Docker or Kubernetes

---

# Example Workflow

```
Customer Login Event
        │
        ▼
Redis Pub/Sub
        │
        ▼
Dagster Sensor
        │
        ▼
Load Customer Profile
        │
        ▼
Embedding Search
        │
        ▼
AI Classification
        │
        ▼
Lead Scoring
        │
        ▼
Next Best Action
        │
        ▼
Update Customer360
        │
        ▼
Trigger Marketing Automation
```

---

# Design Principles

* **Event-Driven** — Every business event can initiate intelligent processing.
* **AI-Native** — AI agents are first-class workflow components, not add-ons.
* **Composable** — Agents can be combined into reusable pipelines.
* **Observable** — Every execution is tracked, logged, and versioned.
* **Scalable** — Supports local development, distributed execution, and cloud-native deployment.
* **Model-Agnostic** — Easily switch between commercial and open-source LLMs.
* **Extensible** — Add new agents, data sources, and AI providers with minimal changes.

AI Agent Pools turns **LEO CDP** into an intelligent orchestration layer where customer events continuously drive AI-powered decision-making, profile enrichment, personalization, and automated business actions.
