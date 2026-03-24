
## system_service

Configuration for core system services, orchestration via Airflow DAG, and integration layer for external/internal APIs in CDP.

### 📦 SystemService Attributes

id (String)

* Primary key (_key in ArangoDB), unique service identifier

name (String)

* Service name

description (String)

* Human-readable description of the service

dagId (String)

* Airflow DAG ID used for orchestration workflows

index (int)

* Ordering / execution priority index

createdAt (Date)

* Creation timestamp

updatedAt (Date)

* Last update timestamp

status (int)

* Service state: 0 = enabled, 1 = ready, -1 = disabled

configs (Map<String, Object>)

* Dynamic configuration parameters (API keys, endpoints, tokens, runtime configs)

coreFieldConfigs (Map<String, AttributeMetaData>)

* Standard CDP field definitions (e.g., email, customer_id, phone)

extFieldConfigs (Map<String, AttributeMetaData>)

* Extended/custom fields per tenant or specific use-case

### Sample data:

```json
{
  "id": "email_service",
  "name": "Email Marketing Service",
  "description": "Service for sending marketing emails and campaign automation",
  "dagId": "email_marketing_dag",
  "index": 1,
  "createdAt": "2025-01-01T10:00:00.000Z",
  "updatedAt": "2025-03-01T12:00:00.000Z",
  "status": 0,
  "configs": {
    "service_provider": "smtp",
    "service_api_url": "https://api.emailservice.com/send",
    "service_api_key": "your_api_key_here",
    "service_api_token": "your_token_here"
  },
  "coreFieldConfigs": {
    "email": {
      "type": "string",
      "required": true
    },
    "customer_id": {
      "type": "string",
      "required": true
    }
  },
  "extFieldConfigs": {
    "first_name": {
      "type": "string"
    },
    "last_name": {
      "type": "string"
    }
  }
}
```

---

## system_event

System tracked event for auditing, monitoring scheduled jobs, admin actions, and system-level activities within the CDP.

### 📦 SystemEvent Attributes

id (String)

* Primary key (_key in ArangoDB), generated as hashed ID based on event data

loginUsername (String)

* Username of the actor who triggered the event

createdAt (Date)

* Timestamp when the event was created

objectName (String)

* Name of the target object (e.g., class name, entity type)

objectId (String)

* Identifier of the target object

action (String)

* Action performed (e.g., CREATE, UPDATE, DELETE, LOGIN, RUN_JOB)

data (String)

* Additional payload or metadata associated with the event (JSON/string)

accessIp (String)

* IP address of the user or system triggering the event

userAgent (String)

* User agent string (browser, bot, or system identifier)

### Sample data:

```json
{
  "id": "a8f3c9d1e7b2f4a6",
  "loginUsername": "admin_user",
  "createdAt": "2026-03-20T10:15:30.000Z",
  "objectName": "CustomerProfile",
  "objectId": "cust_123456",
  "action": "UPDATE",
  "data": "{\"field\":\"email\",\"old\":\"old@email.com\",\"new\":\"new@email.com\"}",
  "accessIp": "192.168.1.10",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/122.0.0.0"
}
```

---

## system_user

System user entity for authentication, authorization, and access control across CDP, including SSO integration, role-based permissions, and data governance.

### 📦 SystemUser Attributes

key (String)

* Primary key (_key in ArangoDB), unique user identifier

userLogin (String)

* Unique login username (can be SSO-based with prefix `sso_`)

profileVisitorId (String)

* مرتبط visitor profile ID for mapping user ↔ CDP profile

displayName (String)

* Full name / display name of the user

role (int)

* Role of user (RBAC): mapped via `SystemUserRole` (e.g., SUPER_ADMIN, DATA_ADMIN, DATA_OPERATOR, etc.)

userEmail (String)

* Email address of the user (unique index)

status (int)

* Account status:

  * 0 = pending
  * 1 = active
  * 2 = disabled
  * 3 = expired

avatarUrl (String)

* URL of user avatar/profile image

creationTime (long)

* Account creation timestamp (epoch millis)

modificationTime (long)

* Last modification timestamp (epoch millis)

registeredTime (long)

* Registration timestamp (epoch millis)

businessUnit (String)

* Organization / department (e.g., data, marketing)

inGroups (Set<String>)

* List of group memberships (e.g., departments, teams)

isOnline (boolean)

* Online status of the user

networkId (long)

* Tenant / network identifier (multi-tenant isolation)

customData (Map<String, String>)

* Custom key-value metadata (e.g., position, tags)

accessProfileFields (List<String>)

* List of allowed profile fields user can access (field-level security)

userPass (String)

* Hashed password (AES-based hashing with username salt)

activationKey (String)

* Key for account activation / password reset

ssoSource (String)

* SSO provider source (e.g., Keycloak realm URL)

notifications (List<Notification>)

* List of system/user notifications

viewableJourneyMapIds (List<String>)

* Journey map IDs that user has read/view access

editableJourneyMapIds (List<String>)

* Journey map IDs that user has edit permissions

### Sample data:

```json
{
  "key": "2560057456798535700",
  "userLogin": "sso_tantrieuf31@gmail.com",
  "profileVisitorId": "",
  "displayName": "Trieu Nguyen",
  "role": 4,
  "userEmail": "tantrieuf31@gmail.com",
  "status": 1,
  "avatarUrl": "",
  "creationTime": 1764497981130,
  "modificationTime": 1764614719142,
  "registeredTime": 0,
  "businessUnit": "data",
  "inGroups": [
    "data_engineering_team"
  ],
  "isOnline": false,
  "networkId": 2560057456798535700,
  "customData": {
    "position": "dev"
  },
  "accessProfileFields": [],
  "userPass": "0c02ae0f25953859d201bb2bc0ea62d6b3711b99",
  "activationKey": "fc76951af2fc083d7a60dea477fcb93a3161e9b6",
  "ssoSource": "https://leoid.example.com/#master#leocdp",
  "notifications": [],
  "viewableJourneyMapIds": [
    "49KsZ7gW8BlNegsk6GrqfS",
    "4YL1kjgL6CNrQYV2rzBKN3",
    "id_default_journey",
    "3UQJspMPNywc2UToNC7YN",
    "zATO2rnyFohL3mhIFlROe",
    "5XnPOnoPnTRzWdcILPum9I"
  ],
  "editableJourneyMapIds": [
    "49KsZ7gW8BlNegsk6GrqfS",
    "4YL1kjgL6CNrQYV2rzBKN3",
    "id_default_journey",
    "3UQJspMPNywc2UToNC7YN",
    "zATO2rnyFohL3mhIFlROe",
    "5XnPOnoPnTRzWdcILPum9I"
  ]
}
```

---

## cdp_activationrule

Activation rule engine for CDP, defining when and how agents are triggered based on conditions, schedules, or events.

### 📦 ActivationRule Attributes

id (String)

* Primary key (_key in ArangoDB), hashed from rule configuration

createdAt (Date)

* Creation timestamp

