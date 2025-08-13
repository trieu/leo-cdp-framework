/**
 * 
 * @author tantrieuf31 (Thomas)
 * 
 * this script contains all navFunctions (router -> functionName) for LeoCdpAdmin.navRouters
 * 
 */

LeoCdpAdmin.navFunctions = {};

//###################### USPA Knowledge Hub ######################

LeoCdpAdmin.navFunctions.loadSelfLearningCourses = function(breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/knowledge/self-learning-courses.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initSelfLearningCourses();
    });
}

//###################### Analytics 360 Hub ######################

LeoCdpAdmin.navFunctions.loadMainDataDashboard = function(breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/analytics/main-dashboard.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initPrimaryDashboard();
    });
}

LeoCdpAdmin.navFunctions.loadContentDashboard = function(breadcrumbHtml) {
	LeoCdpAdmin.loadView('/view/modules/analytics/content-dashboard.html?admin=1', pageDomSelector, function () {
		$('#page_breadcrumb').html(breadcrumbHtml);
		initContentDashboard();
    });
}

LeoCdpAdmin.navFunctions.loadEventDataDashboard = function(breadcrumbHtml) {
	LeoCdpAdmin.loadView('/view/modules/analytics/event-data-dashboard.html?admin=1', pageDomSelector, function () {
		$('#page_breadcrumb').html(breadcrumbHtml);
		intEventDataDashboard();
    });
}

LeoCdpAdmin.navFunctions.loadCXDataDashboard = function(breadcrumbHtml) {
	LeoCdpAdmin.loadView('/view/modules/analytics/cx-analytics-dashboard.html?admin=1', pageDomSelector, function () {
		$('#page_breadcrumb').html(breadcrumbHtml);
		initCXAnalyticsDashboard();
    });
}


LeoCdpAdmin.navFunctions.loadCustomDashboard = function(breadcrumbHtml) {
	LeoCdpAdmin.loadView('/view/modules/analytics/custom-dashboard.html?admin=1', pageDomSelector, function () {
		$('#page_breadcrumb').html(breadcrumbHtml);
		intCustomDashboard();
    });
}


//###################### Journey Data Hub ######################

LeoCdpAdmin.navFunctions.loadPersonaEditor = function(journeyId) {
    LeoCdpAdmin.loadView('/view/modules/journey/persona-editor.html?admin=1', pageDomSelector, function () {
    	loadOkJourneyMapDesigner();
    });
}

LeoCdpAdmin.navFunctions.loadDataJourneyMapList = function(breadcrumbHtml) {
	LeoCdpAdmin.loadView('/view/modules/journey/journey-map-list.html?admin=1', pageDomSelector, function () {
		$('#page_breadcrumb').html(breadcrumbHtml);
		initJourneyMapList();
    });
}

LeoCdpAdmin.navFunctions.loadJourneyMap = function(journeyId, breadcrumbHtml) {
	LeoCdpAdmin.loadView('/view/modules/journey/customer-journey-flow.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initJourneyMap(journeyId);
    });
}

LeoCdpAdmin.navFunctions.loadDataEventFunnel = function(breadcrumbHtml) {
	LeoCdpAdmin.loadView('/view/modules/journey/customer-journey-funnel.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initBehavioralEventList();
    });
}	

//###################### Customer Data Hub ######################

// --- Customer Profile functions ---

LeoCdpAdmin.navFunctions.loadCustomerProfileList = function (breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/customer-profile-list.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initCustomerProfileList();
    });
}

LeoCdpAdmin.navFunctions.loadCustomerProfileListByType = function (profileType, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/customer-profile-list.html?admin=1', pageDomSelector, function () {
    	if(typeof profileType === 'string' && typeof breadcrumbHtml === 'string') {
    		$('#page_breadcrumb').html(breadcrumbHtml);
        	initCustomerProfileList(false, profileType);
    	}
    });
}

LeoCdpAdmin.navFunctions.loadCustomerProfileSearch = function (keywords, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/customer-profile-list.html?admin=1', pageDomSelector, function () {
    	if(typeof keywords === 'string' && typeof breadcrumbHtml === 'string') {
    		$('#page_breadcrumb').html(breadcrumbHtml);
    		initCustomerProfileList(keywords);
    	}
    });
}

