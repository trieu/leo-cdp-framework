# To implement search dataLabels with LIKE %dataLabel% :
	FOR p IN cdp_profile
	FILTER p.dataLabelsAsStr == null OR !HAS(p, "dataLabelsAsStr")
	  UPDATE p WITH {
	    dataLabelsAsStr: CONCAT_SEPARATOR(" , ", p.dataLabels)
	  } IN cdp_profile

# To upgrade computableFields in cdp_trackingevent
FOR e IN cdp_trackingevent
UPDATE e WITH { computableFields: [] } IN cdp_trackingevent

# To implement search cdp_profile:
add view: cdp_profile_view

"links": {
    "cdp_profile": {
      "analyzers": [
        "identity",
        "text_en"
      ],
      "fields": {
        "totalLeadScore": {},
        "visitorId": {},
        "authorizedViewers": {},
        "funnelStage": {},
        "updatedAt": {},
        "crmRefId": {},
        "firstName": {},
        "authorizedEditors": {},
        "fingerprintId": {},
        "inJourneyMaps": {},
        "lastName": {},
        "lastTouchpoint": {},
        "mediaChannels": {},
        "primaryEmail": {},
        "primaryPhone": {},
        "secondaryEmails": {},
        "type": {},
        "journeyMapId": {},
        "status": {},
        "inSegments": {},
        "dataLabels": {},
        "secondaryPhones": {}
      },
      "includeAllFields": false,
      "storeValues": "none",
      "trackListPositions": false
    }
  },