updatedAt (Date)

* Last update timestamp

name (String)

* Unique rule name

activationType (String)

* Type of activation (e.g., `scheduling_task`, `event_trigger_task`, `run-agent`)

description (String)

* Human-readable description of the rule

priority (int)

* Execution priority (higher value = higher priority)

active (boolean)

* Rule status (true = enabled, false = disabled)

condition (String)

* Logical condition to evaluate (default: `"true"`)

actions (List<String>)

* List of actions to execute when rule is triggered

purpose (String)

* Business purpose (e.g., personalization, marketing automation)

agentId (String)

* Target agent/service to execute

segmentId (String)

* Target customer segment ID

assetType (int)

* Asset type identifier (e.g., content type, campaign type)

assetTemplateId (String)

* Reference to asset template ID

assetTemplateName (String)

* Name of the asset template

agentName (String)

* Human-readable agent name

schedulingTime (int)

* Scheduling mode:

  * "< 0" → manual trigger
  * "= 0" → polling loop
  * "> 0" → fixed interval execution

timeUnit (int)

* Time unit for scheduling:

  * 0 = seconds
  * 1 = minutes
  * 2 = hours
  * 4 = days
  * 5 = weeks

timeToStart (String)

* Start time (HH:mm format) for scheduled execution

triggerEventName (String)

* Event name that triggers execution (for event-driven rules)

ownerUsername (String)

* Owner of the rule (default: superadmin)

### Sample data:

```json
{
  "id": "auto_generated_hash_id",
  "name": "personalization-leo_personalization_service-3oLdrUJGRB8w28poFvE2uE",
  "activationType": "run-agent",
  "description": "",
  "priority": 32,
  "active": true,
  "condition": "true",
  "actions": [],
  "purpose": "personalization",
  "agentId": "leo_personalization_service",
  "agentName": "",
  "segmentId": "3oLdrUJGRB8w28poFvE2uE",
  "assetType": -1,
  "assetTemplateId": "",
  "assetTemplateName": "",
  "schedulingTime": 0,
  "timeUnit": 0,
  "timeToStart": "",
  "triggerEventName": "run_default_job",
  "ownerUsername": "superadmin",
  "createdAt": "2026-03-11T09:49:24.942Z",
  "updatedAt": "2026-03-11T09:49:24.956Z"
}
```

---

## cdp_assetcategory

Digital asset category used to organize and classify assets (e.g., content items, product catalogs, campaigns) within the CDP.

### 📦 AssetCategory Attributes

id (String)

* Primary key (_key in ArangoDB), inherited from TaxonomyNode

name (String)

* Category name

description (String)

* Description of the category

assetType (int)

* Type of asset associated with this category (e.g., content item, campaign, product catalog)

navigationOrder (int)

* Ordering index for UI navigation and display

parentId (String)

* Parent category ID (for hierarchical taxonomy structure)

networkId (long)

* Tenant / network identifier

slug (String)

* URL-friendly identifier (usually derived from name + networkId)

privacyStatus (int)

* Privacy level of the category (e.g., public, private, restricted)

headlineImageUrl (String)

* Representative image URL for the category

viewerIds (List<String>)

* List of user IDs allowed to view this category

customData (Map<String, Object>)

* Additional metadata for extensibility

createdAt (Date)

* Creation timestamp

updatedAt (Date)

* Last update timestamp

creationTime (long)

* Creation time in epoch milliseconds

modificationTime (long)

* Last modification time in epoch milliseconds

### Sample data:

```json id="n7k2s9"
{
  "id": "auto_generated_id",
  "name": "Product Item Catalogs",
  "description": "All product calalogs for Personalization Engine and Direct Marketing",
  "assetType": 2,
  "navigationOrder": 3,
  "parentId": "",
  "networkId": 10000,
  "slug": "10000-product-item-catalogs",
  "privacyStatus": 0,
  "headlineImageUrl": "",
  "viewerIds": [],
  "customData": {},
  "createdAt": "2025-10-22T06:47:12.868Z",
  "updatedAt": "2025-10-22T06:47:12.870Z",
  "creationTime": 1761115632868,
  "modificationTime": 1761115632870
}
```

---

## cdp_assetcontent

Digital content asset entity for ads, landing pages, multimedia, gamification, and short links. Supports tracking, personalization, and engagement measurement.

### 📦 AssetContent Attributes

id (String)

* Primary key (_key in ArangoDB), generated via hashed content data

title (String)

* Title of the content item

description (String)

* Description of the content

type (int)

* Content type (e.g., HTML, video, URL, media)

assetType (int)

* Asset category type (e.g., landing page, ad banner, video, short link)

contentClass (String)

* Classification of content (e.g., creative, knowledge, standard, short link)

campaignId (String)

* Associated campaign ID (if content is part of a campaign)

fullUrl (String)

* Full destination URL (used for landing pages or external links)

shortLinkUrl (String)

* Generated short link URL for tracking and sharing

slug (String)

* URL-friendly identifier

categoryIds (List<String>)

* Associated category IDs

groupIds (List<String>)

* Associated group IDs

keywords (List<String>)

* Keywords/tags for search and classification

ownerId (String)

* Owner (user ID) of the content

networkId (long)

* Tenant / network identifier

privacyStatus (int)

* Privacy level (public, private, restricted)

status (int)

* Content status (e.g., draft, published, archived)

systemAsset (boolean)

* Flag indicating system-generated content

isAdItem (boolean)

* Flag indicating whether content is an advertisement

headlineImageUrl (String)

* Main image URL

headlineImages (Map<String, String>)

* Multiple headline images

headlineVideoUrl (String)

* Video URL

mediaInfo (String)

* Rich content (HTML or embedded media)

mediaInfoUnits (List<Object>)

* Structured media blocks

targetGeoLocations (List<String>)

* Target geographic locations

targetSegmentIds (List<String>)

* Target customer segment IDs

targetViewerIds (List<String>)

* Specific target user IDs

viewCount (int)

* Number of views

clickCount (int)

* Number of clicks

shareCount (int)

* Number of shares

reactionReportUnits (Map<String, Object>)

* Reaction analytics (likes, emotions, etc.)

feedbackCount (int)

* Number of feedback entries

transactionCount (int)

* Number of transactions attributed

rankingScore (int)

* Ranking/score for recommendation or sorting

qrCodeUrl (String)

* QR code image URL for the content

publishingTime (long)

* Publishing timestamp

destroyedTime (long)

* Deletion timestamp

createdAt (Date)

* Creation timestamp

updatedAt (Date)

* Last update timestamp

creationTime (long)

* Creation time (epoch seconds)

modificationTime (long)

* Last modification time (epoch millis)

### Sample data:

```json id="x92kq1"
{
  "id": "2FKnWLHaobsFn7hcOyubr9",
  "title": "AI agent",
  "description": "",
  "type": 1,
  "assetType": 1,
  "contentClass": "standard",
  "campaignId": "",
  "fullUrl": "",
  "shortLinkUrl": "https://obs.example.com/content/2FKnWLHaobsFn7hcOyubr9-ai-agent",
  "slug": "2FKnWLHaobsFn7hcOyubr9-ai-agent",
  "categoryIds": [
    "52H5rNvLZeCNiuZemcSqWc"
  ],
  "groupIds": [
    "4ngANNH9339A2p0RMsdh5r"
  ],
  "keywords": [
    "ai"
  ],
  "ownerId": "17094",
  "networkId": 0,
  "privacyStatus": 0,
  "status": 0,
  "systemAsset": false,
  "isAdItem": false,
  "headlineImageUrl": "https://leocdp.example.com/public/uploaded-files/f2af2b072f6fc5d41ffd686d96d44b6f1c6d3ee9.png",
  "headlineImages": {
    "https://leocdp.example.com/public/uploaded-files/f2af2b072f6fc5d41ffd686d96d44b6f1c6d3ee9.png": ""
  },
  "headlineVideoUrl": "",
  "mediaInfo": "<div>AI...</div>",
  "mediaInfoUnits": [],
  "targetGeoLocations": [],
  "targetSegmentIds": [],
  "targetViewerIds": [],
  "viewCount": 0,
  "clickCount": 0,
  "shareCount": 0,
  "reactionReportUnits": {},
  "feedbackCount": 0,
  "transactionCount": 0,
  "rankingScore": 0,
  "qrCodeUrl": "./public/qrcode/content-item-3diNnPOdV4iHDwHne60fLr.png",
  "publishingTime": 0,
  "destroyedTime": 0,
  "createdAt": "2025-12-19T08:21:25.097Z",
  "updatedAt": "2025-12-19T08:22:18.446Z",
  "creationTime": 1766132485,
  "modificationTime": 1766132538446
}
```

---

## cdp_assetgroup

Digital asset group acts as a container for organizing and managing collections of assets (e.g., posts, products, creatives, templates) with ordering, segmentation, and personalization support.

### 📦 AssetGroup Attributes

id (String)

* Primary key (_key in ArangoDB), generated via hashed group data

title (String)

* Name/title of the asset group

description (String)

* Description of the group

type (int)

* Content type of the group

assetType (int)

* Asset type (e.g., content catalog, campaign, product group)

categoryIds (List<String>)

* Associated category IDs

groupIds (List<String>)

* Parent group references (if nested grouping is used)

ownerId (String)

* Owner (user ID) of the group

networkId (long)

* Tenant / network identifier

mediaInfo (String)

* Rich content or description (HTML/text)

headlineImageUrl (String)

* Main image for the group

keywords (List<String>)

* Tags/keywords for classification

defaultGroup (boolean)

* Flag indicating if this is the default group for a given asset type

isManagedByLeoBot (boolean)

* Flag indicating if the group is managed automatically by LeoBot (AI automation)

mergeDataIntoTemplates (boolean)

* Flag indicating if group data should be merged into templates during rendering/personalization

mapContentClassKeywords (Map<String, List<String>>)

* Mapping of content class → keywords for classification and targeting

eventNamesForSegmentation (Set<String>)

* List of event names used for segmentation triggers (e.g., view, click, purchase)

itemsOfGroup (SortedSet<MeasurableItem>)

* Sorted set of content/items belonging to this group (runtime, not persisted)

viewCount (int)

* Total views across group items

clickCount (int)

* Total clicks across group items

shareCount (int)

* Total shares

createdAt (Date)

* Creation timestamp

updatedAt (Date)

* Last update timestamp

creationTime (long)

* Creation time (epoch seconds/millis depending on implementation)

modificationTime (long)

* Last modification time

### Sample data:

```json
{
  "id": "auto_generated_group_id",
  "title": "AI Content Group",
  "description": "Group of AI-related content for personalization",
  "type": 1,
  "assetType": 1,
  "categoryIds": [
    "52H5rNvLZeCNiuZemcSqWc"
  ],
  "groupIds": [],
  "ownerId": "17094",
  "networkId": 10000,
  "mediaInfo": "<div>AI Content Group</div>",
  "headlineImageUrl": "",
  "keywords": [
    "ai",
    "automation"
  ],
  "defaultGroup": false,
  "isManagedByLeoBot": true,
  "mergeDataIntoTemplates": false,
  "mapContentClassKeywords": {
    "creative": ["ai", "ml"],
    "knowledge": ["tutorial", "guide"]
  },
  "eventNamesForSegmentation": [
    "view",
    "click",
    "purchase"
  ],
  "viewCount": 0,
  "clickCount": 0,
  "shareCount": 0,
  "createdAt": "2026-01-01T10:00:00.000Z",
  "updatedAt": "2026-01-01T10:00:00.000Z",
  "creationTime": 1760000000000,
  "modificationTime": 1760000000000
}
```

---

## cdp_assettemplate

Template asset used for rendering dynamic content such as landing pages, email marketing, surveys, and personalized recommendations.

### 📦 AssetTemplate Attributes

id (String)

* Primary key (_key in ArangoDB), generated via hashed template data

title (String)

* Template title

description (String)

* Description of the template

type (int)

* Content type (always TEMPLATE)

assetType (int)

* Asset type (e.g., HTML content, email template, survey form)

categoryIds (List<String>)

* Associated category IDs

groupIds (List<String>)

* Associated group IDs

ownerId (String)

* Owner (user ID) of the template

networkId (long)

* Tenant / network identifier

selectorKey (String)

* Key used for selecting template variant (default: `default`, can support A/B testing)

activationName (String)

* Activation rule or campaign name linked to this template

selectorWeight (double)

* Weight for selection logic (used in weighted random selection)

templateType (int)

* Template type (e.g., survey, email, recommendation)

jsonMetadata (String)

* JSON metadata for template configuration (e.g., schema, variables)

headCode (String)

* HTML `<head>` section code (CSS, meta tags, scripts)

bodyCode (String)

* HTML `<body>` content (main template content)

mediaInfo (String)

* Additional content or preview (HTML/text)

shortLinkUrl (String)

* Generated URL for accessing template (e.g., survey link)

qrCodeUrl (String)

* QR code image URL for template access

slug (String)

* URL-friendly identifier

headlineImageUrl (String)

* Representative image for the template

keywords (List<String>)

* Tags/keywords for classification

status (int)

* Template status (draft, active, archived)

createdAt (Date)

* Creation timestamp

updatedAt (Date)

* Last update timestamp

creationTime (long)

* Creation time (epoch millis)

modificationTime (long)

* Last modification time (epoch millis)

### Sample data:

