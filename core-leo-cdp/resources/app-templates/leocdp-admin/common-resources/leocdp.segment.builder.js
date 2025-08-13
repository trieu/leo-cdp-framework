const getOperators = function(){
	var ops = _.union(getOperatorsForStringField(),getOperatorsForNumberField()); 
	
	ops.push({ type: "after", nb_inputs: 1, multiple: true, apply_to: ['string'] });
	ops.push({ type: "before", nb_inputs: 1, multiple: true, apply_to: ['string'] });
	
	ops.push({ type: "exactly_after", nb_inputs: 1, multiple: true, apply_to: ['string'] });
	ops.push({ type: "exactly_before", nb_inputs: 1, multiple: true, apply_to: ['string'] });
	
	ops.push({ type: "compare_month_and_day", nb_inputs: 1, multiple: true, apply_to: ['date'] });
	ops.push({ type: "compare_month_and_day_between", nb_inputs: 2, multiple: true, apply_to: ['date'] });
	ops.push({ type: "compare_month_and_day_with_now", nb_inputs: 0, multiple: true, apply_to: ['date'] });
	ops.push({ type: "compare_year_month_day_with_now", nb_inputs: 0, multiple: true, apply_to: ['date'] });
	
	//	
	ops.push({ type: "is_today", nb_inputs: 0, multiple: true, apply_to: ['date'] });	
	ops.push({ type: "is_valid_date", nb_inputs: 0, multiple: true, apply_to: ['date'] });
	ops.push({ type: "is_not_valid_date", nb_inputs: 0, multiple: true, apply_to: ['date'] });
	
	ops.push({ type: "contains_any", optgroup: 'List Operator', nb_inputs: 1, multiple: true, apply_to: ['string','number','boolean'] });
	ops.push({ type: "not_contains_any", optgroup: 'List Operator', nb_inputs: 1, multiple: true, apply_to: ['string','number','boolean'] });
	
	ops.push({ type: "has_key", optgroup: 'Key Operator', nb_inputs: 1, multiple: true, apply_to: ['string'] });
	ops.push({ type: "not_has_key", optgroup: 'Key Operator', nb_inputs: 1, multiple: true, apply_to: ['string'] });
	
	ops.push({ type: "sub_field_contains", optgroup: 'Sub Field', nb_inputs: 1, multiple: true, apply_to: ['string'] });
	ops.push({ type: "sub_field_equals", optgroup: 'Sub Field', nb_inputs: 1, multiple: true, apply_to: ['string'] });
	
	ops.push({ type: "data_equals", optgroup: 'Data Filter', nb_inputs: 1, multiple: true, apply_to: ['string','number','boolean'] });
	ops.push({ type: "data_less_than", optgroup: 'Data Filter', nb_inputs: 1, multiple: true, apply_to: ['string','number'] });
	ops.push({ type: "data_equals_or_less_than", optgroup: 'Data Filter', nb_inputs: 1, multiple: true, apply_to: ['string','number'] });
	ops.push({ type: "data_greater_than", optgroup: 'Data Filter', nb_inputs: 1, multiple: true, apply_to: ['string','number'] });
	ops.push({ type: "data_equals_or_greater_than", optgroup: 'Data Filter', nb_inputs: 1, multiple: true, apply_to: ['string','number'] });
	
	ops.push({ type: "data_contains", optgroup: 'Data Filter', nb_inputs: 1, multiple: true, apply_to: ['string'] });
	ops.push({ type: "data_from", optgroup: 'Data Filter', nb_inputs: 1, multiple: true, apply_to: ['string'] });
	ops.push({ type: "in_journey_map", optgroup: 'Data Filter', nb_inputs: 1, multiple: true, apply_to: ['string'] });
	
	//ops.push({ type: "has_data_in", optgroup: 'Data Filter', nb_inputs: 2, multiple: true, apply_to: ['string'] });
	return ops;
}
	
const getTouchpointHubHosts = function(touchpointHubs) {
	var map = {};
	for (const [key, value] of Object.entries(touchpointHubs)) {
		if(key.indexOf('http')===0){
			try{
				var host = new URL(key).host.replace("www.","");
			 	map[host] = value;
			}catch(e){
				console.error(key);
				console.error(e)
			}
		}
	}
	return map;
}

const afterUpdateQueryBuilder = function(e, rule) {
	console.log('afterUpdateQueryBuilder', e, rule)
	var operator = rule.operator.type;
	var autoSetToday = operator === "compare_month_and_day_with_now" || operator === "compare_year_month_day_with_now";
	if (rule.filter.plugin === 'datepicker') {
		var sel = rule.$el.find('.rule-value-container input');
		
      	var localDate = moment.utc().local().format('YYYY-MM-DD');
		var v = sel.val()
		if(autoSetToday && v === '') {				
			sel.val(localDate);
		}
		console.log('afterUpdateQueryBuilder ' + sel.val() )
		sel.datepicker('update');
	}
	
}

// FIXME
const buildSegmentDateFilter = function(fieldId, fieldLabel, fieldGroupLabel) {
	return {
	    id: fieldId,
	    label: fieldLabel,
	    type: 'date',
	    placeholder: 'yyyy-mm-dd',
	    validation: {
	    	format: 'yyyy-mm-dd', allow_empty_value : false
	    },
	    valueSetter: function(rule, selectedDates) {
			console.log("buildSegmentDateFilter.valueSetter ", selectedDates)
			
			selectedDates = typeof selectedDates === 'string' ? [selectedDates] : selectedDates;
			selectedDates = typeof selectedDates === 'object' ? selectedDates : [];
			
			// console.log('buildSegmentDateFilter.valueSetter ', rule.operator.type, value);	
					
			if(selectedDates.length > 0 ) {
				selectedDates[0] = moment.utc(selectedDates[0]).local().format('YYYY-MM-DD');
				selectedDates[1] = moment.utc(selectedDates[1]).local().format('YYYY-MM-DD');
				rule.$el.find('.rule-value-container input').each(function(i) {
					var node = $(this);
		    	 	node.val([selectedDates[i]]).css('width','100px');
					node.datepicker('update');
				});
		    }
	    },
	    valueGetter: function(rule) {	
			var selectedDates = [];
		    var sel = rule.$el.find('.rule-value-container input').each(function() {
				var v = $(this).css('width','100px').val().trim();
				if(v !== '') {				
					selectedDates.push(moment(v).utc().local().format('YYYY-MM-DD').valueOf());
				}
		    });
			console.log("buildSegmentDateFilter.valueGetter ", selectedDates)
			
	      	sel.attr("autocomplete","off").focus(function(){
	    	  	var popupTop = $(this).offset().top + $(this).height() + 8;
	    	  	$('div.datepicker-dropdown').css("top",popupTop);
	      	});		  
	      	return selectedDates.length === 1 ? selectedDates[0] : selectedDates;
	    },
	    plugin: 'datepicker',
	    plugin_config: {
	    	format: 'yyyy-mm-dd',
	    	todayBtn: 'linked',
	    	todayHighlight: true,
	    	autoclose: true,
	    	orientation: 'bottom'
	    },
		operators: getOperatorsForDateField(),
	    optgroup : fieldGroupLabel
	};
}

const buildSegmentProductFilter = function(fieldId, fieldLabel, fieldGroupLabel) {
	return {
	    id: fieldId,
	    label: fieldLabel,
	    type: 'string',
	    validation: { allow_empty_value : false },
	    valueSetter: function(rule, value) {
	    	if (rule.operator.nb_inputs == 1) value = [value];
		    rule.$el.find('.rule-value-container input').each(function(i) {
		    	 $(this).val(value).css('width','220px')
		    });
	    },
	    valueGetter: function(rule) {
	      	var productItemId = '';
		    rule.$el.find('.rule-value-container input').each(function() {
		    	productItemId = $(this).val();
		    });	  
	      	return productItemId;
	    },
	    input: function(rule, name) {
	    	//console.log('buildSegmentProductFilter.input', rule, name);
			var html = '<i class="fa fa-fw fa-search"></i><input placeholder="Item ID" class="form-control product_search" type="text" name="' + name + '">';
        	setTimeout(function(){
				$('input.product_search[name="' + name + '"]').focus(function(){
					console.log("TODO open dialog search product by keywords, then return product ID")
				});
			},1000)
	      	return html;
	    },
		operators: ["contains_any", "not_contains_any"],
	    optgroup : fieldGroupLabel
	};
}

