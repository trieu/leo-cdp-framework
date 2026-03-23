
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
