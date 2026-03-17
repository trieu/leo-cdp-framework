

## cdp_activationrule 

**Description**: This collection defines the logic for data activation within LEO CDP. It acts as a bridge between the core CDP (ArangoDB) and the Activation Engine (PostgreSQL 16+). Activation rules determine when and how profile data is synchronized, processed by agents, or sent to external marketing channels.



### Data Schema (ArangoDB)

| Field | Type | Description |
| :--- | :--- | :--- |
| `name` | String | Unique identifier name for the rule. |
| `activationType` | String | Type of activation (e.g., `run-agent`, `webhook`, `email-sync`). |
| `active` | Boolean | Toggle to enable or disable the rule. |
| `condition` | String | An AQL or JavaScript-based logic string that must be true for the rule to fire. |
| `priority` | Integer | Execution order (lower numbers generally execute first). |
| `segmentId` | String | The ID of the Segment (from `cdp_segments`) this rule applies to. |
| `agentId` | String | The ID of the Agent (from `cdp_agent`) assigned to execute this rule. |
| `schedulingTime` | Integer | `< 0`: Manual; `0`: Polling loop; `> 0`: Fixed-rate interval. |
| `timeUnit` | Integer | 0: Seconds, 1: Minutes, 2: Hours, 4: Days, 5: Weeks. |
| `triggerEventName` | String | The specific event name that triggers this rule (e.g., `run_default_job`). |

---

## cdp_agent

**Description**: Agents are functional modules responsible for data enrichment, synchronization, and activation. They use specific "goals" to process profiles, segments, and touchpoints. Agents can be internal Java classes or external microservices connected via API.



### Data Schema (ArangoDB)

| Field | Type | Description |
| :--- | :--- | :--- |
| `name` | String | Friendly name of the agent (e.g., "Personalization Engine"). |
| `description` | String | Purpose and functionality of the agent. |
| `serviceUri` | String | The execution path. Format: `javaclass:path.to.Class` for internal or `https://api.url` for external. |
| `configs` | Object | Key-value pairs for agent-specific settings (e.g., SMTP, API keys). |
| `forDataEnrichment`| Boolean | If true, the agent adds/updates data on the Profile. |
| `forPersonalization`| Boolean | If true, the agent is used for content/product recommendations. |
| `forSynchronization`| Boolean | If true, the agent pushes data to third-party systems (CRM, ERP). |
| `status` | Integer | `1`: Active, `0`: Inactive/Maintenance. |

---

## cdp_assetcategory

**Description**: This collection manages the hierarchical taxonomy for Digital Assets (Content, Templates, Product catalogs) within the CDP. It allows for organized storage and retrieval of assets used in personalization and marketing campaigns.

### Data Schema (ArangoDB)

| Field | Type | Description |
| :--- | :--- | :--- |
| `id` | String | Unique category ID. |
| `name` | String | Display name of the category (e.g., "Email Templates", "Landing Pages"). |
| `description` | String | Details about the types of assets stored in this category. |
| `parentId` | String | Used for nested categories (points to another `cdp_assetcategory` ID). |
| `assetType` | Integer | The type of assets allowed in this category (e.g., 1 for Content, 2 for Product). |
| `ownerUsername` | String | The user who created or manages this category. |
| `createdAt` | Date | Timestamp of creation. |
| `updatedAt` | Date | Timestamp of last modification. |

### Sample Data
```json
{
  "id": "email_marketing_templates",
  "name": "Email Marketing",
  "description": "Templates for weekly newsletters and promotional offers",
  "parentId": "root",
  "assetType": 1,
  "ownerUsername": "admin",
  "createdAt": "2025-11-01T10:00:00.000Z",
  "updatedAt": "2026-01-15T08:30:00.000Z"
}
```