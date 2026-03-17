# ArangoDB Data Schema Document

## system_service 

Service registry for internal and external microservices, health checks, and API endpoints used by the CDP ecosystem.

### Java Code
```java
// TODO: Insert SystemService.java implementation here
```

### Sample data:
```json
{
  "serviceName": "LEO-Activation-Engine",
  "endpoint": "http://activation.leocdp.com:8080",
  "status": 1,
  "healthCheckUrl": "http://activation.leocdp.com:8080/health",
  "createdAt": "2026-03-17T05:30:00Z"
}
```

---

## system_event 

System-level audit logs tracking administrative actions, background job statuses, and security events.

### Java Code
```java
// TODO: Insert SystemEvent.java implementation here
```

### Sample data:
```json
{
  "eventSource": "UserAuth",
  "severity": "INFO",
  "message": "User superadmin logged in from 192.168.1.1",
  "timestamp": "2026-03-17T10:15:22Z"
}
```

---

## system_user 

User account management including credentials (hashed), RBAC roles, and administrative permissions.

### Java Code
```java
// TODO: Insert SystemUser.java implementation here
```

### Sample data:
```json
{
  "username": "superadmin",
  "email": "admin@leocdp.com",
  "role": "ADMIN",
  "permissions": ["all"],
  "active": true
}
```

---

## cdp_activationrule 

Pure domain model for data activation; logic defines how data is synchronized from ArangoDB to PGSQL16+ for external triggers.

### Java Code
```java
// TODO: Insert ActivationRule.java implementation here
```

### Sample data:
```json
{
  "actions": [],
  "activationType": "run-agent",
  "active": true,
  "agentId": "leo_personalization_service",
  "condition": "true",
  "name": "personalization-rule-01",
  "ownerUsername": "superadmin",
  "priority": 32,
  "schedulingTime": 0,
  "segmentId": "3oLdrUJGRB8w28poFvE2uE",
  "triggerEventName": "run_default_job"
}
```

---

## cdp_assetcategory 

Hierarchical taxonomy for organizing Digital Assets (Content, Products, and Templates).

### Java Code
```java
// TODO: Insert AssetCategory.java implementation here
```

### Sample data:
```json
{
  "name": "Email Templates",
  "description": "Marketing newsletter templates",
  "parentId": "root",
  "assetType": 1
}
```

---

## cdp_assetcontent 

Individual content fragments (HTML, Text, JSON) used for personalization and cross-channel delivery.

### Java Code
```java
// TODO: Insert AssetContent.java implementation here
```

### Sample data:
```json
{
  "title": "Welcome Email Body",
  "bodyContent": "<div>Hello {{profile.firstName}}, welcome back!</div>",
  "categoryId": "email_marketing_01",
  "mediaUrls": []
}
```

---

## cdp_assetgroup 

Logical clusters of assets for specific campaigns or localized regional marketing initiatives.

### Java Code
```java
// TODO: Insert AssetGroup.java implementation here
```

### Sample data:
```json
{
  "groupName": "Summer Sale 2026",
  "assetIds": ["asset_01", "asset_02"],
  "region": "Vietnam"
}
```

---

## cdp_assettemplate 

UI Layout definitions (Email/Web/Mobile) containing data placeholders for dynamic injection.

### Java Code
```java
// TODO: Insert AssetTemplate.java implementation here
```

### Sample data:
```json
{
  "templateName": "Newsletter-V1",
  "htmlContent": "<html>...</html>",
  "placeholders": ["firstName", "unsub_link"]
}
```

---

## cdp_campaign 

Top-level marketing container linking specific Segments to Assets and Touchpoints over a defined duration.

### Java Code
```java
// TODO: Insert Campaign.java implementation here
```

### Sample data:
```json
{
  "name": "Member Retention Q1",
  "segmentId": "loyal_customers",
  "status": "active",
  "budget": 5000.0
}
```

---

## cdp_contextsession 