```json 
{
  "activationName": "sms-product-recommendation",
  "assetType": 6,
  "bodyCode": "",
  "categoryIds": [
    "7egQj2R6GRaJKflVUJU8pc"
  ],
  "contentClass": "",
  "createdAt": "2025-10-22T06:47:13.075Z",
  "creationTime": 1761115633075,
  "customData": {},
  "description": "",
  "destroyedTime": 0,
  "groupIds": [
    "6Z4m3ebXDyHOLnfaitGPWw"
  ],
  "headCode": "",
  "headlineImageUrl": "",
  "headlineImages": {},
  "headlineVideoUrl": "",
  "isAdItem": false,
  "jsonMetadata": "",
  "keywords": [],
  "mediaInfo": " Hi {{profile.firstName}}, you may like this item {{item.title}} at {{targetMediaUnit.trackingLinkUrl}}",
  "mediaInfoUnits": [],
  "modificationTime": 1761115633078,
  "networkId": 0,
  "ownerId": "17094",
  "privacyStatus": 0,
  "publishingTime": 0,
  "qrCodeUrl": "",
  "rankingScore": 0,
  "selectorKey": "default",
  "selectorWeight": 0.3100000023841858,
  "shortLinkUrl": "",
  "slug": "1sZ7V47bAdkZqwEVuAxKs2-sms-template-for-product-recommendation",
  "status": 0,
  "systemAsset": true,
  "targetGeoLocations": [],
  "targetSegmentIds": [],
  "targetViewerIds": [],
  "templateType": 0,
  "title": "SMS template for Product Recommendation",
  "topicIds": [],
  "type": 9,
  "updatedAt": "2025-10-22T06:47:13.078Z"
}
```
---
## cdp_campaign

Marketing campaign entity orchestrating automated customer journeys, targeting segments, executing actions (email, notification, personalization), and tracking ROI metrics.

### 📦 Campaign Attributes

id (String)

* Primary key (_key in ArangoDB), generated from campaign configuration

name (String)

* Campaign name

description (String)

* Campaign description

keywords (List<String>)

* Tags for classification and search

type (int)

* Campaign type (e.g., user-defined, system-driven)

automatedFlowJson (String)

* JSON definition of workflow/flowchart (nodes, conditions, actions)

flowchartDefinition (Object)

* Parsed structure of `automatedFlowJson` (runtime object, not always stored)

status (int)

* Campaign lifecycle status:

  * 0 = draft
  * 1 = planned
  * 2 = in progress
  * 3 = completed
  * 4 = canceled
  * -1 = removed

agentPersona (String)

* AI agent persona used for personalization or messaging tone

scenarios (List<Scenario>)

* List of campaign execution scenarios

targetSegmentId (String)

* Target customer segment ID

targetSegmentName (String)

* Target segment name (resolved dynamically via join, not persisted)

ownerUsername (String)

* Campaign owner (default: superadmin)

reportedUsernames (List<String>)

* Users who receive campaign reports

maxBudget (double)

* Estimated budget

spentBudget (double)

* Actual spending

communicationFrequency (int)

* Frequency of communication (e.g., daily = 1)

timeUnit (int)

* Time unit for frequency:

  * 0 = seconds
  * 1 = minutes
  * 2 = hours
  * 3 = days

createdAt (Date)

* Creation timestamp

updatedAt (Date)

* Last update timestamp

startDate (Date)

* Campaign start time (null = immediate execution)

lastRunAt (Date)

* Last execution timestamp

endDate (Date)

* Campaign end time (null = run indefinitely)

tagName (String)

* Slugified unique identifier for campaign (used for indexing/search)

### Sample data:

```json id="camp92x"
{
  "id": "auto_generated_campaign_id",
  "name": "demo_test_ index 9",
  "description": "ruleJsonUri ./data/marketing-automation-test/rules-for-unit-test.json",
  "keywords": [],
  "type": 0,
  "automatedFlowJson": "{ \"type\": \"flowchart\", \"nodes\": { ... }, \"rules\": [ ... ] }",
  "status": 0,
  "agentPersona": "",
  "scenarios": [],
  "targetSegmentId": "3og1APugf5q4dqxuFh9yLI",
  "ownerUsername": "superadmin",
  "reportedUsernames": [
    "superadmin"
  ],
  "maxBudget": 0,
  "spentBudget": 0,
  "communicationFrequency": 1,
  "timeUnit": 2,
  "createdAt": "2024-07-03T11:03:21.500Z",
  "updatedAt": "2024-07-03T11:03:21.500Z",
  "startDate": null,
  "lastRunAt": null,
  "endDate": null,
  "tagName": "demo_test__index_9"
}
```
---
## cdp_contextsession

Context session representing a visitor session on web/app, capturing device, behavior context, touchpoints, and identity linkage for real-time tracking and analytics. 

### 📦 ContextSession Attributes

sessionKey (String)

* Primary key (_key in ArangoDB), hashed from session context (device + profile + fingerprint + environment)

dateTimeKey (String)

* Time bucket key (format: yyyy-MM-dd-HH-[00|30]) for aggregation

userDeviceId (String)

* Unique device identifier

ip (String)

* IP address of the visitor

locationCode (String)

* Encoded geo-location (e.g., Plus Code)

refMediaHost (String)

* Referrer host/domain (e.g., zdn.vn)

appId (String)

* Application identifier

refTouchpointId (String)

* Referrer touchpoint ID

srcTouchpointId (String)

* Source touchpoint ID

observerId (String)

* Observer/collector ID tracking the session

profileId (String)

* Linked customer profile ID

visitorId (String)

* Anonymous visitor ID

fingerprintId (String)

* Device/browser fingerprint ID

profileType (int)

* Profile type:

  * 0 = anonymous visitor
  * other values = known customer types

environment (String)

* Runtime environment (e.g., dev, staging, prod)

createdAt (Date)

* Session start timestamp

updatedAt (Date)

* Last update timestamp

autoDeleteAt (Date)

* TTL expiration timestamp (auto cleanup via ArangoDB TTL index)

### Sample data:

```json id="ctx92a"
{
  "sessionKey": "auto_generated_session_key",
  "dateTimeKey": "2026-03-24-09-30",
  "userDeviceId": "5nklq9lPquSNLNtuUPghsv",
  "ip": "116.98.1.89",
  "locationCode": "7P3CWCVV+QJ",
  "refMediaHost": "zdn.vn",
  "appId": "",
  "refTouchpointId": "7NwctWrq0ea54eb6WbFSwg",
  "srcTouchpointId": "7BoGWS1z0PfPjxsvi8Kqy6",
  "observerId": "2lO3BwqvOnKfoOimSd8mqz",
  "profileId": "4ZjITqXaZM0qaYlCSssubk",
  "visitorId": "f479ef5b33c54de3a910b7e90661b3cc",
  "fingerprintId": "0fe6d592eb0f99c14b6bfe197c1dc94a",
  "profileType": 0,
  "environment": "dev",
  "createdAt": "2026-03-24T09:48:09.657Z",
  "updatedAt": null,
  "autoDeleteAt": "2026-03-31T09:48:09.657Z"
}
```
---
## cdp_dailyreportunit

Time-series analytics entity storing aggregated event counts per object (profile, product, content, etc.) on a daily basis with hourly breakdown for reporting and dashboards. 

### 📦 DailyReportUnit Attributes

id (String)

* Primary key (_key in ArangoDB), generated from object + event + date + journey

