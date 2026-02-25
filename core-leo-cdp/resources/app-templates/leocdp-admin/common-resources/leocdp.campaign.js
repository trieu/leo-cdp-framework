
var campaignView = function(id) {
	location.hash = "calljs-leoCdpRouter('Campaign_Info','" + id + "')";
}

var campaignEditor = function(id) {
	location.hash = "calljs-leoCdpRouter('Campaign_Editor','" + id + "')";
}

var campaignRunImmediately = function(id) {
	console.log('campaignRunImmediately ' + id)
}

var campaignScheduler = function(id) {
	console.log('campaignScheduler ' + id)
}

var campaignListRefresh = function() {
	var t = new Date().getTime();
	location.hash = "#calljs-leoCdpRouter('Automated_Campaigns','_refresh_" + t + "')";
}

function loadCampaignList() {
	var usersession = getUserSession();
	if (usersession) {
		$('#campaign-list').DataTable({
			'processing': false,
			'serverSide': false,
			'serverMethod': 'POST',
			'ajax': {
				url: baseLeoAdminUrl + '/cdp/campaigns/filter',
				contentType: 'application/json',
				beforeSend: function(request) {
					request.setRequestHeader("leouss", usersession);
				},
				data: function(d) {
					console.log(d)
					return JSON.stringify(d);
				}
			},
			'columnDefs': [
				{
					"render": function(data, type, row) {
						var name = textTruncate(data, 60);
						var callJsViewStr = "javascript:campaignView('" + row.id + "')";
						return '<a title="' + row.name + '" href="' + callJsViewStr + '" >' + name + '</a>';
					},
					"targets": 0
				},
				{
					"render": function(data, type, row) {
						var text = "USER_DEFINED";
						if (data === 1) {
							text = "BRAND_AWARENESS";
						}
						else if (data === 2) {
							text = "LEAD_GENERATION";
						}
						else if (data === 3) {
							text = "CONVERSION_OPTIMIZATION";
						}
						else if (data === 4) {
							text = "RETENTION_OPTIMIZATION";
						}
						else if (data === 5) {
							text = "CROSS_SELLING";
						}
						else if (data === 6) {
							text = "UPSELLING";
						}
						else if (data === 7) {
							text = "REFERRAL_PROGRAM";
						}
						return '<div class="datatable_text text-center">' + text + '</div>';
					},
					"targets": 1
				},
				{
					"render": function(data, type, row) {
						var statusTxt = "DRAFT";
						if (data === 1) {
							statusTxt = "PLANNED";
						}
						else if (data === 2) {
							statusTxt = "IN_PROGRESS";
						}
						else if (data === 3) {
							statusTxt = "COMPLETED";
						}
						else if (data === 4) {
							statusTxt = "CANCELED";
						}
						else if (data === -1) {
							statusTxt = "DELETED";
						}
						return '<div class="datatable_text text-center">' + statusTxt + '</div>';
					},
					"targets": 2
				},
				{
					"render": function(data, type, row) {
						return '<div class="datatable_text text-center">' + data + '</div>';
					},
					"targets": 3
				},
				{
					"render": function(data, type, row) {
						var date =toLocalDateTime(data);
						return date;
					},
					"targets": 4
				},
				{
					"render": function(data, type, row) {
						if (data) {
							var date =toLocalDateTime(data);
							return date;
						}
						return "-";
					},
					"targets": 5
				},
				{
					"render": function(data, type, row) {
						var action = "#calljs-leoCdpRouter('User_Login_Report','"+data+"')";
						var node = $('<a/>').attr('href', action).html(data);
						var html = node[0].outerHTML;
						return html;
					},
					"targets": 6
				},
				{
					"render": function(data, type, row) {
						var callJsViewStr = "javascript:campaignView('" + row.id + "')";
						var callJsEditStr = "javascript:campaignEditor('" + row.id + "')";
						var callJsRunManuallyStr = "javascript:campaignRunImmediately('" + row.id + "')";
						var callJsRunSchedulerStr = "javascript:campaignScheduler('" + row.id + "')";

						var outHtml = '<div style="text-align: left!important;padding-left:5px;">';
						outHtml += '<a class="control" title="Campaign Details" href="' + callJsViewStr + '" ><i class="fa fa-info-circle" aria-hidden="true"></i> View </a> <br>'
						outHtml += '<a class="control" title="Campaign Editor" href="' + callJsEditStr + '" ><i class="fa fa-pencil-square-o" aria-hidden="true"></i>  Edit </a> <br>';
						outHtml += '<a class="control" title="Schedule campaign" href="' + callJsRunSchedulerStr + '" ><i class="fa fa-clock-o" aria-hidden="true"></i> Schedule </a>  <br>';
						outHtml += '<a class="control" title="Run campaign immediately" href="' + callJsRunManuallyStr + '" ><i class="fa fa-play" aria-hidden="true"></i> Run </a> </div>';
						return outHtml;
					},
					"targets": 7
				}
			],
			'columns': [{
					// 0
					"data": "name"
				},
				{
					// 1
					"data": "type"
				},
				{
					// 2
					"data": "status"
				},
				{
					// 3
					"data": "targetedSegmentNames"
				},
				{
					// 4
					"data": "updatedAt"
				},
				{
					// 5
					"data": "lastRunAt"
				},
				{
					// 6
					"data": "ownerUsername"
				},
				{
					// 7
					"data": "automatedFlow"
				}
			]
		});
	}
}