Stateful session tracking that correlates multiple tracking events into a single visit window.

### Java Code
```java
// TODO: Insert ContextSession.java implementation here
```

### Sample data:
```json
{
  "profileId": "p123",
  "sessionKey": "sess_987654321",
  "firstTouchpointId": "web_store",
  "duration": 340
}
```

---

## cdp_dailyreportunit 

Pre-aggregated metrics computed daily for rapid dashboard rendering and performance tracking.

### Java Code
```java
// TODO: Insert DailyReportUnit.java implementation here
```

### Sample data:
```json
{
  "reportDate": "2026-03-16",
  "totalEvents": 150230,
  "uniqueProfiles": 1204,
  "conversionRate": 2.5
}
```

---

## cdp_agent 

Autonomous or semi-autonomous modules used to enrich profile data and execute activation goals.

### Java Code
```java
// TODO: Insert Agent.java implementation here
```

### Sample data:
```json
{
  "name": "Personalization Engine",
  "serviceUri": "javaclass:leotech.cdp.data.service.InternalAgentActivation",
  "forDataEnrichment": true,
  "status": 1
}
```

---

## cdp_dataflowstage 

Configuration and monitoring for ETL/data pipelines as they move through various CDP processing stages.

### Java Code
```java
// TODO: Insert DataFlowStage.java implementation here
```

### Sample data:
```json
{
  "stageName": "Profile-Deduplication",
  "order": 2,
  "inputSource": "raw_events",
  "active": true
}
```

---

## cdp_device 

Hardware and software fingerprinting of user devices (Mobile, Desktop, IoT) interacting with touchpoints.

### Java Code
```java
// TODO: Insert Device.java implementation here
```

### Sample data:
```json
{
  "browserName": "Chrome",
  "os": "Android",
  "deviceId": "uuid-789-xyz",
  "isMobile": true
}
```

---

## cdp_eventmetric 

Metric definitions and calculation logic for high-level event analysis (e.g., LTV, Churn Probability).

### Java Code
```java
// TODO: Insert EventMetric.java implementation here
```

### Sample data:
```json
{
  "metricName": "Purchase-Frequency",
  "calculationLogic": "count(purchase_events) / days_active",
  "minValue": 0.0
}
```

---

## cdp_eventobserver 

Listeners that monitor incoming event streams and trigger internal side-effects or system notifications.

### Java Code
```java
// TODO: Insert EventObserver.java implementation here
```

### Sample data:
```json
{
  "observerName": "Slack-Alert-High-Value-Purchase",
  "observedEvent": "order_completed",
  "minAmount": 1000.0
}
```

---

## cdp_feedbackdata 

Qualitative data captured via surveys, NPS scores, and customer reviews.

### Java Code
```java
// TODO: Insert FeedbackData.java implementation here
```

### Sample data:
```json
{
  "profileId": "p123",
  "score": 9,
  "comment": "Excellent UI and fast delivery!",
  "feedbackType": "NPS"
}
```

---

## cdp_filemetadata 

References and metadata for physical files stored in cloud storage (S3) or local repositories.

### Java Code
```java
// TODO: Insert FileMetadata.java implementation here
```

### Sample data:
```json
{
  "fileName": "customer_export_mar.csv",
  "fileSize": 2048576,
  "mimeType": "text/csv",
  "storagePath": "s3://leo-cdp/exports/"
}
```

---

## cdp_financeevent 

Monetary transaction data including payments, refunds, and currency-specific details.

### Java Code
```java
// TODO: Insert FinanceEvent.java implementation here
```

### Sample data:
```json
{
  "transactionId": "TXN_5544",
  "amount": 150.75,
  "currency": "USD",
  "status": "completed"
}
```

---

## cdp_journeymap 

Visualization and logical definitions of user paths across different CDP stages (Awareness to Advocacy).

### Java Code
```java
// TODO: Insert JourneyMap.java implementation here
```