const buildTouchpointFilter = function(fieldId, fieldLabel, fieldGroupLabel) {
	return {
	    id: fieldId,
	    label: fieldLabel,
	    type: 'string',
	    validation: { allow_empty_value : false },
	    valueSetter: function(rule, value) {
	    	if (rule.operator.nb_inputs == 1) value = [value];
		    rule.$el.find('.rule-value-container input').each(function(i) {
		    	 $(this).val(value).css('width','220px')
		    });
	    },
	    valueGetter: function(rule) {
	      	var touchpointId = '';
		    rule.$el.find('.rule-value-container input').each(function() {
		    	touchpointId = $(this).val();
		    });	  
	      	return touchpointId;
	    },
	    input: function(rule, name) {
	    	//console.log('buildSegmentProductFilter.input', rule, name);
			var html = '<i class="fa fa-fw fa-search"></i><input placeholder="Touchpoint ID" class="form-control touchpoint_search" type="text" name="' + name + '">';
        	setTimeout(function(){
				$('input.touchpoint_search[name="' + name + '"]').focus(function(){
					console.log("TODO open dialog search touchpoint by keywords, then return touchpoint ID")
				});
			},1000)
	      	return html;
	    },
		operators: ["contains_any", "not_contains_any"],
	    optgroup : fieldGroupLabel
	};
}

const buildCustomDateTimeFilter = function(fieldId, fieldLabel, fieldGroupLabel, forBirthday) {
	var operatorsList = forBirthday ? ["after", "before"] : ["exactly_after", "exactly_before"];
	return {
	    id: fieldId,
	    label: fieldLabel,
	    type: 'string',
	    validation: { allow_empty_value : false },
	    valueSetter: function(rule, values) {
		    rule.$el.find('.rule-value-container input').val(values[0]);
			rule.$el.find('.rule-value-container select').val(values[1]);
	    },
	    valueGetter: function(rule) {
		    var value1 = rule.$el.find('.rule-value-container input').val();
			var value2 = rule.$el.find('.rule-value-container select').val();
			console.log(value1, value2)
			var values = [value1, value2];
	      	return values;
	    },
	    input: function(rule, name) {
	    	//console.log('buildSegmentProductFilter.input', rule, name);
			var html = '<i class="fa fa-fw fa-calendar"></i><input placeholder="Enter a number" class="form-control" type="number" name="' + name + '">';
        	html += '<select class="form-control" name="select_time_unit_' + name + '" >';

			if(forBirthday === true) {
				html += '<option value="months">Month</option>';
				html += '<option value="days">Day</option>';
			}
			else {
				html += '<option value="years">Year</option>';
				html += '<option value="months">Month</option>';
				html += '<option value="exactly_days">Day</option>';
			}
			
			html += '</select>';
	      	return html;
	    },
		operators: operatorsList,
	    optgroup : fieldGroupLabel
	};
}

const buildSegmentJourneyFilter = function(fieldId, fieldLabel, fieldGroupLabel, journeyMapsForSegmentation) {
	return {
	    id: fieldId,
	    label: fieldLabel,
	    type: 'string',
	    valueSetter: function(rule, value) {
	    	//console.log('buildSegmentJourneyFilter.valueSetter', rule, value)
	    	
	    	// set data into DOM
	    	var selectNodes = rule.$el.find('.rule-value-container select');
    		selectNodes.each(function(i) {
 		    	$(this).val(value[i]).trigger('change');
 		    });
	    },
	    valueGetter: function(rule) {
	    	var data = [];
	    	rule.$el.find('.rule-value-container select').each(function() {
	    		data.push($(this).val());
	    	});
	    	//console.log('buildSegmentJourneyFilter.valueGetter', data)
	    	return data;
	    },
	    input: function(rule, name) {
	    	// console.log('buildSegmentJourneyFilter.input', rule, name)
			var html = '<select class="form-control segmentation_journey_map" >';
        	journeyMapsForSegmentation.forEach(function(journey){
				var option = '<option value="'+ journey.id +'" >' + journey.name + '</option>';
        		html += option;
			});
			html += '</select> <i class="fa fa-arrow-right" aria-hidden="true"></i> ';
			html += '<select class="form-control" > <option value="0"> IN ANY EVENT </option> <option value="1"> AWARENESS </option> <option value="2"> ATTRACTION </option> <option value="3"> ASK </option> ';
			html += ' <option value="4"> ACTION </option> <option value="5"> ADVOCACY </option> </select>';
	      	return html;
	    },
		
	    operators : [ "in_journey_map"],
	    optgroup : fieldGroupLabel
	};
}

const buildSegmentNumberSlider = function(id, label,  min, max, step, defaultValue, optgroup){
	return {
		'id' : id, 
		'label' : label,
		type : "integer",
		validation : {
			'min' : min,
			'max' : max,
			'step' : step
		},
		operators : getOperatorsForNumberField(),
		plugin: 'slider',
	    plugin_config: {
	      	'min' : min,
			'max' : max,
	      	value: defaultValue,
	      	tooltip: "always"
	    },
	    valueSetter: function(rule, value) {
	      if (rule.operator.nb_inputs == 1) value = [value];
	      rule.$el.find('.rule-value-container input').each(function(i) {
	        $(this).slider('setValue', value[i] || 0);
	      });
	    },
	    valueGetter: function(rule) {
	      var value = [];
	      rule.$el.find('.rule-value-container input').each(function() {
	        value.push($(this).slider('getValue'));
	      });
	      return rule.operator.nb_inputs == 1 ? value[0] : value;
	    },
	    'optgroup' : optgroup
	};
}

const segmentFiltersForCX = [
	// CFS Customer Feedback Score
	buildSegmentNumberSlider('totalCFS', 'Total CFS', 0, 100, 1, 0, 'CFS - Customer Feedback Score'),
	buildSegmentNumberSlider('positiveCFS', 'Positive CFS', 0, 100, 1, 0, 'CFS - Customer Feedback Score'),
	buildSegmentNumberSlider('neutralCFS', 'Neutral CFS', 0, 100, 1, 0, 'CFS - Customer Feedback Score'),
	buildSegmentNumberSlider('negativeCFS', 'Negative CFS', 0, 100, 1, 0, 'CFS - Customer Feedback Score'),

	// CES Customer Effort Score
	buildSegmentNumberSlider('totalCES', 'Total CES', 0, 100, 1, 0, 'CES - Customer Effort Score'),
	buildSegmentNumberSlider('positiveCES', 'Positive CES', 0, 100, 1, 0, 'CES - Customer Effort Score'),
	buildSegmentNumberSlider('neutralCES', 'Neutral CES', 0, 100, 1, 0, 'CES - Customer Effort Score'),
	buildSegmentNumberSlider('negativeCES', 'Negative CES', 0, 100, 1, 0, 'CES - Customer Effort Score'),
	
	// CSAT Customer Satisfaction Score
	buildSegmentNumberSlider('totalCSAT', 'Total CSAT', 0, 100, 1, 0, 'CSAT - Customer Satisfaction Score'),
	buildSegmentNumberSlider('positiveCSAT', 'Positive CSAT', 0, 100, 1, 0, 'CSAT - Customer Satisfaction Score'),
	buildSegmentNumberSlider('neutralCSAT', 'Neutral CSAT', 0, 100, 1, 0, 'CSAT - Customer Satisfaction Score'),
	buildSegmentNumberSlider('negativeCSAT', 'Negative CSAT', 0, 100, 1, 0, 'CSAT - Customer Satisfaction Score'),
	
	// NPS Net Promoter Score
	buildSegmentNumberSlider('totalNPS', 'Total NPS', 0, 100, 1, 0, 'NPS - Net Promoter Score'),
	buildSegmentNumberSlider('positiveNPS', 'Positive NPS', 0, 100, 1, 0, 'NPS - Net Promoter Score'),
	buildSegmentNumberSlider('neutralNPS', 'Neutral NPS', 0, 100, 1, 0, 'NPS - Net Promoter Score'),
	buildSegmentNumberSlider('negativeNPS', 'Negative NPS', 0, 100, 1, 0, 'NPS - Net Promoter Score')
];

