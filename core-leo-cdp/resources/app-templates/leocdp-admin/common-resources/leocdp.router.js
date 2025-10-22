/**
 * 
 * @author tantrieuf31 (Thomas)
 * @since 2020 
 * 
 * this script contains all functional admin web view routers for CDP end-users
 * 
 */
LeoCdpAdmin.navRouters = {
		"defaultRouter" : {
			"menuName" : "Event Data Report",
			"functionName" : "loadEventDataDashboard",
			"breadcrumb" : ["Unified Analytics Hub", "Event Data Report"],
			"activeMenuItem" : "Event_Data_Report"
		},
		
		
		// 0  Knowledge-base for end-users
		"Learn_Leo_CDP": {
			"menuName" : "Self-Learning Courses",
			"functionName" : "loadSelfLearningCourses",
			"breadcrumb" : ["CDP Knowledge Base", "Self-Learning Courses"],
			"activeMenuItem" : "USPA_Knowledge_Hub"
		},
		
		////////////////////////// 1) UNIFIED ANALYTICS MODULE //////////////////////////
		
		// 1.1 Main Customer Data Report
		"Customer_Data_Report": {
			"menuName" : "Customer Data Report",
			"functionName" : "loadMainDataDashboard",
			"breadcrumb" : ["Unified Analytics Hub", "Customer Data Report"],
			"activeMenuItem" : "Customer_Data_Report"
		},
		
		// 1.2 Time Series Reports
		"Event_Data_Report": {
			"menuName" : "Event Data Report",
			"functionName" : "loadEventDataDashboard",
			"breadcrumb" : ["Unified Analytics Hub", "Event Data Report"],
			"activeMenuItem" : "Event_Data_Report"
		
		},
		
		// 1.3 CX Data Analytics Reports
		"CX_Data_Report": {
			"menuName" : "CX Data Report",
			"functionName" : "loadCXDataDashboard",
			"breadcrumb" : ["Unified Analytics Hub", "CX Data Report"],
			"activeMenuItem" : "CX_Data_Report"
		},
		
		// 1.4 Custom Data Reportss
		"Custom_Dashboard": {
			"menuName" : "Custom Dashboard",
			"functionName" : "loadCustomDashboard",
			"breadcrumb" : ["Unified Analytics Hub", "Custom Dashboard"],
			"activeMenuItem" : "Custom_Dashboard"
		
		},
		
		////////////////////////// 2) JOURNEY DATA MODULE //////////////////////////
		
		// 2.1 Targeted Persona
		"Customer_Persona_List" : {
			"menuName" : "Customer Persona List",
			"functionName" : "loadCustomerPersonaList",
			"breadcrumb" : ["Journey Data Hub", "Customer Persona List"]
		},
		"Customer_Persona_Report" : {
			"menuName" : "Customer Persona Report",
			"functionName" : "loadCustomerPersonaReport",
			"breadcrumb" : ["Journey Data Hub", "Customer Persona List", "Persona Report"]
		},
		"Customer_Persona_Editor" : {
			"menuName" : "Customer Persona Details",
			"functionName" : "loadCustomerPersonaDetails",
			"breadcrumb" : ["Journey Data Hub", "Customer Persona List", "Persona Editor"]
		},
		
		// 2.2 Data Journey Map
		"Data_Journey_Map" : {
			"menuName" : "Data Journey Map",
			"functionName" : "loadJourneyMap",
			"breadcrumb" : ["Journey Data Hub", "Data Journey Map"],
			"activeMenuItem" : "Data_Journey_Map"
		},
		"Touchpoint_Hub_Report" : {
			"menuName" : "Touchpoint Hub Report",
			"functionName" : "loadTouchpointHubReport",
			"breadcrumb" : ["Journey Data Hub", "Customer Journey Map", "Touchpoint Hub Report" ],
			"activeMenuItem" : "Data_Journey_Map"
		},
		"Touchpoint_Hub_Editor" : {
			"menuName" : "Touchpoint Hub Editor",
			"functionName" : "loadTouchpointHubEditor",
			"breadcrumb" : ["Journey Data Hub", "Customer Journey Map", "Touchpoint Hub Editor" ],
			"activeMenuItem" : "Data_Journey_Map"
		},
		
		// 2.3 Event Data Funnel
		"Event_Data_Funnel" : {
			"menuName" : "Event Data Funnel",
			"functionName" : "loadDataEventFunnel",
			"breadcrumb" : ["Journey Data Hub", "Event Data Funnel"],
			"activeMenuItem" : "Event_Data_Funnel"
		},
		
		// 2.4 Journey Map List, Report and Editor
		"Journey_Map_List" : {
			"menuName" : "Journey Map List",
			"functionName" : "loadDataJourneyMapList",
			"breadcrumb" : ["Journey Data Hub", "Journey Map List"],
			"activeMenuItem" : "Data_Journey_Map"
		},
		"Journey_Map_Report" : {
			"menuName" : "Journey Map Report",
			"functionName" : "loadJourneyMapReport",
			"breadcrumb" : ["Journey Data Hub", "Journey Map List", "Journey Map Report"],
			"activeMenuItem" : "Data_Journey_Map"
		},
		"Customer_Persona" : {
			"menuName" : "Customer Persona",
			"functionName" : "loadPersonaEditor",
			"breadcrumb" : ["Journey Data Hub", "Journey Map List","Customer Persona"],
			"activeMenuItem" : "Data_Journey_Map"
		},
		
		////////////////////////// 3) CUSTOMER DATA MODULE //////////////////////////
		
		// 3.1 Account Management 
		"Business_Account_Management" : {
			"menuName" : "Business Account Management",
			"functionName" : "loadBusinessAccountList",
			"breadcrumb" : ["Customer Data Hub", "Business Account Management"],
			"activeMenuItem" : "Account_Management"
		},	
		"Business_Account_Info" : {
			"menuName" : "Business Account Information",
			"functionName" : "loadBusinessAccountInfo",
			"breadcrumb" : ["Customer Data Hub", "Business Account Management", "Business Account Information"],
			"activeMenuItem" : "Account_Management"
		},
		"Business_Account_Editor" : {
			"menuName" : "Business Account Editor",
			"functionName" : "loadBusinessAccountEditor",
			"breadcrumb" : ["Customer Data Hub", "Business Account Management", "Business Account Editor"],
			"activeMenuItem" : "Account_Management"
		},
		
		// 3.2 Profile Management
		"Profile_Management" : {
			"menuName" : "Profile Management",
			"functionName" : "loadCustomerProfileList",
			"breadcrumb" : ["Customer Data Hub", "Profile Management"],
			"activeMenuItem" : "Profile_Management"
		},	
		"Profile_List_By_Type" : {
			"menuName" : "Profile Management",
			"functionName" : "loadCustomerProfileListByType",
			"breadcrumb" : ["Customer Data Hub", "Profile Management"],
			"activeMenuItem" : "Profile_Management"
		},	
		"Customer_Profile_Info" : {
			"menuName" : "Profile Information",
			"functionName" : "loadCustomerProfileInfo",
			"breadcrumb" : ["Customer Data Hub", "Profile Management", "Profile Information"],
			"activeMenuItem" : "Profile_Management"
		},
		"Customer_Profile_Info_By_CRM_ID" : {
			"menuName" : "Profile Information",
			"functionName" : "loadCustomerProfileInfoByCrmId",
			"breadcrumb" : ["Customer Data Hub", "Profile Management", "Profile Information"],
			"activeMenuItem" : "Profile_Management"
		},
		"Customer_Profile_Info_By_Visitor_ID" : {
			"menuName" : "Profile Information",
			"functionName" : "loadCustomerProfileInfoByVisitorId",
			"breadcrumb" : ["Customer Data Hub", "Profile Management", "Profile Information"],
			"activeMenuItem" : "Profile_Management"
		},
		"Customer_Profile_Editor" : {
			"menuName" : "Profile Editor",
			"functionName" : "loadCustomerProfileEditor",
			"breadcrumb" : ["Customer Data Hub", "Profile Management", "Profile Editor"],
			"activeMenuItem" : "Profile_Management"
		},
		"Customer_Profile_Import" : {
			"menuName" : "Profile Data Import",
			"functionName" : "loadProfileImporter",
			"breadcrumb" : ["Customer Data Hub", "Profile Management", "Profile Data Import"],
			"activeMenuItem" : "Profile_Management"
		},
		"Customer_Event_Import" : {
			"menuName" : "Event Data Import",
			"functionName" : "loadEventImporter",
			"breadcrumb" : ["Customer Data Hub", "Profile Management", "Event Data Import"],
			"activeMenuItem" : "Profile_Management"
		},
		
		// 3.3 Data Segmentation
		"Segment_Management" : {
			"menuName" : "Segment Management",
			"functionName" : "loadSegmentList",
			"breadcrumb" : ["Customer Data Hub", "Segment Management"],
			"activeMenuItem" : "Segment_Management"
		},
		"Segment_Builder" : {
			"menuName" : "Segment Builder",
			"functionName" : "loadSegmentBuilder",
			"breadcrumb" : ["Customer Data Hub", "Segment Management", "Segment Builder"],
			"activeMenuItem" : "Segment_Management"
		},
		"Segment_Details" : {
			"menuName" : "Segment Details",
			"functionName" : "loadSegmentDetails",
			"breadcrumb" : ["Customer Data Hub", "Segment Management", "Segment Details"],
			"activeMenuItem" : "Segment_Management"
		},
		"Segment_Activation" : {
			"menuName" : "Segment Activation",
			"functionName" : "loadSegmentActivation",
			"breadcrumb" : ["Customer Data Hub", "Segment Management", "Segment Activation"],
			"activeMenuItem" : "Segment_Management"
		},
		
		// 3.4 Customer Profile Search
		"Customer_Profile_Search" : {
			"menuName" : "Customer Profile Search",
			"functionName" : "loadCustomerProfileSearch",
			"breadcrumb" : ["Customer Data Hub", "Profile Management"],
			"activeMenuItem" : "Profile_Management"
		},	
		
		// 3.5 Customer Profile Filter
		"Customer_Profile_Filter" : {
			"menuName" : "Customer Profile Filter",
			"functionName" : "loadCustomerProfileFilter",
			"breadcrumb" : ["Customer Data Hub", "Profile Management"],
			"activeMenuItem" : "Profile_Management"
		},	
		
		////////////////////////// 4) DATA ACTIVATION HUB //////////////////////////
		
		// 4.1 Customer Touchpoint
		"Data_Touchpoint_List" : {
			"menuName" : "Data Touchpoint List",
			"functionName" : "loadDataTouchpointList",
			"breadcrumb" : ["Data Activation Hub", "Data Touchpoint List"],
			"activeMenuItem" : "Data_Touchpoint_List"
		},
		"Data_Touchpoint_Report" : {
			"menuName" : "Touchpoint Report",
			"functionName" : "loadDataTouchpointReport",
			"breadcrumb" : ["Data Activation Hub", "Data Touchpoint List" , "Touchpoint Report" ],
			"activeMenuItem" : "Data_Touchpoint_List"
		},
		"Data_Touchpoint_Editor" : {
			"menuName" : "Touchpoint Editor",
			"functionName" : "loadDataTouchpointEditor",
			"breadcrumb" : ["Data Activation Hub", "Data Touchpoint List" , "Touchpoint Editor" ],
			"activeMenuItem" : "Data_Touchpoint_List"
		},
		
		// 4.2 Digital Asset Management
		"Digital_Asset_Management" : {
			"menuName" : "Digital Asset Management",
			"functionName" : "loadDigitalAssetCategories",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		// 4.3 AI Agent for Data Activation 

		"AI_Agent_Activation" : {
			"menuName" : "AI Agent Activation",
			"functionName" : "loadAgentList",
			"breadcrumb" : ["Data Activation Hub", "AI Agent Activation"],
			"activeMenuItem" : "AI_Agent_Activation"
		},
		
		// Asset Group List
		"Asset_Groups" : {
			"menuName" : "Asset Group ",
			"functionName" : "loadGroupsInCategory",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management", "Asset Groups"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		
		// Asset Group Details and Items
		"Asset_Group_Details" : {
			"menuName" : "Asset Group Details",
			"functionName" : "loadGroupDetails",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management", "Asset Groups", "Asset Group Details"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		
		// Asset Product Importer
		"Product_Importer" : {
			"menuName" : "Product Importer",
			"functionName" : "loadProductImporter",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management", "Asset Groups", "Product Importer"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		
		// Asset Group Editor 
		"Asset_Group_Editor" : {
			"menuName" : "Asset Group Editor",
			"functionName" : "loadGroupEditor",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management", "Asset Groups", "Asset Group Editor"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		
		// Digital Asset Item Viewer
		"Asset_Content_View" : {
			"menuName" : "Item Information",
			"functionName" : "assetContentView",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management", "Asset Groups", "Asset Group Details", "Item Information"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		
		// Template Item Editor
		"Asset_Template_Editor" : {
			"menuName" : "Asset Template Editor",
			"functionName" : "assetTemplateEditor",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management", "Asset Groups", "Asset Group Details", "Asset Template Editor"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		
		// Feedback Form Editor
		"Asset_Feedback_Form_Editor" : {
			"menuName" : "Feedback Form Editor",
			"functionName" : "assetFeedbackFormEditor",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management", "Asset Groups", "Asset Group Details", "Feedback Form Editor"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		
		// HTML Presentation Editor
		"Asset_Presentation_Editor" : {
			"menuName" : "Presentation Editor",
			"functionName" : "assetPresentationEditor",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management", "Asset Groups", "Asset Group Details", "Presentation Editor"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		
		// Content Item Editor
		"Asset_Content_Editor" : {
			"menuName" : "Asset Content Editor",
			"functionName" : "assetContentEditor",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management", "Asset Groups", "Asset Group Details", "Asset Content Editor"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		
		// Product Item Editor
		"Asset_Product_Item_Editor" : {
			"menuName" : "Asset Product Item Editor",
			"functionName" : "assetProductItemEditor",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management", "Asset Groups", "Asset Group Details", "Asset Product Item Editor"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		
		// Smart Link Editor
		"Asset_Short_Link_Editor" : {
			"menuName" : "Asset Short Link Editor",
			"functionName" : "assetShortUrlLinkEditor",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management", "Asset Groups", "Asset Group Details", "Short Link Editor"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		
		// Service Item Editor
		"Asset_Service_Item_Editor" : {
			"menuName" : "Asset Product Service Editor",
			"functionName" : "assetSubscriptionItemEditor",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management", "Asset Groups", "Asset Group Details", "Asset Product Service Editor"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		
		// Service Item Editor
		"Asset_Creative_Editor" : {
			"menuName" : "Asset Product Service Editor",
			"functionName" : "assetItemCreativeEditor",
			"breadcrumb" : ["Data Activation Hub", "Digital Asset Management", "Asset Groups", "Asset Group Details", "Asset Product Service Editor"],
			"activeMenuItem" : "Digital_Asset_Management"
		},
		
		
		
		
		// 4.4 Marketing Activation Campaigns
		
		"Automated_Campaigns" : {
			"menuName" : "Automated Campaigns",
			"functionName" : "loadCampaignList",
			"breadcrumb" : ["Data Activation Hub", "Automated Campaigns"],
			"activeMenuItem" : "Automated_Campaigns"
		},
		"Campaign_Info" : {
			"menuName" : "Campaign Information",
			"functionName" : "loadCampaignInfo",
			"breadcrumb" : ["Data Activation Hub", "Automated Campaigns", "Campaign Information"],
			"activeMenuItem" : "Automated_Campaigns"
		},
		"Campaign_Editor" : {
			"menuName" : "Campaign Editor",
			"functionName" : "loadCampaignEditor",
			"breadcrumb" : ["Data Activation Hub", "Automated Campaigns", "Campaign Editor"],
			"activeMenuItem" : "Automated_Campaigns"
		},
		
		////////////////////////// 5) SYSTEM DATA MODULE //////////////////////////
		
		// 5.1 User Login Management
		"User_Login_Management" : {
			"menuName" : "User Login Management",
			"functionName" : "loadUserLoginManagement",
			"breadcrumb" : ["System Management", "User Login Management"],
			"activeMenuItem" : "User_Login_Management"
		},
		"User_Login_Editor" : {
			"menuName" : "User Login Editor",
			"functionName" : "loadUserLoginEditor",
			"breadcrumb" : ["System Management", "User Login Management","Login Account Editor"],
			"activeMenuItem" : "User_Login_Management"
		},
		"User_Login_Report" : {
			"menuName" : "User Login Report",
			"functionName" : "loadUserLoginReport",
			"breadcrumb" : ["System Management", "User Login Management","Login Account Report"],
			"activeMenuItem" : "User_Login_Management"
		},
		"My_Login_Editor" : {
			"menuName" : "My Login Account Editor",
			"functionName" : "loadMyLoginInfo",
			"breadcrumb" : ["My Login Account"],
			"activeMenuItem" : "User_Login_Management"
		},
	
		// 5.2 System Monitor and Configs 
		"System_Data_Settings" : {
			"menuName" : "System Data Settings",
			"functionName" : "loadSystemInfoConfigs",
			"breadcrumb" : ["System Management", "System Data Settings"],
			"activeMenuItem" : "System_Data_Settings"
		}
};

//############################ Router Processing JS ############################

function leoCdpRouterParams(arr){
	return typeof arr === "object" ? arr.join('&&') : "";
}

function gotoLeoCdpRouter(){
	var paramStr = '';
	for(var i=0; i < arguments.length; i++){
		paramStr = paramStr +  "'" +  arguments[i];
		if( i+1 < arguments.length ){
			paramStr +=  "'," ;
		} else {
			paramStr +=  "'" ;
		}
	}
	var hash = 'calljs-leoCdpRouter(' + paramStr  + ")";
	location.hash = hash;
}

function leoCdpRouter(objKey, paramsStr) {
	LeoCdpAdmin.routerKey = objKey;	
	var obj = LeoCdpAdmin.navRouters[objKey];
	console.log('LeoCdpAdmin.navRouters[objKey] ', objKey , obj);
	console.log( 'paramsStr ' + paramsStr );
	
	// reset active menu item in navigation
	$('#main-navbar').find('a.active').removeClass('active');
	
	// reset search box
	$('#main_search_profile').val('');
	
	// generate breadcrumb navigation
	var titleNav = '';
	var breadcrumbList = obj.breadcrumb;
	var len = breadcrumbList.length;
	var activeMenuItemId = '';
	var maxBreadcrumbItem = 4;
	
	var name = breadcrumbList[0];
	titleNav  = titleNav + name + " - ";
	var key = name.replace(/ /g, "_");
	var jsFunc = LeoCdpAdmin.navRouters[key] ? "leoCdpRouter('"+ key + "')"  : '';
	
	var breadcrumbHtml = '<a id="tnv_'+key+'" title="'+ name +'" href="javascript:void(0)' + jsFunc + '"> ' + breadcrumbList[0] + ' </a>  ';
	for(var i=1; i< len; i++ ) {
		var name = breadcrumbList[i];
		titleNav  = titleNav + name + " - ";
		var key = name.replace(/ /g, "_");
		var jsFunc = LeoCdpAdmin.navRouters[key] ? "leoCdpRouter('"+ key + "')"  : '';
		maxBreadcrumbItem = maxBreadcrumbItem -1 ;
		
		if(i < len - 1){
			breadcrumbHtml = breadcrumbHtml + ' <i class="fa fa-long-arrow-right" aria-hidden="true"></i> <a id="tnv_'+key+'" title="'+ name +'" href="#calljs-' + jsFunc + '"> ' + breadcrumbList[i] + ' </a> ';
		} else {
			breadcrumbHtml = breadcrumbHtml + ' <i class="fa fa-long-arrow-right" aria-hidden="true"></i> <a id="tnv_'+key+'" title="'+ name +'" href="javascript:void(0)"> ' + breadcrumbList[i] + ' </a> ';
		}
		
		activeMenuItemId = LeoCdpAdmin.navRouters[key] ? (LeoCdpAdmin.navRouters[key].activeMenuItem || activeMenuItemId)  : activeMenuItemId;
		console.log('activeMenuItemId ' + activeMenuItemId)
	}
	
	// set active menu in navigation
	if(activeMenuItemId != ''){
		$('#main-navbar').find('#'+activeMenuItemId).addClass('active');
	}
	
	// get functionName of router
	var vf = LeoCdpAdmin.navFunctions[obj.functionName];
	//
	if(typeof vf === 'function') {
		// init context for view router
		LeoCdpAdmin.routerContext = {};
		if(typeof paramsStr === 'string') {
			if( paramsStr.indexOf('_refresh_') < 0 ){
				var params = _.union(paramsStr.split('&&'), [breadcrumbHtml])
				LeoCdpAdmin.routerContext.paramsStr = paramsStr;
				vf.apply(null, params );
			}
			else {
				LeoCdpAdmin.routerContext.paramsStr = false;
				vf.apply(null,[breadcrumbHtml]);
			}
		} 
		else {
			LeoCdpAdmin.routerContext.paramsStr = false;
			vf.apply(null,[breadcrumbHtml]);
		}
		document.title = titleNav;
	} else {
		console.error( " LeoCdpAdmin.navFunctions[obj.functionName] is not a function " );
		console.error( obj );
	}
	// scroll to top location
	window.scroll(0,0);
}