LeoCdpAdmin.navFunctions.loadCustomerProfileFilter = function (labels, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/customer-profile-list.html?admin=1', pageDomSelector, function () {
    	if(typeof labels === 'string' && typeof breadcrumbHtml === 'string') {
    		$('#page_breadcrumb').html(breadcrumbHtml);
    		initCustomerProfileFilter(labels);
    	}
    });
}

LeoCdpAdmin.navFunctions.loadProfileImporter = function (breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/customer-profile-import.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initProfileImporter();
    });
}

LeoCdpAdmin.navFunctions.loadEventImporter = function (breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/customer-event-import.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initEventImporter();
    });
}

LeoCdpAdmin.navFunctions.loadCustomerProfileInfo = function (profileId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/customer-profile-info.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initProfile360Analytics(profileId, false, false);
    });
}

LeoCdpAdmin.navFunctions.loadCustomerProfileInfoByCrmId = function (crmRefId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/customer-profile-info.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initProfile360Analytics(false, crmRefId, false);
    });
}

LeoCdpAdmin.navFunctions.loadCustomerProfileInfoByVisitorId = function (visitorId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/customer-profile-info.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initProfile360Analytics(false, false, visitorId);
    });
}

LeoCdpAdmin.navFunctions.loadCustomerProfileEditor = function (profileId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/customer-profile-editor.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initProfileEditor(profileId);
    });
}

LeoCdpAdmin.navFunctions.removeCustomerProfile = function(id) {
	 $('#delete_callback').val('');
	 $('#confirmDeleteDialog').modal({ focus: true });
	 if (id) {
	     var callback = "removeCustomerProfile" + id;
	     window[callback] = function () {
	         var urlStr = baseLeoAdminUrl + '/cdp/profile/remove';
	         LeoAdminApiUtil.callPostAdminApi(urlStr, { 'id': id }, function (json) {
	             if (json.httpCode === 0 && json.errorMessage === '') {
	                 if (json.data) {
	                     location.hash = 'calljs-leoCdpRouter("Profile_Management")';
	                 }
	             }
	         });
	     }
	     $('#delete_callback').val(callback);
	     var name = $('#profile_full_name').text()
	     $('#deletedInfoTitle').text("Name: '"+ name + "' with Profile ID: " + id).show();
	     $('#deletedInfoMsg').text('Do you want to remove this profile ?');
	 }
}


// --- Customer Segment functions ---

LeoCdpAdmin.navFunctions.loadSegmentList = function (breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/segment-list.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initSegmentList();
    });
}

LeoCdpAdmin.navFunctions.loadSegmentBuilder = function (segmentId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/segment-builder.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initSegmentBuilder(segmentId);
    });
}

LeoCdpAdmin.navFunctions.loadSegmentDetails = function (segmentId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/segment-details.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initSegmentDetails(segmentId);
    });
}

LeoCdpAdmin.navFunctions.loadSegmentActivation = function (segmentId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/segment-activation.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initSegmentActivation(segmentId);
    });
}

//--- Business Account functions ---

LeoCdpAdmin.navFunctions.loadBusinessAccountList = function (breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/business-account-list.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initAccountList();
    });
}

LeoCdpAdmin.navFunctions.loadBusinessAccountEditor = function (accountId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/business-account-editor.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initBusinessAccountEditor(accountId);
    });
}

LeoCdpAdmin.navFunctions.loadBusinessAccountInfo = function (accountId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/customer/business-account-info.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initBusinessAccountInfo(accountId);
    });
}


//###################### Data Activation Hub ######################

// --- Touchpoint ---

LeoCdpAdmin.navFunctions.loadDataTouchpointList = function(breadcrumbHtml) {
	LeoCdpAdmin.loadView('/view/modules/activation/data-touchpoint-list.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initDataTouchpointList();
    });
}

// --- Asset Category functions ---

LeoCdpAdmin.navFunctions.loadDigitalAssetCategories = function (breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/asset/category-list.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initContentCategoryList();
    });
}

LeoCdpAdmin.navFunctions.loadGroupsInCategory = function(catKey, catType, catName, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/asset/group-list-in-category.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initLoadGroupsInCategory(catKey, catType, catName);
    });
}