const segmentFiltersForCustomerValue = [		
	// Scoring Model
	{
		id : "totalTransactionValue", // 3
		label : "Total Transaction Value",
		type : "double",
		validation : {
			min : 0,
			step : 0.01
		},
		operators : getOperatorsForNumberField(),
		optgroup : "Scoring Model"
	}, 
	{
		id : "totalLoyaltyScore", // 4
		label : "Total Loyalty Score",
		type : "integer",
		validation : {
			step : 1,
			min : 0
		},
		operators : getOperatorsForNumberField(),
		optgroup : "Scoring Model"
	},
	{
		id : "totalCreditScore", // 5
		label : "Customer Credit Score",
		type : "integer",
		validation : {
			min : 0,
			max : 100,
			step : 1
		},
		operators : getOperatorsForNumberField(),
		optgroup : "Scoring Model"
	},
	{ 
		id : "rfeScore", // 6
		label : "RFE Score",
		type : "integer",
		validation : {
			min : 0,
			step : 1
		},
		operators : getOperatorsForNumberField(),
		optgroup : "Scoring Model"
	},
	{ 
		id : "rfmScore", // 7
		label : "RFM Score",
		type : "integer",
		validation : {
			min : 0,
			step : 1
		},
		operators : getOperatorsForNumberField(),
		optgroup : "Scoring Model"
	},
	{
		id : "churnScore", // 8
		label : "Churn Score",
		type : "double",
		validation : {
			min : -100,
			max : 100,
			step : 0.01
		},
		operators : getOperatorsForNumberField(),
		optgroup : "Scoring Model"
	}
];

const advancedSegmentFilters = [
    // Consent Choices
	{
		id : "receiveWebPush",
		label : "Receive Web Notification",
		type : "integer",
		input : "radio",
		values : {
			"0" : "No data",
			"1" : "Subscribed",
			"-1" : "Unsubscribed"
		},
		operators : ["equal","not_equal" ],
		optgroup : "Data Privacy Consent/Agreement"
	}, 
	{
		id : "receiveAppPush",
		label : "Receive App Notification",
		type : "integer",
		input : "radio",
		values : {
			"0" : "No data",
			"1" : "Subscribed",
			"-1" : "Unsubscribed"
		},
		operators : ["equal","not_equal" ],
		optgroup : "Data Privacy Consent/Agreement"
	}, 
	{
		id : "receiveEmail",
		label : "Receive Email Marketing",
		type : "integer",
		input : "radio",
		values : {
			"0" : "No data",
			"1" : "Subscribed",
			"-1" : "Unsubscribed"
		},
		operators : [ "equal","not_equal" ],
		optgroup : "Data Privacy Consent/Agreement"
	 }, 
	{
		id : "receiveSMS",
		label : "Receive Mobile SMS",
		type : "integer",
		input : "radio",
		values : {
			"0" : "No data",
			"1" : "Subscribed",
			"-1" : "Unsubscribed"
		},
		operators : [ "equal","not_equal" ],
		optgroup : "Data Privacy Consent/Agreement"
	},
	{
		id : "receiveAds",
		label : "Receive Retargeting Ads",
		type : "integer",
		input : "radio",
		values : {
			"0" : "No data",
			"1" : "Subscribed",
			"-1" : "Unsubscribed"
		},
		operators : [ "equal","not_equal" ],
		optgroup : "Data Privacy Consent/Agreement"
	},
	
	// Marketing Data
	{
	    id: 'personalProblems',
	    label: 'Personal Problems',
	    type: 'string',
	    operators: ["contains_any", "not_contains_any"],
	    optgroup : "Marketing Information"
	},
	{
	    id: 'personalInterests',
	    label: 'Personal Interests',
	    type: 'string',
	    operators: ["contains_any", "not_contains_any"],
	    optgroup : "Marketing Information"
	},
	{
	    id: 'solutionsForCustomer',
	    label: 'Solutions For Customer',
	    type: 'string',
	    operators: ["contains_any", "not_contains_any"],
	    optgroup : "Marketing Information"
	},
	{
	    id: 'nextBestActions',
	    label: 'Next Best Actions',
	    type: 'string',
	    operators: ["contains_any", "not_contains_any"],
	    input: 'select',
	    values: {
	      "read-contents" : "READ CONTENTS",
	      "buy-some-items": "BUY SOME ITEMS",
	      "subscribe-a-service":"SUBSCRIBE A SERVICE",
	      "checkout-items-in-cart": "CHECKOUT ITEMS IN CART",
	      "checkin-location": "CHECKIN LOCATION",
	      "play-a-game": "PLAY A GAME",
	      "take-a-course": "TAKE A COURSE",
	      "watch-a-video": "WATCH A VIDEO",
	      "take-a-trip": "TAKE A TRIP",
	      "read-a-book" : "READ A BOOK"
	    },
	    optgroup : "Marketing Information"
	},
	{
	    id: 'mediaChannels',
	    label: 'Reachable Media Channel',
	    type: 'string',
	    operators: ["contains_any", "not_contains_any"],
	    optgroup : "Marketing Information"
	},
	{
	    id: 'contentKeywords',
	    label: 'Content Keywords',
	    type: 'string',
	    operators: ["contains_any", "not_contains_any"],
	    optgroup : "Marketing Information"
	},
	{
	    id: 'productKeywords',
	    label: 'Product Keywords',
	    type: 'string',
	    operators: ["contains_any", "not_contains_any"],
	    optgroup : "Marketing Information"
	}
];


const getSegmentCustomQueryFilter = function(){
	return typeof segmentCustomQueryEditor === 'object' ? segmentCustomQueryEditor.getValue().trim() : '';
}

const handleSegmentCustomQueryFilter = function(readOnly){
	if(typeof segmentDataModel === 'object') {
		var aql = segmentDataModel.customQueryFilter;
		initSegmentCustomQueryFilter(aql || '', readOnly)
	}
}

const initSegmentCustomQueryFilter = function(customQueryFilter, readOnly){
	var editor = window.segmentCustomQueryEditor;
	if(editor === false) {
		var node = $('#segment_custom_aql_editor').val(customQueryFilter)[0];
		// AQL code editor
		editor = CodeMirror.fromTextArea(node, {        
			 mode: "aql"
	    });

		if(typeof customQueryFilter === 'string'){
			editor.setValue(customQueryFilter);
		}
	}

	
	var holder = $('#segment_custom_query_holder');
	
	if(readOnly && customQueryFilter === ''){
		holder.hide();
	}
	else {
		var w = holder.width();
		editor.setSize((w > 100 ? w : 800), 230);
		editor.replaceRange(" ", { line: editor.lineCount() });
		editor.focus();
		editor.setCursor(editor.lineCount(), 0);
		editor.setOption("readOnly", readOnly);
	}

	
	window.segmentCustomQueryEditor = editor;
}

