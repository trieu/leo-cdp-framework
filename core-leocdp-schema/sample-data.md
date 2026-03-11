
## cdp_activationrule 

Pure domain model for data acivation, data must be synched from ArangoDB (core CDP) to PGSQL16+ (LEO Activation)

```java

public class ActivationRule extends PersistentObject implements Comparable<ActivationRule>, Serializable {

	private static final long serialVersionUID = 1825726230556120351L;

	public static final String SCHEDULING_TASK = "scheduling_task";
	public static final String EVENT_TRIGGER_TASK = "event_trigger_task";

	public static final String COLLECTION_NAME = getCdpCollectionName(ActivationRule.class);

	private static final Gson GSON = new Gson();
	static ArangoCollection dbCollection;

	// =========================
	// Persistent Fields
	// =========================
	@Key
	@Expose
	private String id;
	@Expose
	private Date createdAt;
	@Expose
	private Date updatedAt;

	@Expose
	private String name;
	@Expose
	private String activationType;
	@Expose
	private String description = "";

	@Expose
	private int priority = 1;
	@Expose
	private boolean active = true;

	@Expose
	private String condition = "true";
	@Expose
	private List<String> actions = new ArrayList<>();

	@Expose
	private String purpose = "";

	@Expose
	private String agentId = "";
	@Expose
	private String segmentId = "";

	@Expose
	int assetType = -1;

	@Expose
	String assetTemplateId = "";

	@Expose
	String assetTemplateName = "";

	@Expose
	String agentName = "";

	/**
	 * schedulingTime: < 0 : manual / one-time = 0 : polling loop > 0 : fixed-rate
	 * schedule
	 */
	@Expose
	private int schedulingTime = 0;

	/**
	 * timeUnit: 0 = seconds 1 = minutes 2 = hours (DEFAULT) 4 = days 5 = weeks
	 */
	@Expose
	private int timeUnit = TimeUnitCode.HOUR;

	@Expose
	private String timeToStart = "00:00";
	@Expose
	private String triggerEventName = "";

	@Expose
	private String ownerUsername = SystemUser.SUPER_ADMIN_LOGIN;
}

```

Sample data:

```json
{
  "actions": [],
  "activationType": "run-agent",
  "active": true,
  "agentId": "leo_personalization_service",
  "agentName": "",
  "assetTemplateId": "",
  "assetTemplateName": "",
  "assetType": -1,
  "condition": "true",
  "createdAt": "2026-03-11T09:49:24.942Z",
  "description": "",
  "name": "personalization-leo_personalization_service-3oLdrUJGRB8w28poFvE2uE",
  "ownerUsername": "superadmin",
  "priority": 32,
  "purpose": "personalization",
  "schedulingTime": 0,
  "segmentId": "3oLdrUJGRB8w28poFvE2uE",
  "timeToStart": "",
  "timeUnit": 0,
  "triggerEventName": "run_default_job",
  "updatedAt": "2026-03-11T09:49:24.956Z"
}

```

## cdp_agent

Agent to enrich data and using goals to activate profile, segment and touchpoint


```json
{
  "configs": {
    "service_provider": "https://leocdp.com",
    "smtp_enabled": "true"
  },
  "coreFieldConfigs": {},
  "createdAt": "2025-10-22T06:47:11.000Z",
  "dagId": "",
  "description": "The default personalization engine of CDP",
  "extFieldConfigs": {},
  "forDataEnrichment": true,
  "forPersonalization": true,
  "forSynchronization": true,
  "index": 0,
  "name": "Personalization Engine",
  "serviceUri": "javaclass:leotech.cdp.data.service.InternalAgentActivation",
  "startedAt": "2025-12-25T06:35:29.000Z",
  "status": 1,
  "updatedAt": "2026-01-27T18:52:08.509Z"
}

```

## cdp_assetcategory