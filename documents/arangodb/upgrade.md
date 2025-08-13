
# SH

arangosh --server.endpoint tcp://leocdp.database:8600 --server.username root --server.database leo_cdp --server.authentication true



# Graph

```javascript
db._useDatabase("leo_cdp_test");
var graph_module = require("@arangodb/general-graph");

graph_module._drop("cdp_purchased_product_graph");

col = db.cdp_profile2purchasedproduct;
col.drop();

db._createEdgeCollection("cdp_profile2conversion");
var edgeDefinitions = [{ collection: "cdp_profile2conversion", "from": ["cdp_profile"], "to" : ["cdp_productitem"] }];
graph = graph_module._create("cdp_conversion_graph", edgeDefinitions);
```

# Upgrade DataService

```
	FOR t IN cdp_dataservice
	UPDATE t WITH {
	    forSynchronization: t.forSegmentDataActivation, forSegmentDataActivation: null,
	    forDataEnrichment: t.forProfileEnrichment, forProfileEnrichment: null,
	    forPersonalization: t.forTouchpointEnrichment, forTouchpointEnrichment: null,
	    dataSynchService: null,
	    emailSenderService: null,
	    forSegmentActivation: null,
	    messagePusherService: null,
	    periodInSecondsToLoop: null,
	    personalizationService: null,
	    profileScoringService: null,
	    smsSenderService: null,
	    targetProfiles: null,
	    targetTouchpoints: null
	}
	IN cdp_dataservice 
	OPTIONS { keepNull: false }
```

# Upgrade Tracking Event
```
	FOR e IN cdp_trackingevent
	FILTER ! HAS (e, 'transactionDiscount')
	UPDATE e WITH {
	    transactionDiscount : 0
	}
	IN cdp_trackingevent 
```

# up
```
FOR e IN cdp_trackingevent
FILTER (e.transactionStatus == '' OR !HAS(e,'transactionStatus') ) AND e.rawJsonData != ''
LET obj = ( REGEX_REPLACE( TO_STRING(e.rawJsonData) , "'", '"') )  
LIMIT 0, 2
SORT e.updatedAt DESC
RETURN obj
```