### Sample data:
```json
{
  "journeyName": "B2B Onboarding",
  "stages": ["Sign-up", "Verify-Email", "First-Purchase"],
  "active": true
}
```

---

## cdp_notebook 

Data science notebooks (SQL/AQL/Python) used for ad-hoc analysis and segment experimentation.

### Java Code
```java
// TODO: Insert Notebook.java implementation here
```

### Sample data:
```json
{
  "title": "Customer Churn Analysis 2026",
  "queryType": "AQL",
  "content": "FOR p IN cdp_profile FILTER p.totalSpending < 10 RETURN p"
}
```

---

## cdp_productitem 

Catalog items including SKUs, pricing, inventory levels, and product attributes.

### Java Code
```java
// TODO: Insert ProductItem.java implementation here
```

### Sample data:
```json
{
  "sku": "IPHONE-15-PRO",
  "productName": "iPhone 15 Pro Max",
  "price": 1199.0,
  "categoryId": "smartphones"
}
```

---

## cdp_profile 

The Unified Customer Profile (360-degree view) containing identities, behavioral scores, and attributes.

### Java Code
```java
// TODO: Insert Profile.java implementation here
```

### Sample data:
```json
{
  "firstName": "John",
  "primaryEmail": "john.doe@example.com",
  "crmId": "CRM-7788",
  "totalSpending": 4500.50,
  "lastSeenAt": "2026-03-17T05:00:00Z"
}
```

---

## cdp_segment 

Query-based or static groupings of profiles used for targeting and data activation.

### Java Code
```java
// TODO: Insert Segment.java implementation here
```

### Sample data:
```json
{
  "name": "High Value Customers",
  "segmentType": "dynamic",
  "query": "FILTER doc.totalSpending > 1000",
  "profileCount": 540
}
```

---

## cdp_socialevent 

Interactions from social media platforms (Likes, Shares, Comments) linked to a known profile.

### Java Code
```java
// TODO: Insert SocialEvent.java implementation here
```

### Sample data:
```json
{
  "platform": "Facebook",
  "action": "share",
  "contentId": "post_summer_sale",
  "socialUsername": "@john_doe_26"
}
```

---

## cdp_targetmediaunit 

Definitions of advertising targets (Ad IDs, Placement IDs) for DSP/SSP synchronization.

### Java Code
```java
// TODO: Insert TargetMediaUnit.java implementation here
```

### Sample data:
```json
{
  "mediaSource": "Google-Ads",
  "placementId": "PL-009988",
  "costPerClick": 0.45
}
```

---

## cdp_touchpoint 

Specific digital or physical interaction points (URL, QR Code, POS, App Screen).

### Java Code
```java
// TODO: Insert Touchpoint.java implementation here
```

### Sample data:
```json
{
  "name": "Homepage Hero Banner",
  "url": "https://leocdp.com/",
  "touchpointType": "web"
}
```

---

## cdp_touchpointhub 

Logical grouping of touchpoints (e.g., "All Mobile Apps" or "Vietnam Retail Stores").

### Java Code
```java
// TODO: Insert TouchpointHub.java implementation here
```

### Sample data:
```json
{
  "hubName": "Global-E-commerce",
  "touchpointIds": ["web_us", "web_vn", "app_ios"]
}
```

---

## cdp_trackingevent 

High-volume raw event data for all customer behaviors tracked via LEO JS SDK or API.

### Java Code
```java
// TODO: Insert TrackingEvent.java implementation here
```

### Sample data:
```json
{
  "eventMetric": "page-view",
  "profileId": "p123",
  "touchpointId": "tp_99",
  "timestamp": "2026-03-17T05:33:00Z"
}
```

---

## cdp_webhookdataevent 

Raw payloads and status logs for incoming data received via third-party webhooks.

### Java Code
```java
// TODO: Insert WebhookDataEvent.java implementation here
```

### Sample data:
```json
{
  "source": "Shopify",
  "payload": {"order_id": "9988", "customer": "..." },
  "processed": true
}
```