// --- Asset Group functions ---

LeoCdpAdmin.navFunctions.loadGroupEditor = function(groupId, categoryId, breadcrumbHtml) {
	 LeoCdpAdmin.loadView('/view/modules/asset/group-editor.html?admin=1', pageDomSelector, function () {
		 $('#page_breadcrumb').html(breadcrumbHtml);
	     if (groupId) {
	         console.log('edit group ' + groupId);
	         loadDataPageEditor({ 'groupId': groupId,'categoryId': categoryId })
	     } else {
	         console.log('new group');
	         loadDataPageEditor({ 'groupId': "", 'categoryId': categoryId })
	     }
	 });
}

LeoCdpAdmin.navFunctions.loadGroupDetails = function(groupId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/asset/group-details.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	 initGroupDetails(groupId);
    });
}

LeoCdpAdmin.navFunctions.deleteGroup = function(assetGroupModel) {
	 $('#delete_callback').val('');
	 $('#confirmDeleteDialog').modal({ focus: true });
	 if (assetGroupModel) {
	     var callback = "deleteGroup" + assetGroupModel.id;
	     window[callback] = function () {
	         var urlStr = baseLeoAdminUrl + '/cdp/asset-group/delete';
	         var params = { 'groupId': assetGroupModel.id };
	         LeoAdminApiUtil.callPostAdminApi(urlStr, params, function (json) {
	             if (json.httpCode === 0 && json.errorMessage === '') {
	                 if (json.data) {
	                     location.hash = 'calljs-leoCdpRouter("Digital_Asset_Management")';
	                 }
	             }
	         });
	     }
	     $('#delete_callback').val(callback);
	     $('#deletedInfoTitle').text(assetGroupModel.title).show();
	     $('#deletedInfoMsg').text('Do you want to delete this group ?');
	 }
}

// --- Asset Item functions ---

LeoCdpAdmin.navFunctions.loadProductImporter = function (assetGroupId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/asset/item-product-import.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initProductImporter(assetGroupId);
    });
}

LeoCdpAdmin.navFunctions.assetContentView = function(id, groupId, categoryId,  breadcrumbHtml) {
	 if (id) {
	     LeoCdpAdmin.loadView('/view/modules/asset/post-item-info.html?admin=1', pageDomSelector, function () {
	    	$('#page_breadcrumb').html(breadcrumbHtml);
	     	initPostInfoView({ 'itemId': id, 'groupId': groupId, 'categoryId': categoryId } );
	     });
	 }
}

LeoCdpAdmin.navFunctions.assetContentEditor = function(id, groupId, categoryId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/asset/item-post-editor.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	console.log('edit content post' + id);
	    initPostEditor({'itemId': id, 'groupId': groupId, 'categoryId': categoryId } );
    });
}

LeoCdpAdmin.navFunctions.assetTemplateEditor = function(id, groupId, categoryId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/asset/item-template-editor.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	console.log('assetTemplateEditor ' + id);
    	initTemplateEditor({'itemId': id, 'groupId': groupId, 'categoryId': categoryId });
    });
}

LeoCdpAdmin.navFunctions.assetFeedbackFormEditor = function(id, groupId, categoryId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/asset/item-feedback-editor.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	console.log('assetTemplateEditor ' + id);
	    initSurveyEditor({'itemId': id, 'groupId': groupId, 'categoryId': categoryId });
    });
}

LeoCdpAdmin.navFunctions.assetPresentationEditor = function(id, groupId, categoryId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/asset/item-presentation-editor.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	console.log('assetTemplateEditor ' + id);
	    initPresentationEditor({'itemId': id, 'groupId': groupId, 'categoryId': categoryId });
    });
}

LeoCdpAdmin.navFunctions.assetProductItemEditor = function(id, groupId, categoryId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/asset/item-product-editor.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	console.log('assetProductItemEditor ' + id);
	    initProductItemEditor({'itemId': id, 'groupId': groupId, 'categoryId': categoryId });
    });
}

LeoCdpAdmin.navFunctions.assetShortUrlLinkEditor = function(id, groupId, categoryId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/asset/item-short-link-editor.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	console.log('assetShortUrlLinkEditor ' + id);
    	initShortUrlLinkEditor({'itemId': id, 'groupId': groupId, 'categoryId': categoryId });
    });
}