dateKey (String)

* Date key in format `yyyy-MM-dd` for daily aggregation

year (int)

* Year extracted from createdAt

month (int)

* Month extracted from createdAt

day (int)

* Day extracted from createdAt

hour (int)

* Hour extracted from createdAt

journeyMapId (String)

* Journey map identifier (default: id_default_journey)

objectId (String)

* Target object ID (e.g., profileId, productId, contentId)

objectName (String)

* Target object type (e.g., cdp_profile, product, content)

eventName (String)

* Event type (e.g., page-view, click, purchase)

dailyCount (long)

* Total count of events for the day

hourlyEventStatistics (Map<String, Map<String, Long>>)

* Hourly breakdown of event counts
* Format: `{ "yyyy-MM-dd HH:00:00": { "eventName": count } }`

createdAt (Date)

* Timestamp when the record is created

updatedAt (Date)

* Last update timestamp

partitionId (int)

* Partition identifier for scaling and sharding

### Sample data:

```json id="drp92x"
{
  "id": "auto_generated_id",
  "dateKey": "2022-05-25",
  "year": 2022,
  "month": 5,
  "day": 25,
  "hour": 13,
  "journeyMapId": "id_default_journey",
  "objectId": "6eioUN0BcVVTbDFJ9s6lT8",
  "objectName": "cdp_profile",
  "eventName": "page-view",
  "dailyCount": 1,
  "hourlyEventStatistics": {
    "2022-05-25 13:00:00": {
      "page-view": 1
    }
  },
  "createdAt": "2022-05-25T13:58:51.050Z",
  "updatedAt": "2022-05-25T13:58:51.050Z",
  "partitionId": 0
}
```
---
## cdp_agent

Agent represents an executable service in CDP used for personalization, data enrichment, and data synchronization. It extends `SystemService` and acts as a bridge between CDP and internal/external services. 

### 📦 Agent Attributes

id (String)

* Primary key (_key in ArangoDB), generated from service name (slugified)

name (String)

* Agent/service name

description (String)

* Description of the agent

serviceUri (String)

* Execution endpoint or handler:

  * `javaclass:...` → internal Java job
  * `ExternalAgentService` → external API

dagId (String)

* DAG ID for orchestration (Airflow or internal scheduler)

status (int)

* Agent state:

  * 0 = enabled
  * 1 = ready
  * -1 = disabled

index (int)

* Priority or ordering index

configs (Map<String, Object>)

* Configuration parameters (API keys, endpoints, SMTP config, etc.)

coreFieldConfigs (Map<String, Object>)

* Standard CDP field definitions

extFieldConfigs (Map<String, Object>)

* Extended/custom fields

forPersonalization (boolean)

* Whether agent is used for personalization use-cases

forDataEnrichment (boolean)

* Whether agent enriches data (profiles, products, etc.)

forSynchronization (boolean)

* Whether agent synchronizes data with external systems

startedAt (Date)

* Timestamp when agent started execution

createdAt (Date)

* Creation timestamp

updatedAt (Date)

* Last update timestamp

### Sample data:

```json id="agent92x"
{
  "id": "leo_email_marketing",
  "name": "LEO Email Marketing",
  "description": "The default email marketing engine of CDP",
  "serviceUri": "javaclass:leotech.cdp.data.service.InternalAgentActivation",
  "dagId": "leo_email_marketing",
  "status": 0,
  "index": 0,
  "configs": {
    "smtp_enabled": false,
    "smtp_username": "",
    "smtp_password": "",
    "smtp_host": "",
    "smtp_port": 587
  },
  "coreFieldConfigs": {},
  "extFieldConfigs": {},
  "forPersonalization": true,
  "forDataEnrichment": false,
  "forSynchronization": false,
  "createdAt": "2025-11-05T11:45:14.131Z",
  "updatedAt": "2025-11-05T11:45:15.146Z",
  "startedAt": null
}
```
---
## cdp_dataflowstage

Data flow stage represents a step in the customer data journey funnel (e.g., acquisition → engagement → conversion), used for tracking, analytics, and optimization of customer lifecycle.

### 📦 DataFlowStage Attributes

id (String)

* Primary key (_key in ArangoDB), slugified from `name`

name (String)

* Stage name (e.g., Engaged Customer, New Visitor)

type (String)

* Funnel type/category (e.g., general_data_funnel, marketing_funnel)

flowName (String)

* Name of the journey flow (e.g., standard_customer_flow)

flowType (int)

* Flow classification:

  * 0 = system metric
  * 1 = marketing
  * 2 = sales
  * 3 = customer service

orderIndex (int)

* Position of the stage in the funnel (used for ordering)

createdAt (Date)

* Creation timestamp

updatedAt (Date)

* Last update timestamp

### Sample data:

```json id="dfs92x"
{
  "id": "engaged_customer",
  "name": "Engaged Customer",
  "type": "general_data_funnel",
  "flowName": "standard_customer_flow",
  "flowType": 2,
  "orderIndex": 6,
  "createdAt": "2025-09-30T08:21:02.685Z",
  "updatedAt": "2025-09-30T08:21:02.685Z"
}
```
---
## cdp_device

Device entity representing a physical or logical device used by a visitor/profile for analytics, segmentation, and reporting. 

### 📦 Device Attributes

id (String)

* Primary key (_key in ArangoDB), generated from device fingerprint (name + type + OS + browser)

name (String)

* Device name (e.g., iPhone, Chrome Desktop)

type (String)

* Device type (e.g., General_Mobile, Desktop, Tablet, Backend_System)

osName (String)

* Operating system name (e.g., iOS, Android, Windows)

osVersion (String)

* Operating system version

browserName (String)

* Browser or device identifier (often same as device name)

createdAt (Date)

* Creation timestamp

updatedAt (Date)

* Last update timestamp

### Sample data:

```json id="dev92x"
{
  "id": "auto_generated_device_id",
  "name": "iPhone",
  "type": "General_Mobile",
  "osName": "iOS",
  "osVersion": "18.6.2",
  "browserName": "iPhone",
  "createdAt": "2026-03-24T10:22:27.556Z",
  "updatedAt": null
}
```
---
## cdp_eventmetric

Event metric defines a standardized event in CDP for tracking user behavior, scoring, and mapping to customer journey stages and funnels. 

### Java code path 

core-leo-cdp/src/main/java/leotech/cdp/model/journey/EventMetric.java

### 📦 EventMetric Attributes

id (String)

* Primary key (_key in ArangoDB), slugified from `eventName`

eventName (String)

* Unique event identifier (normalized, lowercase, kebab-case)

eventLabel (String)

* Human-readable label for UI display

flowName (String)

* Flow/category name (e.g., general_business_event, marketing_flow)

funnelStageId (String)

* Reference to DataFlowStage ID (e.g., new-visitor, engaged-customer)

journeyMapId (String)

* Journey map identifier (optional, can be empty)

journeyStage (int)

* Customer journey stage (5A model):

  * 1 = Awareness
  * 2 = Attraction
  * 3 = Ask
  * 4 = Action
  * 5 = Advocacy

