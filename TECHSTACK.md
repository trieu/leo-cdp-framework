# **LEO CDP Tech Stack – Version 2.0**

## **Slide 1 – LEO CDP Tech Stack Overview**

**LEO CDP 2.0** được xây dựng trên nền tảng open source, tối ưu cho Customer 360, real-time personalization và AI-first automation.
Nền tảng kết hợp Graph DB, Vector DB, Data API, AI Agents và Generative AI để tạo nên một CDP hiện đại, nhanh, mở và mở rộng dễ dàng.

### Flow
Events → ArangoDB → Airflow Pipelines → PostgreSQL → Vector Embedding → Real-Time Cache → FastAPI → AI Agents → Data API → Plotly.js Visualization

Mang lại khả năng:

- Customer 360 real-time
- AI-driven personalization
- Automation theo hành trình khách hàng
- Reporting & insights ngay trong hệ thống

---

## **Slide 2 – NoSQL Database: ArangoDB 3.11**

**Introduction:** Multi-model database (Document + Graph + Search).

**Purpose:**
• Lưu trữ sự kiện, quan hệ khách hàng, hồ sơ hành trình.
• Dùng graph để hỗ trợ **identity resolution**, tìm cộng đồng, phân tích hành vi.
• Hỗ trợ search tốc độ cao cho log và event stream.

---

## **Slide 3 – RDBMS + Vector DB: PostgreSQL 16 + pgvector**

**Introduction:** Relational + Vector Search in one system.

**Purpose:**
• Là **Golden Record Store** cho master profiles.
• Tích hợp vector embeddings để phục vụ semantic search, recommendation, lookalike modeling.
• Lưu bảng phân khúc, KPI và dữ liệu tương tác đã chuẩn hóa.

---

## **Slide 4 – Data Cache: Redis**

**Introduction:** In-memory key-value store.

**Purpose:**
• Tăng tốc xác định danh tính (mapping visitor → profile).
• Giữ **Real-Time Persona** cho API personalization.
• Cache segmentation & scoring để giảm tải xuống DB.

---

## **Slide 5 – Keycloak SSO (OpenID Connect 1.0)**

**Introduction:** High-performance API layer with enterprise-grade authentication.

**Purpose:**
• Tích hợp **Keycloak SSO** theo chuẩn **OpenID Connect 1.0**.
• Quản lý người dùng, roles, policies cho toàn hệ thống.
• Hỗ trợ multi-tenant realm, client credentials, token exchange.

---

## **Slide 6 – Data Workflow: Apache Airflow**

**Introduction:** Workflow orchestration engine.

**Purpose:**
• Điều phối ETL/ELT giữa ArangoDB, PostgreSQL, Cloud Storage.
• Chạy các job enrichment, scoring, ML training theo lịch.
• Giám sát pipeline và cảnh báo lỗi theo real-time.

---

## **Slide 7 – Data Report: Data API + SQL Gemini Agent + Plotly.js**

**Introduction:** AI-powered analytics system.

**Purpose:**
• Data API cung cấp lớp truy vấn chuẩn hóa cho mọi nguồn dữ liệu.
• **SQL Gemini Agent** tự sinh câu SQL, join bảng, tối ưu truy vấn theo ngữ cảnh.
• Trình bày kết quả bằng **Plotly.js** – biểu đồ tương tác nhúng vào Admin UI.
• Tạo báo cáo theo yêu cầu, không cần BI dashboard truyền thống.
• Hỗ trợ exploratory analytics trực tiếp ngay trong CDP.

---

## **Slide 8 – AI Agent Framework: LangChain & LangGraph**

**Introduction:** Framework orchestration cho AI Agents.

**Purpose:**
• Xây dựng reasoning engine cho Marketing AI, Customer Query AI.
• Liên kết LLMs, APIs, DBs, Event Stream thành workflow thống nhất.
• Tạo automation thông minh như: smart audience builder, anomaly detection, campaign optimization.

---

## **Slide 9 – Generative AI: Gemini, OpenAI, OSS Models**

**Introduction:** Hybrid Generative AI infrastructure.

**Purpose:**
• Tóm tắt hành vi khách hàng.
• Tạo nội dung marketing, email, ads cá nhân hóa.
• Chạy LLM trên cloud (Gemini/OpenAI) hoặc **quantized local models** để tối ưu chi phí.
• Dùng embedding model để cải thiện segmentation & intent detection.

---

## **Slide 10 – API Testing: Python + Pytest**

**Introduction:** Automated testing suite.

**Purpose:**
• Bảo đảm độ ổn định cho API ingestion, tracking, segmentation.
• Chạy test tự động trong CI/CD mỗi lần release.
• Theo dõi hiệu năng API và regression.

---

## **Slide 11 – Markdown + MkDocs (Documentation Engine)**

**Introduction:** Documentation as code.

**Purpose:**
• Sinh tự động tài liệu từ source code, comments, AI specs.
• Publish docs dạng Website với MkDocs – nhanh, đẹp, dễ tìm kiếm.
• Docs luôn cập nhật theo thay đổi trong hệ thống.
• Là knowledge base cho engineers, PMs và AI Agents.

---

## **Slide 12 – Docker & Docker Compose**

**Introduction:** Containerized environment.

**Purpose:**
• Đóng gói dịch vụ AI, API, DB thành container dễ triển khai.
• Hỗ trợ môi trường local dev + staging + production đồng nhất.
• Quản lý multi-service stack chỉ với 1 file compose.