function initCampaignMetadata(campaignMetaData){
	var id = campaignMetaData.id;
	var name = campaignMetaData.name || "New Campaign (Draft)";

	document.title = 'Campaign Editor: ' + name;
	$('#campaign_name_header').text('Campaign: ' + name);
	$('#campaign_description').val(campaignMetaData.description);
	$('#campaign_name').val(name).keyup(function() {
		campaignMetaData.name = $(this).val().trim();
		$('#campaign_name_header').text('Campaign: ' + campaignMetaData.name);
	});
	$('#runAtDateAndTime').datetimepicker();
	
	// init UI after setting data into DOM

	// loadProductsInCampaign(cpId)
	if(campaignMetaData.automatedFlowJson != null){
		var metadata = {'segment_name':'test' }
		metadata['condition_name'] = "Today is the birthday of customer";
		metadata['condition_expression'] = "context.isCurrentDateEqualProfileBirthday(\"@{currentDate}\")";
		metadata['campaign_action_type'] = 'email';
		
		var finalJsonStr = Handlebars.compile(campaignMetaData.automatedFlowJson)(metadata);
		var flow = JSON.parse(finalJsonStr)
		renderCampaignAutomatedFlow(flow)
	}
	
	// load all segments for selection
	getAllSegmentRefs(function(list) { 
		var node = $('#campaign_targetedSegmentIds').empty();
		list.forEach(function(e){
			node.append($('<option />').attr('value',e.id).text(e.name));
		});		
		$('#campaign_targetedSegmentIds').chosen({
             width: "100%",
             no_results_text: "No data available!"
        }).trigger("chosen:updated");  		
	});
	
	// Condition Type
	$('#campaign_condition').chosen({
         width: "100%",
         no_results_text: "No data available!"
    }).change(function(){
        var id = $(this).val();
		console.log(id)
		if('getEventAtUrl' === id){
			$('#runAtDateAndTimeForCondition').hide()
			$('#eventMetricWithUrlForCondition').show();
		}
		else if('runAtDateAndTime' === id){
			$('#eventMetricWithUrlForCondition').hide();
			$('#runAtDateAndTimeForCondition').show()
		}
    }).trigger("chosen:updated");   

	// Action Type
	$('#campaign_action_type').chosen({
         width: "100%",
         no_results_text: "No data available!"
    }).change(function(){
        var id = $(this).val();
		console.log(id)
		$('#frequencyCappingType').text(id.toUpperCase())
    }).trigger("chosen:updated");

	// template
	$('#campaign_template_id').chosen({
         width: "100%",
         no_results_text: "No data available!"
    }).change(function(){
        var id = $(this).val();
		console.log(id)
    }).trigger("chosen:updated");

	// set label of type
	$('#frequencyCappingType').text($('#campaign_action_type').find(":selected").val())

	// Frequency Capping
	$('#frequencyCappingTimeUnit').chosen({
         width: "100%",
         no_results_text: "No data available!"
    }).change(function(){
        var id = $(this).val();
		console.log(id)
    }).trigger("chosen:updated");

	// load all event metrics
	loadEventMetricsForCampaignEventTrigger([]);
}

var automatedFlowJsonData = {};
function loadCampaignEditor(id) {
	var editor_loader = $('#campaign_editor_loader');
	var editor_div = $('#campaign_editor_div');
	var urlStr = baseLeoAdminUrl + '/cdp/campaign/get';
	var param = { 'id': id };
	var ajaxHandler = function(json) {
		editor_loader.hide();
		editor_div.show();
		
		if (json.httpCode === 0 && json.errorMessage === '') {
			// only super-admin role can remove the segment

			if (!json.canEditData) {
				$('button.data-control-edit').attr('disabled', 'disabled');
			} else {
				$('button.data-control-edit').click(function() {
					location.hash = "calljs-leoCdpRouter('Campaign_Editor','" + id + "')";
				})
			}
			
			var campaignModel = json.data;
			if(campaignModel){
				initCampaignMetadata(json.data)
			}
			else {
				
			}
		} else {
			LeoAdminApiUtil.logErrorPayload(json);
		}
	};
	LeoAdminApiUtil.callPostAdminApi(urlStr, param, ajaxHandler);
}