dataType (int)

* Data source type:

  * 1 = first-party
  * 2 = second-party
  * 3 = third-party

score (int)

* Score value assigned to this event

cumulativePoint (int)

* Accumulated score over time

scoreModel (int)

* Scoring model type (e.g., lead scoring, engagement scoring, CLV scoring)

systemMetric (boolean)

* Flag indicating if this is a system-defined metric

showInObserverJS (boolean)

* Whether event is exposed to frontend tracking (observer JS)

createdAt (Date)

* Creation timestamp

updatedAt (Date)

* Last update timestamp

### Sample data:

```json id="evt92x"
{
  "id": "click-to-download",
  "eventName": "click-to-download",
  "eventLabel": "Click To Download",
  "flowName": "general_business_event",
  "funnelStageId": "new-visitor",
  "journeyMapId": "",
  "journeyStage": 2,
  "dataType": 1,
  "score": 5,
  "cumulativePoint": 0,
  "scoreModel": 1,
  "systemMetric": false,
  "showInObserverJS": false,
  "createdAt": "2026-01-23T08:08:30.995Z",
  "updatedAt": null
}
```

---

## touchpoint_type (enum / constant mapping)

TouchpointType defines standardized interaction channels across omni-channel CDP (digital, physical, messaging, ads, B2B, AI, etc.), used to classify EventObserver, TouchpointHub, and journey tracking.

### Java code path 

core-leo-cdp/src/main/java/leotech/cdp/model/journey/TouchpointType.java

### 📦 TouchpointType Attributes

type (int)

* Integer identifier stored in DB (used in EventObserver, TouchpointHub, etc.)

name (String)

* Constant name mapped from integer (via reflection)

category (String)

* Logical grouping of touchpoints

---

### 🔑 Core Touchpoint Types

CDP_API (0)

* Internal CDP ingestion API

---

### 🌐 Digital & Owned Media

SEARCH_ENGINE (1)
WEB_APP (2)
WEBSITE (3)
MOBILE_APP (4)
AR_VR_APP (5)
IOT_APP (6)
CHATBOT (7)
VIDEO_CHANNEL (8)
SOCIAL_MEDIA (9)
WEB_PORTAL (10)
KNOWLEDGE_HUB (11)
DIGITAL_DOCUMENT (33)
COMMUNITY_FORUM (60)
LOYALTY_PORTAL (61)

---

### 🏬 Physical & Real-world

RETAIL_STORE (12)
SHOPPING_TV (13)
SHOPPING_MALL (14)
COFFEE_SHOP (15)
CONFERENCE_HALL (16)
URBAN_PARK (17)
OFFICE_BUILDING (18)
EXPERIENCE_SPACE (19)
PR_EVENT_SPACE (20)
BILLBOARD_OUTDOOR (21)
BILLBOARD_INDOOR (22)
COMMUTER_STORE (23)
SPORTING_EVENT (24)
COMMUNITY_SPACE (25)
SCHOOL_SPACE (26)
TRANSIT_STATION (55)

---

### 📩 Messaging & Communication

EMAIL_CAMPAIGN (35)
SMS_MESSAGE (36)
PUSH_NOTIFICATION (37)
OTT_MESSAGING_APP (38)
VOICE_CALL_TELEMARKETING (39)

---

### 📢 Advertising & Paid Media

DISPLAY_AD (40)
SEARCH_AD (41)
SOCIAL_AD (42)
CTV_OTT_AD (43)
AUDIO_PODCAST_AD (44)

---

### 🛒 Commerce & Finance

ONLINE_MARKETPLACE (45)
AFFILIATE_NETWORK (46)
POS_TERMINAL (47)
SELF_SERVICE_KIOSK (48)
PAYMENT_GATEWAY (49)
ATM_MACHINE (56)

---

### 🏢 B2B & Enterprise

WEBINAR (50)
DIRECT_MAIL (51)
PARTNER_PORTAL (52)

---

### ❤️ Healthcare & Devices

WEARABLE_DEVICE (57)
TELEHEALTH_APP (58)
IN_VEHICLE_SYSTEM (59)

---

### 🤖 AI & Next-gen

VOICE_ASSISTANT (62)
GEN_AI_INTERFACE (63)
METAVERSE_SPACE (64)

---

### 👥 Customer Experience

KEY_INFLUENCER (27)
CUSTOMER_SERVICE (28)
FEEDBACK_SURVEY (29)

---

### ⚙️ System & Data Sources

DATA_OBSERVER (30)
REDIS_DATA_SOURCE (31)
KAFKA_DATA_SOURCE (32)
CRM_DATABASE (34)

### Sample data:

```json id="tp92x"
{
  "type": 33,
  "name": "DIGITAL_DOCUMENT",
  "category": "Digital & Owned Media"
}
```


---
## cdp_eventobserver

Event Observer is the data collection endpoint for tracking user behavior from touchpoints (website, mobile app, QR, digital document, etc.) into CDP for analytics and activation. 

### Java Code Path

core-leo-cdp/src/main/java/leotech/cdp/model/journey/EventObserver.java

### 📦 EventObserver Attributes

id (String)

* Primary key (_key in ArangoDB), generated from name + type + touchpointHubId

name (String)

* Name of the observer (e.g., LEO LIVE BOOK)

slug (String)

* URL-friendly identifier generated from name

type (int)

* Current touchpoint type in version 1.0 (enum from TouchpointType):

  * 0 = CDP_API
  * 1 = SEARCH_ENGINE
  * 2 = WEB_APP
  * 3 = WEBSITE
  * 4 = MOBILE_APP
  * 5 = AR_VR_APP
  * 6 = IOT_APP
  * 7 = CHATBOT
  * 8 = VIDEO_CHANNEL
  * 9 = SOCIAL_MEDIA
  * 10 = WEB_PORTAL
  * 11 = KNOWLEDGE_HUB
  * 12–26 = Physical locations (store, mall, event, etc.)
  * 27 = KEY_INFLUENCER
  * 28 = CUSTOMER_SERVICE
  * 29 = FEEDBACK_SURVEY
  * 30 = DATA_OBSERVER
  * 31 = REDIS_DATA_SOURCE
  * 32 = KAFKA_DATA_SOURCE
  * 33 = DIGITAL_DOCUMENT
  * 34 = CRM_DATABASE

status (int)

* Observer status: 1 = active, 0 = inactive

collectDirectly (boolean)

* Whether data is collected directly from client-side (JS, SDK)

firstPartyData (boolean)

* Indicates if data source is first-party

dataSourceUrl (String)

* Source URL of the data (landing page, app, document)

dataSourceHosts (Set<String>)

* Allowed host domains for tracking

thumbnailUrl (String)

* Thumbnail image for UI display

qrCodeData (Object)

* QR code metadata:

  * trackingUrl (String)
  * shortUrl (String)
  * landingPageUrl (String)
  * qrCodeImage (String)

accessTokens (Map<String,String>)