LeoCdpAdmin.navFunctions.assetSubscriptionItemEditor = function(id, groupId, categoryId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/asset/item-subscription-editor.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	console.log('assetSubscriptionItemEditor ' + id);
	    initPostEditor({'itemId': id, 'groupId': groupId, 'categoryId': categoryId });
    });
}

LeoCdpAdmin.navFunctions.assetItemCreativeEditor = function(id, groupId, categoryId, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/asset/item-creative-editor.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	console.log('assetItemCreativeEditor ' + id);
	    initPostEditor({'itemId': id, 'groupId': groupId, 'categoryId': categoryId });
    });
}


LeoCdpAdmin.navFunctions.deleteItemAsset = function(itemModel) {
	 $('#delete_callback').val('');
	 $('#confirmDeleteDialog').modal({focus: true});
	 if (itemModel) {
	     var callback = "deleteItemAsset" + itemModel.id;
	     window[callback] = function () {
	         var urlStr = baseLeoAdminUrl + '/cdp/asset-item/delete';
	         var params = {
	             'itemId': itemModel.id,
	             'groupId': itemModel.groupIds[0] ? itemModel.groupIds[0] : ''
	         };
	         LeoAdminApiUtil.callPostAdminApi(urlStr, params, function (json) {
	            location.hash = 'calljs-leoCdpRouter("Digital_Asset_Management")';
	         });
	     }
	     $('#delete_callback').val(callback);
	     $('#deletedInfoTitle').text(itemModel.title).show();
	     $('#deletedInfoMsg').text('Do you want to delete this item ?');
	 }
}


//###################### Customer Activation ######################

//---  Activation Service functions ---

LeoCdpAdmin.navFunctions.loadDataServiceList = function (breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/activation/data-service-list.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initDataServiceList()
    });
}


LeoCdpAdmin.navFunctions.loadCampaignList = function (breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/campaign/campaign-list.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initCampaignManagement();
    });
}

LeoCdpAdmin.navFunctions.loadCampaignInfo = function (id, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/campaign/campaign-info.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initCampaignInfo(id);
    });
}

LeoCdpAdmin.navFunctions.loadCampaignEditor = function (id, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/campaign/campaign-editor.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initCampaignEditor(id);
    });
}

LeoCdpAdmin.navFunctions.loadActivationRules = function(breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/activation/activation-rules.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initActivationRules();
    });
}

LeoCdpAdmin.navFunctions.loadCouponReport = function(id, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/activation/coupon-report.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initCouponReport(id);
    });
}

LeoCdpAdmin.navFunctions.loadEmailCampaigns = function(breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/campaign/email-campaign-list.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initEmailCampaigns();
    });
}

LeoCdpAdmin.navFunctions.loadEmailCampaignReport = function(id, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/campaign/email-campaign-report.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initEmailCampaignReport(id);
    });
}

LeoCdpAdmin.navFunctions.loadEmailCampaignEditor = function(id, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/activation/email-campaign-editor.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initEmailCampaignEditor(id);
    });
}


//###################### System Management navigation ######################

LeoCdpAdmin.navFunctions.loadUserLoginManagement = function(breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/system/login-account-list.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initUserLoginManagement();
    });
}

LeoCdpAdmin.navFunctions.loadUserLoginEditor = function(id, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/system/login-account-editor.html?admin=1', pageDomSelector, function () {
        // load data from API
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initUserLoginEditor(id);
    });
}

LeoCdpAdmin.navFunctions.loadMyLoginInfo = function(breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/system/login-account-info.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initMyLoginInfo();
    });
}

LeoCdpAdmin.navFunctions.loadUserLoginReport = function(userLogin, breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/system/login-account-report.html?admin=1', pageDomSelector, function () {
        // load data from API
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initUserLoginReport(userLogin);
    });
}

LeoCdpAdmin.navFunctions.loadSystemInfoConfigs = function(breadcrumbHtml) {
    LeoCdpAdmin.loadView('/view/modules/system/system-info-configs.html?admin=1', pageDomSelector, function () {
    	$('#page_breadcrumb').html(breadcrumbHtml);
    	initSystemConfigManagement();
    });
}