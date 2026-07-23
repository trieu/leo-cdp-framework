# Roadmap — Customer 360

> Xem hiện trạng chi tiết (schema, API, use case) tại [TECHNICAL-DOCUMENTATION.md](TECHNICAL-DOCUMENTATION.md). Roadmap dưới đây được suy ra từ các hạ tầng đã sẵn sàng nhưng còn thiếu logic đi kèm (mục 9 của tài liệu kỹ thuật), sắp xếp theo mức độ ưu tiên.

## ✅ Đã hoàn thành

- Schema PostgreSQL 16 + pgvector cho CRM Journey Graph (8 vertex / 14 edge) và Customer Identity Resolution (CIR).
- Engine CIR (`identity-resolution-service/`) với quy tắc khớp **metadata-driven** (`cdp_profile_attributes`), hỗ trợ đa tenant + đa domain (`retail`/`banking`).
- Hash PII (SHA-256) trước khi lưu cho dữ liệu demo.
- **`is_hashed`/`persona_name`**: khi PII đã hash, `identity_resolution/persona.py` tự sinh nhãn `persona_name` dễ đọc, không phải PII (deterministic, không đảo ngược hash) — ràng buộc bởi CHECK constraint DB + logic Python trong `resolver.py`.
- REST API đầy đủ (`customer360-api/`, FastAPI + SQLAlchemy 2) — CRUD cho toàn bộ bảng + endpoint reporting (`/summary`, `/master-profiles/duplicates`, `/identity-graph/coverage`).
- Script sinh dữ liệu mẫu quy mô lớn (`init_sample_data.py`, 1000 raw profiles, tỷ lệ trùng lặp có kiểm soát) + demo end-to-end (`run-demo.sh`).

## 🎯 Ưu tiên ngắn hạn

- **Authentication/Authorization cho `customer360-api`**: hiện chưa có API key/OAuth2, chưa kiểm soát truy cập theo `tenant_id` — cần trước khi expose ra ngoài môi trường dev.
- **Ingestion layer thật (Kafka/PubSub/RabbitMQ → `cdp_raw_profiles_stage`)**: hiện dữ liệu chỉ được nạp qua script hoặc `POST /api/v1/raw-profiles`; cần worker ingestion thật như mô tả trong [identity-resolution.md](identity-resolution.md).
- **Real-time trigger thật**: thay thế `IdentityResolutionTrigger` (gọi tường minh) bằng cơ chế trigger DB hoặc consumer event thật (`cdp_trigger_process_new_raw_profiles`).
- **Lịch trình batch hằng ngày (2AM sweep)**: đóng gói `daily_job.py` thành cronjob/Airflow DAG chạy production thật, tích hợp với `airflow-ai-agent/`.

## 🚧 Trung hạn

- **Pipeline ML cho các cột scoring** (schema đã sẵn sàng: `churn_probability`, `predictive_clv`, `lead_conversion_probability`, `engagement_score`, `identity_confidence_score`…): huấn luyện + suy luận mô hình, ghi kết quả qua `PATCH /api/v1/master-profiles/{id}`, cập nhật `model_versions`/`scores_updated_at`.
- **Job sinh embedding tự động** cho `persona_embedding` (master profile) và `embedding` (CRM entities, `graph_edges`) — gọi LLM embedding API theo lịch hoặc theo event.
- **Dashboard vận hành CIR** dựa trên `/api/v1/reporting/*` (funnel xử lý, độ phủ identity graph, cảnh báo raw profile bị kẹt ở `status_code=1`).
- **Mở rộng quy tắc khớp**: thêm fuzzy matching thật (`fuzzy_trgm`/`fuzzy_dmetaphone`, cần `pg_trgm`/`fuzzystrmatch`) cho các trường không hash được (hiện chỉ hỗ trợ `exact` do PII đã hash).

## 🔭 Dài hạn / Tầm nhìn

- **Semantic search & lookalike audience** dựa trên `persona_embedding`/`graph_edges.embedding` — segmentation bằng ngôn ngữ tự nhiên (ví dụ: "khách hàng doanh nghiệp phần mềm, có >3 opportunity").
- **Graph-based dashboard** trực quan hoá hành trình khách hàng (Lead → Contact → Opportunity) và identity graph (raw profile ↔ master profile).
- **PostGIS / spatial intelligence**: tận dụng phần mở rộng không gian địa lý đã đề cập trong [README.md](README.md) cho các use case theo vị trí (ví dụ: `preferred_store_code`, phân khúc theo khu vực).
- **Data Quality tự động**: cảnh báo/khắc phục tự động khi `profile_completeness_score` hoặc `identity_confidence_score` thấp.