* Access tokens for secure data ingestion (key-value pairs)

securityCode (String)

* Security token for validating incoming events

touchpointHubId (String)

* Reference to parent touchpoint hub

journeyMapId (String)

* Associated journey map ID

journeyLevel (int)

* Level in customer journey hierarchy

estimatedTotalEvent (long)

* Estimated number of collected events

javascriptTags (List<String>)

* JS tracking tags injected into client

webApiHooks (List<String>)

* API endpoints for server-side tracking

mobileAppId (String)

* Mobile app identifier (if applicable)

deviceId (String)

* Associated device ID

securityAccessIp (String)

* Allowed IP for secure ingestion

observerUri (String)

* Internal observer endpoint URI

createdAt (Date)

* Creation timestamp

updatedAt (Date)

* Last update timestamp

### Sample data:

```json
{
  "id": "auto_generated_id",
  "name": "LEO LIVE BOOK",
  "slug": "leo-live-book",
  "type": 33,
  "status": 1,
  "collectDirectly": true,
  "firstPartyData": true,
  "dataSourceUrl": "https://leocdp.com/docs/index.html",
  "dataSourceHosts": [],
  "thumbnailUrl": "",
  "qrCodeData": {
    "trackingUrl": "https://datahub4uspa.leocdp.net/qrct/5evGdFREK1Q4Mq9uewhpRv",
    "shortUrl": "https://datahub4uspa.leocdp.net/ct/5evGdFREK1Q4Mq9uewhpRv",
    "landingPageUrl": "https://leocdp.com/docs/index.html",
    "qrCodeImage": "./public/qrcode/leo-live-book-4U63ZeCEgbIYyRZ0d5UJAu.png"
  },
  "accessTokens": {
    "1hgb91dmV1BhyoW9YMEKnb": "1148041_5NciIhnNztV82ClLaXNdeA"
  },
  "securityCode": "1131949_13oDADiOdDKhLngl6Op0s",
  "touchpointHubId": "OAQ61HuMFnENPBQ3WVJci",
  "journeyMapId": "id_default_journey",
  "journeyLevel": 3,
  "estimatedTotalEvent": 0,
  "javascriptTags": [],
  "createdAt": "2026-02-11T09:31:53.498Z",
  "updatedAt": null
}
```

---

## cdp_feedbackdata

Feedback data object transformed from tracking events, representing surveys, ratings, reviews, and customer experience signals across touchpoints in the CDP system.

### Java Code Path

`leotech.cdp.model.analytics.FeedbackData` 

---

### 📦 FeedbackData Attributes

id (String)

* Primary key (_key in ArangoDB), unique feedback identifier

feedbackDataType (String)

* Type of feedback data from ETL (e.g., survey, review, rating source classification)

refTemplateId (String)

* Reference to survey/template definition

refProductItemId (String)

* Associated product item ID

refContentItemId (String)

* Associated content item ID

refTouchpointHubId (String)

* Touchpoint hub identifier (high-level channel grouping)

refTouchpointId (String)

* Specific touchpoint identifier (form, page, event source)

refCampaignId (String)

* Marketing campaign reference

refDataFlowStageId (String)

* Data pipeline stage identifier

feedbackType (String)

* Logical classification (e.g., SURVEY, REVIEW, NPS, CES)

scoreCX (ScoreCX)

* Customer experience scoring object (sentiment, positive/negative/neutral metrics)

header (String)

* Survey or feedback title/header

group (String)

* Taxonomy or category (e.g., brand, topic, domain)

evaluatedObject (String)

* Abstract evaluated entity (product, service, course, etc.)

evaluatedItem (String)

* Specific evaluated item (URL, video, location, touchpoint)

evaluatedPerson (String)

* Evaluated person (e.g., staff, agent, instructor)

surveyChoicesId (String)

* Deterministic hash ID for survey choices set

surveyChoices (List<SurveyChoice>)

* List of survey options with associated scoring weights

timePeriod (String)

* Aggregation key (e.g., YYYYMMDD for analytics partitioning)

createdAt (Date)

* Creation timestamp (also used as updatedAt internally)

---

### Sample JSON data

```json
{
  "comment": "",
  "createdAt": "2024-06-30T04:38:27.543Z",
  "dateKey": "2024-06-30",
  "decisionMakers": [],
  "deviceId": "YKZ2rE4uyy7Rzj0wf37wX",
  "evaluatedItem": "",
  "evaluatedObject": "",
  "evaluatedPerson": "",
  "eventName": "submit-feedback-form",
  "extraTextQuestionsAnswer": {},
  "feedbackDataType": "",
  "feedbackScore": 0,
  "feedbackType": "SURVEY",
  "geoLatitude": -1,
  "geoLongitude": -1,
  "group": "",
  "header": "DECODING EQ - THRIVING IN A DYNAMIC WORLD",
  "language": "en",
  "mediaSources": [],
  "multipleChoiceQuestionAnswer": {},
  "onSharedDevices": false,
  "originalSources": [],
  "profileAge": -1,
  "profileAgeGroup": -1,
  "profileDateOfBirth": "",
  "profileEmail": "test@gmail.com",
  "profileExtAttributes": {
    "workingHistory": " My company ",
    "firstName": "Marry",
    "primaryPhone": "84-9031111",
    "jobTitles": "Bussiness development Director",
    "primaryEmail":  "test@gmail.com",
  },
  "profileExtId": {},
  "profileFirstName": "Marry",
  "profileGender": -1,
  "profileLastName": "",
  "profileLivingLocation": "",
  "profileNationality": "",
  "profilePhone": "0908797937",
  "ratingQuestionAnswer": {},
  "refCampaignId": "",
  "refContentItemId": "",
  "refDataFlowStageId": "",
  "refProductItemId": "",
  "refProfileId": "58fDYzHjsiBrQZMIwilHjW",
  "refTemplateId": "7RV0frnHxzYjavCTlORVMw",
  "refTouchpointHubId": "39FVRm9JXxzwLqWhWF3wlw",
  "refTouchpointId": "",
  "refVisitorId": "1aeedf578c874d3fa35297d3c19f872c",
  "scoreCX": {
    "happy": false,
    "negative": 0,
    "negativePercentage": 0,
    "neutral": 100,
    "neutralPercentage": 100,
    "positive": 0,
    "positivePercentage": 0,
    "sentimentScore": 71,
    "sentimentType": 0
  },
  "singleChoiceQuestionAnswer": {},
  "status": 1,
  "timePeriod": "20240709",
  "touchpointId": "2Zy2DISv80hEVieo4S0voO",
  "touchpointName": "DECODING EQ - THRIVING IN A DYNAMIC WORLD",
  "touchpointUrl": "https://datahub4uspa.leocdp.net/webform"
}
```

---

## cdp_filemetadata

File metadata storage for uploaded assets in CDP, supporting file management, ownership, access control, and polymorphic relationships to other domain objects.

### Java Code Path

`leotech.cdp.model.asset.FileMetadata` 

---