const loadSegmentBuilder = function(touchpointHubs,  behavioralEventMap, assetGroups, jsonQueryRules, callback) {
	
	var getDataFilter = function(touchpointHubs,  behavioralEventMap, assetGroups, journeyMapsForSegmentation) {
		var dataFilter = [ 
			{
				id : "status",
				label : "Custom Query Filter",
				type : "integer",
				input : "radio",
				values : {
					"1" : "Enabled"
				},
				operators : [ "equal"],
				optgroup : "Basic Profile Information"
			},
			{
				id : "dataContext",
				label : "Data Context",
				type : "integer",
				input : "radio",
				values : {
					"1" : "Production ",
					"0" : "QC Test ",
					"-1" : "Fake Data "
				},
				operators : [ "equal","not_equal" ],
				optgroup : "Basic Profile Information"
			},
			{
			    id: 'type',
			    label: 'Profile Type',
			    type: 'integer',
			    input: 'select',
			    values: {
			      0: 'ANONYMOUS_VISITOR',
			      1: 'LOGIN_USER_CONTACT',
			      2: 'CUSTOMER_CONTACT',
			      3: 'STUDENT_CONTACT',
			      4: 'CRM_IMPORTED_CONTACT',
			      5: 'DIRECT_INPUT_CONTACT',
			      6: 'INFLUENCER_CONTACT',
			      7: 'CLIENT_CONTACT',
			      8: 'B2B_PARTNER_CONTACT',
			      9: 'EMPLOYEE_CONTACT',
			      10:'KEY_ACCOUNT_CONTACT',
			      11:'SYSTEM_USER_CONTACT',
			    },
			    operators: ["equal","not_equal"],
			    optgroup : "Basic Profile Information"
			},
			{
			    id: 'funnelStage',
			    label: 'Data Funnel Stage',
			    type: 'string',
			    input: 'select',
			    values: {
			      "new-visitor": "New Visitor",
			      "returning-visitor": "Returning Visitor",
			      "lead": "Lead",
			      "prospect": "Prospect",
			      "new-customer" : "New Customer",
			      "engaged-customer": "Engaged Customer",
			      "happy-customer": "Happy Customer",
			      "customer-advocate": "Customer Advocate",
			      "unhappy-customer": "Unhappy Customer",
			      "terminated-customer": "Terminated Customer"
			    },
			    operators: ["equal","not_equal"],
			    optgroup : "Basic Profile Information"
			},
			buildSegmentJourneyFilter("cdp_profile__inJourneyMaps","Data Journey Map","Basic Profile Information",journeyMapsForSegmentation),
			buildSegmentNumberSlider('dataQualityScore', 'Data Quality Score', 0, maxTotalDataQualityScore, 1, 0, 'Basic Profile Information'),
			{
			    id: 'dataLabels',
			    label: 'Data Labels',
			    type: 'string',
			    operators: ["contains", "not_contains", "contains_any", "not_contains_any"],
			    optgroup : "Basic Profile Information"
			},
			buildSegmentDateFilter("createdAt","Created Date","Basic Profile Information"),
			buildCustomDateTimeFilter("createdAt#1","Created in Time Range","Basic Profile Information"),
			buildSegmentDateFilter("updatedAt","Updated Date by CDP","Basic Profile Information"),
			buildSegmentDateFilter("updatedByCrmAt","Updated Date by CRM","Basic Profile Information"),
			
			// Profile Key
			{
				id : "_key",
				label : "Profile ID",
				type : "string",
				operators: ["equal","not_equal"],
				optgroup : "Profile Key Information"
			}, 
			{
				id : "visitorId",
				label : "Web Visitor ID",
				type : "string",
				operators: ["equal","not_equal", "is_empty", "is_not_empty", "is_null", "is_not_null"],
				optgroup : "Profile Key Information"
			}, 
			{
				id : "fingerprintId",
				label : "Fingerprint ID",
				type : "string",
				operators: ["equal","not_equal", "is_empty", "is_not_empty", "is_null", "is_not_null"],
				optgroup : "Profile Key Information"
			},
			{
				id : "crmRefId",
				label : "Imported CRM ID",
				type : "string",
				operators : getOperatorsForStringField(),
				optgroup : "Profile Key Information"
			},
						{
				id : "primaryEmail",
				label : "Primary Email",
				type : "string",
				operators : getOperatorsForStringField(),
				optgroup : "Profile Key Information"
			}, 
			{
				id : "primaryPhone",
				label : "Primary Phone",
				type : "string",
				operators : getOperatorsForStringField(),
				optgroup : "Profile Key Information"
			}, 
			{
				id : "primaryUsername",
				label : "Primary Username",
				type : "string",
				operators : getOperatorsForStringField(),
				optgroup : "Profile Key Information"
			}, 
			{
			    id: 'governmentIssuedIDs',
			    label: 'Citizen/Passport IDs',
			    type: 'string',
			    operators: ["contains_any", "not_contains_any"],
			    optgroup : "Profile Key Information"
			},
			{
			    id: 'applicationIDs',
			    label: 'Application IDs',
			    type: 'string',
			    operators: ["contains_any", "not_contains_any"],
			    optgroup : "Profile Key Information"
			},
						
			// Personal Information
			buildCustomDateTimeFilter("dateOfBirth#1","Birthday Event","Personal Event", true),
			{
				id : "firstName",
				label : "First Name",
				type : "string",
				operators : getOperatorsForStringField(),
				optgroup : "Personal Information"
			}, {
				id : "lastName",
				label : "Last Name",
				type : "string",
				operators : getOperatorsForStringField(),
				optgroup : "Personal Information"
			},
			{
				id : "primaryNationality",
				label : "Primary Nationality",
				type : "string",
				operators : getOperatorsForStringField(),
				optgroup : "Personal Information"
			}, 
			{
				id : "permanentLocation",
				label : "Permanent Location Address",
				type : "string",
				operators : getOperatorsForStringField(),
				optgroup : "Personal Information"
			}, 
			{
				id : "livingLocation",
				label : "Living Location Address",
				type : "string",
				operators : getOperatorsForStringField(),
				optgroup : "Personal Information"
			}, 
			{
				id : "livingCity",
				label : "Living City",
				type : "string",
				operators : getOperatorsForStringField(),
				optgroup : "Personal Information"
			},
			{
				id : "livingState",
				label : "Living State",
				type : "string",
				operators : getOperatorsForStringField(),
				optgroup : "Personal Information"
			},
			buildSegmentNumberSlider('age', 'Age', 0, 150, 1, 0, 'Personal Information'),
			buildSegmentDateFilter("dateOfBirth","Date of Birth","Personal Information"),		
			{
				id : "gender",
				label : "Gender",
				type : "integer",
				input : "radio",
				values : {
					0 : "Female",
					1 : "Male",
					7 : "Unknown",
				},
				operators : [ "equal" ,"not_equal" ],
				optgroup : "Personal Information"
			}, 
			buildSegmentNumberSlider('genderProbability', 'Gender Probability', 0, 100, 1, 0, 'Personal Information'),
			
			// Scoring Model
			{
				id : "totalLeadScore",  // 2
				label : "Total Lead Score",
				type : "integer",
				validation : {
					min : 0,
					max : 100,
					step : 1
				},
				operators : getOperatorsForNumberField(),
				optgroup : "Scoring Model"
			},
			{
				id : "totalProspectScore",  // 3
				label : "Total Prospect Score",
				type : "integer",
				validation : {
					step : 1
				},
				operators : getOperatorsForNumberField(),
				optgroup : "Scoring Model"
			}, 
			{
				id : "totalEngagementScore", // 4
				label : "Customer Engagement Score",
				type : "integer",
				validation : {
					min : 0,
					step : 1
				},
				operators : getOperatorsForNumberField(),
				optgroup : "Scoring Model"
			}, 
			{
				id : "totalCAC", // 5
				label : "Customer Acquisition Cost",
				type : "double",
				validation : {
					min : 0,
					step : 0.01
				},
				operators : getOperatorsForNumberField(),
				optgroup : "Scoring Model"
			},
			{
				id : "totalCLV", // 2
				label : "Customer Lifetime Value",
				type : "double",
				validation : {
					step : 0.01
				},
				operators : getOperatorsForNumberField(),
				optgroup : "Scoring Model"
			},
			
			// Data Traffic Sources
			//buildTouchpointFilter('topEngagedTouchpointIds', 'Top Engaged Touchpoints',  'Data Sources'),
			{
			    id: 'touchpoint__name',
			    field : "cdp_touchpoint__name",
			    label: 'Engaged Touchpoint Names',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Data Sources"
			},
			{
			    id: 'engagedTouchpointIds',
			    field : "cdp_trackingevent__srcTouchpointUrl",
			    label: 'Source Touchpoint URL',
			    type: 'string',
			    input: 'select',
			    values: touchpointHubs,
			    operators: ["data_from","data_contains"],
			    optgroup : "Data Sources"
			},
			{
			    id: 'referrerChannels',
			    label: 'Web Referrer Channel',
			    type: 'string',
			    input: 'select',
			    values: getTouchpointHubHosts(touchpointHubs),
			    operators: ["has_key", "not_has_key"],
			    optgroup : "Data Sources"
			},
			{
			    id: 'lastTouchpoint__name',
			    label: 'Last Touchpoint Name',
			    type: 'string',
			    operators: ["sub_field_contains", "sub_field_equals"],
			    optgroup : "Data Sources"
			},
			{
			    id: 'lastTouchpoint__url',
			    label: 'Last Touchpoint URL',
			    type: 'string',
			    operators: ["sub_field_contains", "sub_field_equals"],
			    optgroup : "Data Sources"
			},
			{
			    id: 'lastSeenIp',
			    label: 'Last Seen IP Address',
			    type: 'string',
			    operators: ["equal"],
			    optgroup : "Data Sources"
			}, 
			
			// Sale Sources
			{
			    id: 'saleAgencies',
			    label: 'Sales Agency / Sales Source',
			    type: 'string',
				operators: ["contains", "not_contains", "contains_any", "not_contains_any"],
			    optgroup : "Sale Sources"
			},
			{
			    id: 'saleAgents',
			    label: 'Sales Agent / Sales Person',
			    type: 'string',
			    operators: ["contains", "not_contains", "contains_any", "not_contains_any"],
			    optgroup : "Sale Sources"
			},
			
			// Education
			{
			    id: 'softSkills',
			    label: 'Soft Skills',
			    type: 'string',
			    operators: ["contains", "not_contains", "contains_any", "not_contains_any"],
			    optgroup : "Education"
			},
			{
			    id: 'learningHistory',
			    label: 'Learning History',
			    type: 'string',
			    operators: ["contains", "not_contains", "contains_any", "not_contains_any"],
			    optgroup : "Education"
			},
			{
			    id: 'studyCertificates',
			    label: 'Study Certificates',
			    type: 'string',
			    operators: ["contains", "not_contains", "contains_any", "not_contains_any"],
			    optgroup : "Education"
			},
			
			
			// Working Experience
			{
			    id: 'jobTitles',
			    label: 'Job Title',
			    type: 'string',
			    operators: ["contains", "not_contains", "contains_any", "not_contains_any"],
			    optgroup : "Working Experience"
			},
			{
			    id: 'workingHistory',
			    label: 'Working History',
			    type: 'string',
			    operators: ["contains", "not_contains", "contains_any", "not_contains_any"],
			    optgroup : "Working Experience"
			},
			{
			    id: 'businessIndustries',
			    label: 'Business Industry',
			    type: 'string',
			    operators: ["contains", "not_contains", "contains_any", "not_contains_any"],
			    optgroup : "Working Experience"
			},
			
		
			// Behavioural Event
			{
			    id: 'behavioralEvents',
			    label: 'Behavioral Event Metrics',
			    type: 'string',
			    input: 'select',
			    values: behavioralEventMap,
			    operators: ["contains_any", "not_contains_any"],
			    optgroup : "Behavioural Event"
			},
			{
			    id: 'cdp_trackingevent__metricName',
			    label: 'Event Metric Name',
			    type: 'string',
			    operators: getOperatorsForStringEqualityField(),
			    optgroup : "Behavioural Event"
			},
			{
			    id: 'cdp_trackingevent__metricValue',
			    label: 'Event Metric Value',
			    type: 'integer',
			    operators: getOperatorsForNumberField(),
			    optgroup : "Behavioural Event"
			},
			{
			    id: 'cdp_trackingevent__timeSpent',
			    label: 'Time Spent in seconds',
			    type: 'integer',
			    operators: getOperatorsForNumberField(),
			    optgroup : "Behavioural Event"
			},
			{
				id: 'cdp_trackingevent__fingerprintId',
			    label: 'Web Fingerprint ID',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Behavioural Event"
			},
			buildSegmentDateFilter("cdp_trackingevent__createdAt", "Event Creation Date", "Behavioural Event"),
			buildCustomDateTimeFilter("cdp_trackingevent__createdAt#1","Event Time Range","Behavioural Event"),
			buildSegmentDateFilter("cdp_trackingevent__updatedAt", "Event Modification Date", "Behavioural Event"),
			{
			    id: 'cdp_trackingevent__srcTouchpointName',
			    label: 'Source from Touchpoint Name',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Behavioural Event"
			},
			{
			    id: 'cdp_trackingevent__srcTouchpointUrl',
			    label: 'Source from Touchpoint URL',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Behavioural Event"
			},
			{
			    id: 'cdp_trackingevent__refTouchpointName',
			    label: 'Referrer from Touchpoint Name',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Behavioural Event"
			},
			{
			    id: 'cdp_trackingevent__refTouchpointUrl',
			    label: 'Referrer from Touchpoint URL',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Behavioural Event"
			},
			{
			    id: 'cdp_trackingevent__locationName',
			    label: 'Event from Location Name',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Behavioural Event"
			},
			{
			    id: 'cdp_trackingevent__locationCode',
			    label: 'Event from Location Code',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Behavioural Event"
			},
			{
			    id: 'cdp_trackingevent__sourceIP',
			    label: 'Event from Source IP',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Behavioural Event"
			},			
			{
				id: 'cdp_trackingevent__refCampaignId',
			    label: 'Event from Campaign ID',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Behavioural Event"
			},
			{
			    id: 'cdp_trackingevent__environment',
			    label: 'Data Environment',
			    type: 'string',
			    input: 'select',
			    values: {
			      "pro": "In Production",
			      "qctest": "In QC Test",
			      "dev": "In Development"
			    },
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Behavioural Event"
			},
			{
				id: 'cdp_trackingevent__fraudScore',
			    label: 'Fraud Score',
			    type: 'integer',
			    operators: ["data_equals"],
			    optgroup : "Behavioural Event"
			},
			// Transaction Event Information
			{
			    id: 'cdp_trackingevent__transactionId',
			    label: 'Transaction ID',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Transaction Event"
			},
			buildSegmentDateFilter("cdp_trackingevent__createdAt#2", "Transaction Date", "Transaction Event"),
			{
			    id: 'cdp_trackingevent__transactionValue',
			    label: 'Transaction Value',
			    type : "double",
				validation : {
					step : 0.01
				},
				operators : getOperatorsForNumberField(),
			    optgroup : "Transaction Event"
			},
			{
			    id: 'cdp_trackingevent__transactionStatus',
			    label: 'Transaction Status',
				type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Transaction Event"
			},
			{
			    id: 'cdp_trackingevent__transactionCode',
			    label: 'Transaction Code',
			    type: 'integer',
			    operators : getOperatorsForNumberField(),
			    optgroup : "Transaction Event"
			},
			{
			    id: 'cdp_trackingevent__transactionDiscount',
			    label: 'Transaction Discount',
			    type : "double",
				validation : {
					step : 0.01
				},
				operators : getOperatorsForNumberField(),
			    optgroup : "Transaction Event"
			},
			{
			    id: 'cdp_trackingevent__transactionCurrency',
			    label: 'Transaction Currency',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Transaction Event"
			},
			{
			    id: 'cdp_trackingevent__transactionPayment',
			    label: 'Transaction Payment',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Transaction Event"
			},
			
			
			// Device Information
			{
				id: 'cdp_trackingevent__deviceType',
			    label: 'Device Type',
			    type: 'string',
			    operators: getOperatorsForStringEqualityField(),
			    input : "radio",
				values : {
					"General_Desktop" : "Desktop PC",
					"General_Mobile" : "Mobile",
					"General_Tablet" : "Tablet"
				},
			    optgroup : "Device Information"
			},
			{
				id: 'cdp_trackingevent__deviceId',
			    label: 'Device ID',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Device Information"
			},
			{
				id: 'cdp_trackingevent__deviceName',
			    label: 'Device Name',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Device Information"
			},
			{
				id: 'cdp_trackingevent__deviceOS',
			    label: 'Device OS',
			    type: 'string',
			    operators: ["data_equals", "data_contains"],
			    optgroup : "Device Information"
			},
			{
			    id: 'lastUsedDeviceId',
			    label: 'Last Used Device ID',
			    type: 'string',
			    operators: ["equal"],
			    optgroup : "Device Information"
			},
			
			// Asset Item: Product 
			buildSegmentProductFilter('shoppingItemsIds', 'Shopping Item ID',  'Product Information'),
			buildSegmentProductFilter('purchasedItemIds', 'Purchased Item ID',  'Product Information'),
			{
			    id: 'productKeywords#1',
			    label: 'Purchased Item Category',
			    type: 'string',
			    input: 'select',
			    values: window.PRODUCT_ITEM_CATEGORIES || {},
			    operators: ["contains_any", "not_contains_any"],
			    optgroup : 'Product Information'
			}
		];
		
		// add event filters of asset group 
		assetGroups.forEach(function(e){
			var assetGroupFilter = 
			{ 
				id : "assetGroup_"+e.id,
				field : "behavioralEvents",
				label : "Events in Asset Group: " + e.title,
				type: 'string',
			    input: 'select',
			    values: e.eventNamesForSegmentation,
			    operators: ["contains_any", "not_contains_any"],
				optgroup : "Behavioral Events in Asset Group"
			};
			dataFilter.push(assetGroupFilter)
		});
		
		dataFilter = dataFilter.concat(advancedSegmentFilters);
		dataFilter = dataFilter.concat(segmentFiltersForCustomerValue);
		dataFilter = dataFilter.concat(segmentFiltersForCX);
		
		return dataFilter;
	}
	// end of getDataFilter
	
	// request server
	LeoAdminApiUtil.getSecuredData('/cdp/journeymap/list-all-for-segmentation', {}, function(json){
		if (json.httpCode === 0 && json.errorMessage === '') {
			var journeyMapsForSegmentation = json.data;
			
			var rulesOfQuery = jsonQueryRules || false;
			
			//init UI of visual query builder
			var sel = $('#segment-builder-holder');
			
			sel.on('afterCreateRuleFilters.queryBuilder', function(e, rule) {
			    // Your custom function
			    console.log('QueryBuilder afterUpdateRuleFilter', e, rule);
				var sel = 'select[name="' + rule.id + '_filter"]';
				setTimeout(function(){
					$(sel).select2();
				},300)
			}).on('afterUpdateRuleValue.queryBuilder', afterUpdateQueryBuilder);
			
			sel.queryBuilder({
				plugins : [ 'bt-tooltip-errors'],
				operators: getOperators(),
				filters : getDataFilter(touchpointHubs, behavioralEventMap, assetGroups, journeyMapsForSegmentation),
				rules : rulesOfQuery
			});
			
			if(typeof callback === 'function'){
				callback();
			}
			
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
	});
}



//
const deleteSegment = function() {
	if(typeof segmentDataModel === "object" && window.segmentDataModel.id != '' ) {
		$('#delete_callback').val('');
		$('#confirmDeleteParentChildDialog').modal({ focus: true });
		
		// init checkbox
		$('#deleteChildItemMsg').text("Delete all profiles and related data in this segment ! (Be Careful)");
		$('#deleteChildItemCheckbox').prop('checked', false);
		$('#deleteChildItem').show();
		
        var callback = "deleteSegment" + segmentDataModel.id;
        
        //callback for OK button
	    window[callback] = function () {
	    	var urlStr = baseLeoAdminUrl + '/cdp/segment/delete';
	    	
	    	var checked = $('#deleteChildItemCheckbox').is(":checked");
			iziToast.info({
        	    title: 'Data Deletion',
        	    message: 'Start the job to delete the segment "'+segmentDataModel.name +'"'
        	});

			// the input
			var params = {'segmentId': segmentDataModel.id, 'deleteAllProfiles': checked};
			
	        LeoAdminApiUtil.callPostAdminApi(urlStr, params, function (json) {
	            if (json.httpCode === 0 ) {
					var c = json.data;
	                if( c >= 0  ){
	                	$('#deleteChildItem').hide();
	                	$('#deleteChildItemMsg').text('')
	                	
	                	iziToast.success({
	                	    title: 'OK',
	                	    message: 'Successfully deleted the segment "'+segmentDataModel.name +'". Total deleted profile: ' + c,
	                	    onClosing: function(instance, toast, closedBy){
	                	    	location.hash = "calljs-leoCdpRouter('Segment_Management')";
	                	    }
	                	});
	                } 
	                else {
	                	iziToast.error({
	                	    title: 'Error',
	                	    message: json.data,
	                	    onClosing: function(instance, toast, closedBy){
	                	    	location.reload(true);
	                	    }
	                	});
	                }
	            } else {
	                $('#error-on-save').html(json.errorMessage).show().delay(5000).fadeOut('slow');
	                LeoAdminApiUtil.logErrorPayload(json);
	            }
	        });
	    }
	    $('#delete_parent_callback').val(callback);
	    $('#deletedParentInfoTitle').html(segmentDataModel.name);
	    $('#deletedParentInfoMsg').html('Do you want to delete the segment ?');
	}
}

const loadProfileViewForSegmentParams = function (data) {
	var holder = $('#segment-builder-holder');
	var result = holder.queryBuilder('getRules');
	data.jsonQueryRules = typeof result === 'object' ? JSON.stringify(result) : "";
	data.customQueryFilter = getSegmentCustomQueryFilter()
	
	data.realtimeQuery = holder.data('realtime-query') === true;
	
	data.withLastEvents = holder.data('with-last-events') === true;	
	data.segmentId = segmentDataModel.id;
	console.log('loadProfileViewForSegmentParams', result)
	return JSON.stringify(data);
}
 
const loadProfilesInSegment = function(realtimeQuery, timeToLoad) {
	var holderDataTable = $('#profile_list_querybuilder');
	if(typeof segmentDataModel === 'object'){
		// make sure segment is loaded in DOM
		if(realtimeQuery === true){
			segmentDataModel.realtimeQuery = true;
			
			var dataTable = holderDataTable.DataTable();
			if(dataTable) {
				dataTable.clear();
				// wait for DOM is clear OK
				setTimeout(dataTable.ajax.reload, 888);
			}
			
		} else {
			var loader = $('#segment_profile_list_loader');
			loader.show();
			
			timeToLoad = typeof timeToLoad === 'number' ? timeToLoad : 900;
			setTimeout(function(){
				var urlBySegmentId = '/cdp/segment/profile-query-builder';
		    	loadProfileViewForSegment(holderDataTable, urlBySegmentId);
				$('#profile-list-panel').show();
				loader.hide();
			},timeToLoad)
		}
	}
}

// 
const loadProfileViewForSegment = function(holderDataTable, ajaxUrl) {
	var hasPersonalization = typeof initPersonalizationInSegment === "function" && window.defaultPersonalizationService.length > 0;
	var columnDef =  [
    	{
            "render": function (data, type, row) {
            	var name = 'Web Visitor';
            	try {
            		var firstName = row.firstName || '';
            		var lastName = row.lastName || '';
            		if(firstName.length > 0 || lastName.length > 0){
                		name = textTruncate(firstName + ' ' + lastName,32);
                	}
        		}
        		catch(err) {
        		   console.log(err)
        		}
            	
                var callJsViewStr = "#calljs-leoCdpRouter('Customer_Profile_Info','" + data + "')";
                return '<div style="font-size:13.5px;"><a target="_blank" title="Profile Report" href="' + callJsViewStr + '" >' + name + '</a></div>';
            },
            "targets": 0,
            "orderable": false
        },
        {
            "render": function (data, type, row) {
            	var text = profileContactTypes[data].replace('_CONTACT','');
				var callJsViewStr = "#calljs-leoCdpRouter('Customer_Profile_Info','" + row.id + "')";
                var aNode = '<a target="_blank" title="Profile Report" href="' + callJsViewStr + '" >' + text + '</a>';
                return '<div class="datatable_text" style="font-size:10.8px;">'  + aNode + '</div>';
            },
            "targets": 1,
            "orderable": false
        },
        {
            "render": function (data, type, row) {
            	var genderText = "-";
            	if(data === 0){
            		genderText = '<i class="fa fa-female"></i> Female';
            	}
            	else if(data === 1){
            		genderText = '<i class="fa fa-male"></i> Male';
            	}
            	else if(data >= 2 && data <=6){
            		genderText = "LGBT";
            	}
                return '<div class="datatable_text" style="font-size:10.8px;" >'  + genderText + '</div>';
            },
            "targets": 2,
            "orderable": false
        },
        {
            "render": function (data, type, row) {
            	//Email
            	data = typeof data === "string" ? data : "-";
            	var callJsViewStr = "#calljs-leoCdpRouter('Customer_Profile_Info','" + row.id + "')";
            	var link = '<a title="Profile Email" href="' + callJsViewStr + '" >' + textTruncate(data, 24) + '</a>';
            	return '<div class="datatable_text" style="font-size:11px;" >'  + link + '</div>';
            },
            "targets": 3,
            "orderable": false
        },
        {
            "render": function (data, type, row) {
            	//Phone
            	data = typeof data === "string" ? data : "-";
            	var callJsViewStr = "#calljs-leoCdpRouter('Customer_Profile_Info','" + row.id + "')";
            	var link = '<a title="Profile Phone" href="' + callJsViewStr + '" >' + textTruncate(data, 20) + '</a>';
            	return '<div class="datatable_text" style="font-size:11px;" >'  + link + '</div>';
            },
            "targets": 4,
            "orderable": false
        },
        {
            "render": function (data, type, row) {
            	var fullText = row.lastTouchpoint ? row.lastTouchpoint.name : '';
                return '<div class="datatable_text" style="font-size:11px;" title="' + fullText + '" >'  + textTruncate(fullText, 45) + '</div>';
            },
            "targets": 5,
            "orderable": false
        },
        {
            "render": function (data, type, row) {
                return '<div class="datatable_text text-center">'  + data + '</div>';
            },
            "targets": 6,
            "orderable": false
        },
        {
            "render": function (data, type, row) {
                return '<div class="datatable_text text-center">'  + data + '</div>';
            },
            "targets": 7,
            "orderable": false
        },
        {
            "render": function (data, type, row) {
            	return '<div class="datatable_text text-center">'  + data + '</div>';
            },
            "targets": 8,
            "orderable": false
        },
        {
            "render": function (data, type, row) {
            	if(data) {            		
					var date = toLocalDateTime(data);
                    return '<div class="small">'  + date + '</div>';
            	}
            	return "";
            },
            "targets": 9,
            "orderable": false
        }
    ];
	
    var usersession = getUserSession();
    if (typeof usersession === 'string') {
        var table = holderDataTable.DataTable({
        	"lengthMenu": [[20, 30, 50], [20, 30, 50]],
        	'processing': true,
            'serverSide': true,
            'searching': false,
            'serverMethod': 'POST',
            'ajax': {
                url: ajaxUrl,
                contentType: 'application/json',
                beforeSend: function (request) {
					$('#btn-run-querybuilder, #btn-reset-querybuilder').attr('disabled','disabled')
                    request.setRequestHeader("leouss", usersession);
                },
                data: loadProfileViewForSegmentParams
            },
            'columnDefs': columnDef,
            'columns': [
				{
                    "data": "id" // 0
                },
                {
                    "data": "type" // 1
                },
                {
                    "data": "gender" // 2
                },                     
                {
                    "data": "primaryEmail" // 3
                },
                {
                    "data": "primaryPhone" // 4
                },
                {
                    "data": "lastTouchpoint" // 5
                },
                {
                    "data": "dataQualityScore" // 6
                },
                {
                    "data": "totalLeadScore" // 7
                },
                {
                    "data": "totalCLV" // 8
                },
                {
                    "data": "updatedAt" // 9
                }
            ]
        });

		table.on('draw', function () {
			// show run buttons
			$('#btn-run-querybuilder, #btn-reset-querybuilder').removeAttr('disabled');
			
			var c = table.rows().count();
			console.log('loadProfileViewForSegment is displayed in table, count: ' + c);
			if(hasPersonalization && c > 0){
				var row = table.row(0).data();
				initPersonalizationInSegment(row.id);
            }
		});
    }
}    

const loadSegmentListByFilter = function(containerDomSel, loaderDomSel, tableDomSel, segmentFilterParams, loadDataCallback) {
    var collumnList = [
    	{
           "data": "id" // 0 
        },
   		{
           "data": "name" // 1
       	},
       	{
            "data": "totalCount" // 2
        },
       	{
           "data": "description" // 3
       	},
       	{
            "data": "ownerUsername" // 4
      	},
       	{
           "data": "createdAt" // 5
       	},
       	{
           "data": "updatedAt" // 6
       	}
   	];
    
    var usersession = getUserSession();
    if (usersession) {
        var obj = $(tableDomSel).DataTable({
        	"lengthMenu": [[20, 30, 40], [20, 30, 40]],
        	'processing': true,
            'serverSide': true,
            'searching': true,
            'search': {
                return: true,
            },
            'ordering': true,
            'serverMethod': 'POST',
            'ajax': {
                url: baseLeoAdminUrl + "/cdp/segments/filter",
                contentType: 'application/json',
                beforeSend: function (request) {
                	$(loaderDomSel).show();
                	$(containerDomSel).hide();
                    request.setRequestHeader("leouss", usersession);
                },
                data: function (d) {
                	var order = d.order[0];
                	d.sortField = collumnList[order.column].data;
                	d.sortAsc = order.dir === "asc" ;
                	d.searchValue = d.search.value;
                	delete d.order; delete d.search;
                	
                	if(typeof segmentFilterParams === "function"){
                		d = segmentFilterParams(d);
                	}
                    return JSON.stringify(d);
                },
                dataSrc: function ( json ) {
                	var canInsertData = json.canInsertData;
		 	    	var canEditData = json.canEditData;
		     		var canDeleteData = json.canDeleteData;
		     		
                	$(tableDomSel).data('canInsertData', canInsertData)
		     		$(tableDomSel).data('canEditData', canEditData)
		     		$(tableDomSel).data('canDeleteData', canDeleteData)
		     		
		     		if(typeof loadDataCallback === "function"){
						loadDataCallback(json)
					}
            		
            		// done show UI
            		$(loaderDomSel).hide();
                	$(containerDomSel).show();
                	
            		return json.data;
                 }
            },
            'columnDefs': [
            	{
                    "render": function (data, type, row) {
                        return '<i class="fa fa-users" aria-hidden="true"></i>';
                    },
                    "targets": 0,
                    "orderable": false
                },
            	{
                    "render": function (data, type, row) {
                    	var callJsViewStr = "#calljs-leoCdpRouter('Segment_Details','" + row.id + "')";
                    	var text = textTruncate(data,50);
                        return '<div class="segment_name_in_list"> <mark> <a title="Segment Name: '+data+'" href="' + callJsViewStr + '" >' + text + '</a> </mark></div>';
                    },
                    "targets": 1
                },
                {
                    "render": function (data, type, row) {
						var s =  data.toLocaleString();
                        return '<div class="segment_size " style="min-width:95px; font-size:14.2px" title="The size of segment: '+s+'" ><mark>' + s + '</mark></div>';
                    },
                    "targets": 2
                },
            	{
                    "render": function (data, type, row) {
                        return '<div  style="font-size:0.8em;" title="' + data + '" >' + textTruncate(data, 150) + '</div>';
                    },
                    "targets": 3,
                    "orderable": false
                },
                {
                    "render": function (data, type, row) {
                    	return '<div style="min-width:90px; text-align:center" >' +  data + '</div>';
                    },
                    "targets": 4,
                    "orderable": false
                },
                {
                    "render": function (data, type, row) {
                        var date = toLocalDateTime(data);
                        return '<div class="small text-center" style="color:#3300ff;min-width:80px" >'  + date + '</div>';
                    },
                    "targets": 5
                },
                {
                    "render": function (data, type, row) {
                    	if(data){
                    		 var date = toLocalDateTime(data);
                    		 return '<div class="small text-center" style="color:#3300ff;min-width:80px" >'  + date + '</div>';
                    	}
                       return '-';
                    },
                    "targets": 6
                },
                
                {
                    "render": function (data, type, row) {
                        var html = '';
                        var callJsViewStr = "#calljs-leoCdpRouter('Segment_Details','" + row.id + "')";
                        html += '<a class="control" title="Segment Report" href="' + callJsViewStr + '" > <i class="fa fa-info-circle" aria-hidden="true"></i> View</a>';
                        
                    	if( $(tableDomSel).data('canEditData') ){	
                    		var callJsEditStr = "#calljs-leoCdpRouter('Segment_Builder','" + row.id + "')";
                    		html += '<br> <a class="control" title="Segment Builder" href="' + callJsEditStr + '" > <i class="fa fa-pencil-square-o" aria-hidden="true"></i> Edit</a> ';
                    		//var callJsCopyStr = "#calljs-leoCdpRouter('Segment_Builder_Copy','" + row.id + "')";
                    		//html += '<br> <a class="control" title="Copy this segment" href="' + callJsCopyStr + '" > <i class="fa fa-clone" aria-hidden="true"></i> Copy</a>';
                    	}
                        return html;
                    },
                    "targets": 7 
                }
            ],
            'columns': collumnList,
            'order': [[6, 'desc']]
        });
        return obj;
    }
}


// segment-builder

var segmentQueryBuilderCallback =  function(){
	$('#btn-reset-querybuilder').on('click', function() {
		$('#segment-builder-holder').queryBuilder('reset');
	});
	
	// show UI
    $("#segment_builder_div").show();
    $("#segment_builder_loader").hide();
    
	loadSegmentStatistics(segmentDataStats);
	
	// compute segment size by submiting query to database
	var runQueryBuilder = function() {
		var btn = $(this);
		var result = $('#segment-builder-holder').queryBuilder('getRules');
		if (!$.isEmptyObject(result)) {
			$('#btn-run-querybuilder, #btn-reset-querybuilder').attr('disabled','disabled')
			
			$("#segment_profile_list").hide();
		    $("#segment_profile_list_loader").show();
			
			var jsonQueryStr = JSON.stringify(result);
			//console.log('jsonQueryStr ' + jsonQueryStr)
			
			var params = {};
			params.jsonQueryRules = jsonQueryStr;
			params.customQueryFilter = getSegmentCustomQueryFilter();
			
			// call and show statistics
			var urlStr = baseLeoAdminUrl + '/cdp/segment/statistics';
	        LeoAdminApiUtil.callPostAdminApi(urlStr, params , function (json) {
	        	$("#segment_profile_list").show();
			    $("#segment_profile_list_loader").hide();
			    btn.removeAttr('disabled')
	        	
	            if (json.httpCode === 0 && typeof json.data === 'object') {
	               	loadSegmentStatistics(json.data);
	   				
	   			    // load profile list table
	               	loadProfilesInSegment(true);
	            } else {
	            	iziToast.error({
                	    title: 'Error',
                	    message: json.data,
                	    onClosing: function(instance, toast, closedBy){
                	    	location.reload(true);
                	    }
                	});
	            }
	        });
		}
	}
	
	$('#btn-run-querybuilder').click(runQueryBuilder);
}
	
var getSegmentBuilderData = function(id) {
    
	var urlStr = baseLeoAdminUrl + '/cdp/segment/load';
    // call server
    LeoAdminApiUtil.callPostAdminApi(urlStr, { 'id': id }, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
        	segmentDataStats = json.data.segmentStats;
        	var behavioralEventMap = json.data.behavioralEventMap;
        	var assetGroups = json.data.assetGroups;
        	var touchpointHubs = json.data.touchpointHubs;
        	
    		// only super-admin role can remove the segment 
    		var segmentData = json.data.segmentData;
            var segmentName = segmentData.name;
			var customQueryFilter = segmentData.customQueryFilter;
            var managedBySystem = segmentData.managedBySystem;
            var jsonQueryRules = JSON.parse(segmentData.jsonQueryRules);
            
            document.title = 'Segment Builder: ' + segmentName;
            $('#segment_name').text('Segment: ' + segmentName);
            
            // type
            $('#sm_type input:checked').prop('checked', false);
            $('#sm_type input[value="'+segmentData.type+'"]').prop('checked', true);
            $('#sm_status input[value="'+segmentData.status+'"]').prop('checked', true);
            
            if(managedBySystem){
            	$('button.data-control-delete').attr('disabled','disabled');
            	$('button.data-control-delete').attr('title','Can not delete this segment, which is managed by system');
            }
            else {
            	if( json.canDeleteData){
        			$('button.data-control-delete').click(deleteSegment);
    			} else {
    				$('button.data-control-delete').attr('disabled','disabled');
    			}
            }
            
            // web form
            setSegmentDataForm(segmentData);

			handleSegmentCustomQueryFilter(false)
            
            // visual query builder
            loadSegmentBuilder(touchpointHubs, behavioralEventMap, assetGroups, jsonQueryRules, segmentQueryBuilderCallback);
            
            // load profile list table
            loadProfilesInSegment(false, 700);

			// load authorized users
            loadSystemUsersForDataAuthorization(json.canSetAuthorization, segmentData, $('#authorizedSegmentViewers'), $('#authorizedSegmentEditors'));
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}
	

	
var createNewSegmentModel = function() {
    var urlStr = baseLeoAdminUrl + '/cdp/segment/load';
    LeoAdminApiUtil.callPostAdminApi(urlStr, {}, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
        	var behavioralEventMap = json.data.behavioralEventMap;
        	var segmentData = json.data.segmentData;
        	var assetGroups = json.data.assetGroups;
        	var touchpointHubs = json.data.touchpointHubs;
        	
        	// set web form
        	setSegmentDataForm(segmentData)
			
            document.title = 'New Segment Builder';
        	$('#segment_name').text('New Segment Builder: ');
           
         	// init UI after setting data into DOM
         	//initDateFilterComponent(true);
			loadSegmentBuilder(touchpointHubs, behavioralEventMap, assetGroups , false, segmentQueryBuilderCallback);
			
            // load profile list table
            loadProfilesInSegment(false, 700);
            loadSystemUsersForDataAuthorization(json.canSetAuthorization, segmentData, $('#authorizedSegmentViewers'), $('#authorizedSegmentEditors'));
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}
	
	
var setSegmentDataForm = function(segmentData){
	segmentDataModel = segmentData;
	
	// meta data  
	$('#sm_name').val(segmentData.name).keyup(function() {
		segmentDataModel.name = $(this).val().trim();
		$('#segment_name').text('Segment: ' + segmentDataModel.name);
	});
	$('#sm_description').val(segmentData.description).keyup(function() {
		segmentDataModel.description = $(this).val().trim();
	});
	
	$('#sm_realtimeQuery').prop("checked", segmentData.realtimeQuery ).change(function() {
		segmentDataModel.realtimeQuery = $(this).prop("checked");
	});
	
	$('#sm_indexscore').val(segmentData.indexScore).change(function() {
		segmentDataModel.indexScore = parseInt($(this).val());
	});
	
	// purpose of data segmentation 
	// 1 
	$('#sm_forDeepAnalytics').prop("checked", segmentData.forDeepAnalytics ).change(function() {
		segmentDataModel.forDeepAnalytics = $(this).prop("checked");
	});
	// 2
	$('#sm_forPredictiveAnalytics').prop("checked", segmentData.forPredictiveAnalytics ).change(function() {
		segmentDataModel.forPredictiveAnalytics = $(this).prop("checked");
	});
	// 3
	$('#sm_forPersonalization').prop("checked", segmentData.forPersonalization ).change(function() {
		segmentDataModel.forPersonalization = $(this).prop("checked");
	});
	// 4
	$('#sm_forEmailMarketing').prop("checked", segmentData.forEmailMarketing ).change(function() {
		segmentDataModel.forEmailMarketing = $(this).prop("checked");
	});
	// 5
	$('#sm_forRealtimeMarketing').prop("checked", segmentData.forRealtimeMarketing ).change(function() {
		segmentDataModel.forRealtimeMarketing = $(this).prop("checked");
	});
	// 6
	$('#sm_forReTargeting').prop("checked", segmentData.forReTargeting ).change(function() {
		segmentDataModel.forReTargeting = $(this).prop("checked");
	});
	// 7
	$('#sm_forLookalikeTargeting').prop("checked", segmentData.forLookalikeTargeting ).change(function() {
		segmentDataModel.forLookalikeTargeting = $(this).prop("checked");
	});
	// 8
	$('#sm_for3rdSynchronization').prop("checked", segmentData.for3rdSynchronization ).change(function() {
		segmentDataModel.for3rdSynchronization = $(this).prop("checked");
	});
}
	
var saveSegmentMetaData = function() {
	var name = $('#sm_name').val().trim();
	if(name === ""){
		notifyErrorMessage("Please enter the name of segment, it can not be empty !");
		return;
	}
	
	if(typeof segmentDataModel === "object"){
		// set segment name
		segmentDataModel.name = name;
		
		// jquery builder final data
		var result = $('#segment-builder-holder').queryBuilder('getRules');
		if (!$.isEmptyObject(result)) {
			segmentDataModel.jsonQueryRules = JSON.stringify(result) || "";
		} else {
			segmentDataModel.jsonQueryRules = "";
		}
		
		segmentDataModel.customQueryFilter = getSegmentCustomQueryFilter()
		segmentDataModel.type = parseInt($('#sm_type input:checked').val());
		segmentDataModel.status = parseInt($('#sm_status input:checked').val());
		segmentDataModel.realtimeQuery = $('#sm_realtimeQuery').prop("checked");
		
		// segment authorization
		if(currentUserProfile.role >= 5){
			segmentDataModel.authorizedViewers = $('#authorizedSegmentViewers').val() || [];
			segmentDataModel.authorizedEditors = $('#authorizedSegmentEditors').val() || [];
		}
		
		//done set data model, POST to API
		var dataJsonStr = JSON.stringify(segmentDataModel) ;
        var urlStr = baseLeoAdminUrl + '/cdp/segment/save';
        
     	// show loader
        $("#segment_builder_div").hide();
        $("#segment_builder_loader").show();
        
        LeoAdminApiUtil.callPostAdminApi(urlStr,  { 'dataObject' : dataJsonStr }, function (json) {
            if (json.httpCode === 0 ) {
                if(json.data === ''){
                    $('#error-on-save').html('Data is not valid !').show().delay(5000).fadeOut('slow');
                } else {
                	var segmentId = json.data;
                	location.hash = "calljs-leoCdpRouter('Segment_Details','" + segmentId + "')";
                	notifySavedOK('Your segment has been created and saved')
                }
            } else {
            	if( json.errorMessage.indexOf('jsonQueryRules') >= 0 ) {
            		json.errorMessage = 'Please set filtering parameters in the segment query builder';
            	}
                $('#error-on-save').html(json.errorMessage).show().delay(5000).fadeOut('slow');
                LeoAdminApiUtil.logErrorPayload(json);
            }
        });
	}
}