function loadEventMetricsForCampaignEventTrigger(selectedEventNames){
	//
	LeoAdminApiUtil.getSecuredData('/cdp/funnel/event-metrics', {} , function(json){  
		var eventMetrics = json.data;
		var htmlSelectedEvents = "";
		
		eventMetrics.forEach(function(e){
			var eventName = e.eventName;
			if(selectedEventNames.includes(eventName)){
				htmlSelectedEvents += ('<option selected value="' + e.id + '">' + e.eventLabel + '</option>')
   			} else {
   				htmlSelectedEvents += ('<option value="' + e.id + '">' + e.eventLabel + '</option>')
   			}
       	});
		
    	$('#eventForCampaignCondition').html(htmlSelectedEvents).chosen({
            width: "100%",
            no_results_text: "Oops, nothing found!"
        }).change(function(){
	        var id = $(this).val();
			console.log(id)
	    }).trigger("chosen:updated");   ;
	})
}

function renderCampaignAutomatedFlow(flowJsonData) {
	automatedFlowJsonData = flowJsonData;
	console.log("flowJsonData \n ", flowJsonData);
	var mermaidCode = jsonToMermaid(flowJsonData);
	console.log("jsonToMermaid \n ", mermaidCode);

	mermaid.initialize({ startOnLoad: false, securityLevel: 'loose', useMaxWidth: true });
	mermaid.render('id', mermaidCode).then(({ svg, bindFunctions }) => {
		var diagram = document.getElementById('campaignAutomatedFlow');
		diagram.innerHTML = svg;
		if (bindFunctions) {
			bindFunctions(diagram);
		}
	});
	
	//automatedFlowJsonData = mermaidToJSON(mermaidCode);
	//var flowchartJSON = JSON.stringify(flowJsonData, null, 2)
	//console.log("mermaidToJSON:", flowchartJSON);
	//console.log("mermaidToJSON === automatedFlowJsonData:", flowchartJSON === automatedFlowJsonData);
}

function automatedFlowNodeClick(id) {
	console.log(id)
	notifySuccessMessage(id)
}

function mermaidToJSON(mermaidCode) {
	const lines = mermaidCode.split('\n').map(line => line.trim()).filter(line => line);
	const jsonOutput = automatedFlowJsonData;
	
	jsonOutput.rules = [];
	
	lines.forEach(line => {
		line = line.trim();
		try {
			if (line.startsWith('flowchart')) {
				jsonOutput['type'] = 'flowchart';
			}
			else if (line.includes('-->')) {
				var [start, rest] = line.split('-->');
				var [_, conditionResult, end] = rest.includes('|') ? rest.split('|') : [null, rest];

				conditionResult = conditionResult ? conditionResult.trim(): null;
				end = end ? end.trim().replace(/ \[\[|\]\] /g, '') : conditionResult;

				jsonOutput.rules.push({
					start: start.trim(),
					conditionResult: conditionResult && conditionResult != end ? conditionResult : null,
					end: end
				});
			}
			else if (line.includes('[')) {
				var toks = line.split("[");
				const id = toks[0].trim(); // Extract ID:
				const label = line.match(/\[(.*?)\]/)?.[1].trim();
				
				var node = jsonOutput.nodes[id];
				if(typeof node === 'object'){
					node.id = id;
					node.label = label;
				}
				else {
					jsonOutput.nodes[id] = { id, label };
				}
			}
			else {
				console.log("mermaidToJSON, skip line: ", line)
			}
		} catch (e) {
			console.log("error", line)
		}
	});

	return jsonOutput;
}

function jsonToMermaid(json) {
	var flowDirection = 'LR';
	//flowDirection = 'TD';
	let mermaidCode = `${json.type} ${flowDirection}\n`;
	// nodes
	Object.values(json.nodes).forEach(node => {
		mermaidCode += `${node.id}[${node.label}]\n`;
		mermaidCode += `click ${node.id} href "javascript:automatedFlowNodeClick('${node.id}')"\n`;
	});
	// rules
	json.rules.forEach(edge => {
		if (edge.conditionResult) {
			edge.conditionResult = edge.conditionResult
			mermaidCode += `${edge.start} -->|${edge.conditionResult}| ${edge.end}\n`;
		} else {
			mermaidCode += `${edge.start} --> ${edge.end}\n`;
		}
	});
	return mermaidCode;
}