### 📦 FileMetadata Attributes

id (String)

* Primary key (_key in ArangoDB), generated from file path (hashed ID)

path (String)

* Physical or logical file path (unique)

name (String)

* File name

uploadedTime (long)

* Upload timestamp (epoch milliseconds)

createdAt (Date)

* Creation timestamp

revision (int)

* File version number (default = 1)

refObjectClass (String)

* Polymorphic reference to object class (e.g., Campaign, Profile, Content)

refObjectKey (String)

* Polymorphic reference to object key (ID of the related object)

downloadable (boolean)

* Flag indicating if file can be downloaded

ownerLogin (String)

* Owner identifier (userId or botId)

privacyStatus (int)

* Access control: 0=public, 1=protected, -1=private

viewerIds (List<Long>)

* List of user IDs allowed to view the file

networkId (long)

* Tenant / network identifier (multi-tenant support)

---

### Sample JSON data

```json
{
  "id": "f9a8c3b2d1e4",
  "path": "/uploads/campaigns/banner_2026.png",
  "name": "banner_2026.png",
  "uploadedTime": 1709280000000,
  "createdAt": "2025-03-01T10:00:00.000Z",
  "revision": 1,
  "refObjectClass": "Campaign",
  "refObjectKey": "cmp_123456",
  "downloadable": true,
  "ownerLogin": "admin_user",
  "privacyStatus": 0,
  "viewerIds": [1001, 1002, 1003],
  "networkId": 1
}
```

---

## cdp_financeevent

Finance event storage for all monetary-related interactions (payments, subscriptions, billing cycles, debts). Acts as the **source of truth for revenue, cashflow, and financial lifecycle per customer/profile**. 

### Java Code Path

`leotech.cdp.model.analytics.FinanceEvent` 

### 📦 FinanceEvent Attributes

id (String)

* Primary key (_key in ArangoDB), generated via hash from profile, payment type, timestamp, and context data

createdAt (Date)

* Event creation timestamp (when the financial event is recorded)

updatedAt (Date)

* Last update timestamp (used for sync, refresh, and incremental pipelines)

refProfileId (String)

* Reference to customer/profile ID (core identity in CDP)

paymentType (String)

* Type of financial event (e.g., payment, subscription, refund, installment, cancellation)

observerId (String)

* System/user/service that recorded or observed the event (tracking audit source)

refTouchpointHubId (String)

* High-level data hub where the event occurred (e.g., CRM, POS, Web platform)

srcTouchpointId (String)

* Source system ID where the event originated (e.g., Facebook, Shopify, App backend)

srcTouchpointName (String)

* Human-readable name of the source system

srcTouchpointUrl (String)

* URL of the source system (for traceability/debugging)

refTouchpointId (String)

* Reference touchpoint ID (normalized system-level identifier)

refTouchpointName (String)

* Normalized touchpoint name

refTouchpointUrl (String)

* Normalized touchpoint URL

refApplicationId (String)

* Application or product/service ID related to this financial event (e.g., subscription plan, loan, SaaS app)

period (int)

* Billing or payment cycle index (e.g., month number, installment sequence)

periodType (String)

* Type of period (default: "month", can be day/week/year depending on business logic)

dueDate (Date)

* Deadline for payment (used for receivables tracking)

dueAmountValue (long)

* Total amount due (monetary value, typically stored in smallest unit like cents)

principalValue (long)

* Principal amount (excluding interest/fees)

interestValue (long)

* Interest or additional charges applied to the principal

paid (boolean)

* Payment status: true = paid, false = unpaid (critical for receivable analytics)

extData (Map<String, String>)

* Flexible metadata for custom attributes (e.g., currency, transaction_id, payment_method, notes)

### Sample JSON data

```json
{
  "id": "fin_8f3a92c1",
  "createdAt": "2025-03-01T10:15:30.000Z",
  "updatedAt": "2025-03-01T10:15:30.000Z",
  "refProfileId": "profile_12345",
  "paymentType": "subscription_payment",
  "observerId": "system_billing_service",
  "refTouchpointHubId": "hub_crm",
  "srcTouchpointId": "stripe",
  "srcTouchpointName": "Stripe Payment Gateway",
  "srcTouchpointUrl": "https://dashboard.stripe.com",
  "refTouchpointId": "payment_gateway",
  "refTouchpointName": "Payment Gateway",
  "refTouchpointUrl": "https://payment.company.com",
  "refApplicationId": "app_saas_001",
  "period": 3,
  "periodType": "month",
  "dueDate": "2025-03-05T00:00:00.000Z",
  "dueAmountValue": 100000,
  "principalValue": 90000,
  "interestValue": 10000,
  "paid": true,
  "extData": {
    "currency": "VND",
    "transaction_id": "txn_abc123",
    "payment_method": "credit_card"
  }
}
```
---
## cdp_journeymap

Journey Map defines the **end-to-end customer journey model**, connecting touchpoints, stages, and transitions into a structured graph used for analytics, attribution, and personalization in CDP. Only **persisted fields are stored in ArangoDB**, while visualization/runtime fields are computed dynamically. 

### Java Code Path

`leotech.cdp.model.journey.JourneyMap` 

### 📦 JourneyMap Attributes

#### ✅ Persisted in ArangoDB

id (String)

* Primary key (_key in ArangoDB), generated from journey map name (or fixed default ID)

name (String)

* Name of the journey map (business-defined journey context)

status (int)

* Lifecycle status (1=active, 0=inactive, -1=deleted depending on system convention)

createdAt (Date)

* Creation timestamp

updatedAt (Date)

* Last updated timestamp (used for sync, cache invalidation)

defaultMetricName (String)

* Default metric for analytics (e.g., "page-view", "conversion")

authorizedViewers (Set<String>)

* List of user IDs allowed to view the journey map

authorizedEditors (Set<String>)

* List of user IDs allowed to edit/update the journey map

#### ❌ NOT persisted (runtime / computed only)

journeyStages (List<String>)

* Ordered stage names derived from touchpoint hubs (used for UI rendering only)

journeyNodes (List<JourneyNode>)

* Visualization nodes (graph structure for frontend rendering)

journeyLinks (List<JourneyNodeLink>)

* Connections between nodes representing journey transitions and metrics

touchpointHubMap (Map<String, TouchpointHub>)

* In-memory mapping of stage → touchpoint hub (core logic layer, not stored directly)

touchpointHubIndex (Map<String, Integer>)

* Index mapping for fast lookup and graph computation

sortedTouchpointHubs (List<TouchpointHub>)

* Ordered touchpoint hubs by journey level (used to construct journey flow dynamically)

### Sample JSON data

```json id="journeymap_real"
{
  "authorizedEditors": [
    "test"
  ],
  "authorizedViewers": [
    "test"
  ],
  "createdAt": "2025-08-12T08:53:40.181Z",
  "defaultMetricName": "page-view",
  "name": "My journey map",
  "status": 1,
  "updatedAt": "2025-10-10T04:15:58.892Z"
}
```
