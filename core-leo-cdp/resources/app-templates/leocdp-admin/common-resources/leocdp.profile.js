
function profileAddNewTextHashSet(fieldName){
	var field = '<div class="col-md-11" ><input class="form-control" type="text" placeholder="Value" data-fieldholder="hashset" data-fieldtype="string" autocomplete="off"></div>';
	var removeIcon = '<div class="col-md-1" ><h4 onclick="$(this).parent().parent().parent().remove()" ><i class="fa fa-times-circle delete-item-btn" aria-hidden="true"></i></h4> </div>';
	var html = '<li class="list-group-item" ><div class="row" >' + field + removeIcon + '</div></li>';
	var node = $(html);
	$('p[data-field="' + fieldName + '"] > ul').append(node);
	node.effect("highlight", {}, 3600);
	node.find("input:first").focus();
}

function profileAddNewCustomField(fieldName){
	var node = $(loadCustomFieldEditor(fieldName,"",""));
	$('p[data-field="' + fieldName + '"] > ul').append(node);
	node.effect("highlight", {}, 3600);
	node.find("input:first").focus();
}

function handleProfileListFiltersBy(domNode, selector){
	$('#profile_filter_inputs > .form-control').val("").hide();
	$(selector).show();
	$('#profile_filter_input_label').text($(domNode).text()).show()
	$('#main_search_profile').val('');
	showDeleteNotActiveProfile();
}

var addNewHashMapItem = function(field, iconMap) {
	var fieldIdPrefix =  field + '_' + new Date().getTime();
	var hashmapNode = $('#'+field);
	
	// clone new iconMap to avoid duplicated options
	var optionIconMap = Object.assign({}, iconMap) ;
	hashmapNode.find('option:selected').each(function(){ 
		var selectedKey = $(this).val(); 
		delete optionIconMap[selectedKey];
	});

	var node = $(loadHashMapSelector(fieldIdPrefix, field, optionIconMap, '' , ''));
	hashmapNode.find('ul.list-group').append(node);
	node.effect("highlight", {}, 4000);
}

function removeHashMapItem(node){
	$(node).parent().parent().remove();
}

function loadHashMapSelector(fieldIdPrefix, field, optionHashMap, selectedOptionValue, value) {
	var fieldKey = '<div class="col-md-3" > <select class="form-control" id="key_'+fieldIdPrefix+'" name="'+field+'" >';
	_.forOwn(optionHashMap,function(value, key) {
		var optCode = '<option name="'+field+'" value="' + key + '"> ' + key.toUpperCase() + ' </option>';
		if(key === selectedOptionValue){
			optCode = '<option name="'+field+'" selected value="' + key + '"> ' + key.toUpperCase() + ' </option>';
		} 
		fieldKey = fieldKey + optCode;
	});
	fieldKey = fieldKey + ' </select> </div>';
	if(selectedOptionValue === ''){
		$('#key_' + fieldIdPrefix).find('option:first').attr('selected','selected')
	}
	
	var valueStr = value ? value : ''
	var fieldValue = '<div class="col-md-8" ><input id="value_'+fieldIdPrefix+'" value="'+valueStr+'" class="form-control" type="text" placeholder="Value" data-fieldholder="hashmap" data-fieldtype="string" autocomplete="off"></div>';
	var removeIcon = '<div title="Delete" class="col-md-1" onclick="removeHashMapItem(this)" > <h4><i class="fa fa-times-circle delete-item-btn" aria-hidden="true"></i></h4> </div>';
	var html = '<li id="item_' + fieldIdPrefix + '" class="list-group-item" ><div class="row" > ' + fieldKey + fieldValue + removeIcon + ' </div></li>';

	return html;
}

function loadCustomFieldEditor(fieldIdPrefix, field, value) {
	var fieldKey = '<div class="col-md-3" > <input type="text" class="form-control custom_field_name" placeholder="Field Name" value="'+field+'"> </div>';
	var valueStr = value ? value : ''
	var fieldValue = '<div class="col-md-8" ><input class="form-control custom_field_value" type="text" value="'+valueStr+'" placeholder="Field Value" data-fieldholder="hashmap" data-fieldtype="string" autocomplete="off"></div>';
	var removeIcon = '<div title="Delete" class="col-md-1" onclick="removeHashMapItem(this)" > <h4><i class="fa fa-times-circle delete-item-btn" aria-hidden="true"></i></h4> </div>';
	var html = '<li class="list-group-item" ><div class="row" > ' + fieldKey + fieldValue + removeIcon + ' </div></li>';
	return html;
}

const loadProfileDataIntoDOM = function(editable, profileId, crmRefId, visitorId, dataProcessor, callback, onErrorHandler) {
	var urlStr = '/cdp/profile/get';
    var params = {};
    
    if(typeof profileId === "string") {
    	params['profileId'] =  profileId;
		params['idType'] =  'profileId';
    }
    else if(typeof crmRefId === "string") {
    	params['crmRefId'] =  crmRefId;
		params['idType'] =  'crmRefId';
    }
    else if(typeof visitorId === "string") {
    	params['visitorId'] =  visitorId;
		params['idType'] =  'visitorId';
    }

    // exit due to nothing to process
    if(urlStr === false) {
    	console.error("loadProfileDataIntoDOM is failed, profileId or crmRefId must be a valid string! ");
    	console.log("urlStr " + urlStr)
    	console.log("params " + params)
    	return;
    }
    
    // the final URL to request data
	var url = baseLeoAdminUrl + urlStr;
	
	LeoAdminApiUtil.callPostAdminApi(url, params, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
        	var canInsertData = json.canInsertData;
	    	var canEditData = json.canEditData;
    		var canDeleteData = json.canDeleteData;
    		
    		if( ! canEditData ){
				$('button.data-control-edit').attr('disabled','disabled');
			}
    		if( ! canDeleteData ){
				$('button.data-control-delete').attr('disabled','disabled');
			}
    		if( ! canInsertData ){
				$('button.data-control-insert').attr('disabled','disabled');
			}
    		
        	LeoCdpAdmin.routerContext.dataObject = typeof dataProcessor === 'function' ? dataProcessor(json.data) : json.data;
        	
        	$('#data_model_holder').find('*[data-field]').each(function(){
        		var inputType = $(this).attr("type"); 
        		var fieldholder = $(this).data('fieldholder');
        		var fieldtype = $(this).data('fieldtype'); 
        		var field = $(this).data('field');
        		var toks = field.split('.');
        		
        		// begin data field
        		if(toks.length === 1) {
        			var value = LeoCdpAdmin.routerContext.dataObject[toks[0]];
        			if(fieldholder === 'html') {
        				if(fieldtype === 'int' || fieldtype === 'float' ) {
        					if( value === undefined || value == null ){
                				value = "-";
                			} else {
                				value = new Number(value).toLocaleString();
                				if(value == NaN || value == 'NaN'){
                					value = "-";
                				}
                			}
            			}
        				else if(fieldtype === 'date') {
        					value = value ? moment.utc(value).local().format('YYYY-MM-DD') : '-';
        				}
        				else if(fieldtype === 'date_time') {
        					value = value ? toLocalDateTime(value) : '-';
        				}
        				else if(fieldtype === 'phone') {
        					value = value ? value.replace(/\D/g,'') : '-';
        				} 
        				$(this).html(value);
        			}
        			else if(fieldholder === 'percentage'){
        				$(this).html(value).parent().attr('aria-valuenow',value).css('width',value+'%'); 
        				var p = $(this).parent();
        				p.attr("title", value + "% " + p.attr("title"))
        			}
        			else if(fieldholder === 'url'){
        				$(this).html( checkToAddUrlValueToDOM(value) );
        			}
        			else if(fieldholder === 'locationcode') {
        				var aNode = $('<a/>').attr('href','javascript:').html(textTruncate(value,80)).click(function(){
        					var lc = LeoCdpAdmin.routerContext.dataObject.locationCode;
        					if(lc.indexOf("http") === 0){
        						eModal.iframe(lc, "Location")
        					}
        					else if(lc && lc.trim() != ''){
        						var url = 'https://plus.codes/'+ lc;
            					eModal.iframe(url, value)
        					}
        				})
        				$(this).html(aNode);
        			}
        			else if(fieldholder === 'selectionvalue') {
						if(inputType === 'checkbox' && value == true) {
							$(this).attr('checked','checked');
						}
        				else if(inputType === 'radio' && value == $(this).val()) {
        					$(this).attr('checked','checked');
        				} 
        			}
        			else if(fieldholder === 'html_hashmap') {
        				var ulHtml = ' <ul class="list-group" > ';
        				_.forOwn(value,function(val, key) {
        					key = key.trim();
        					if(editable){
        						// in editor, add form node
        						var li = val;
        						var fieldIdPrefix =  field + '_' + key;
        						var iconMap;
        						if(field === 'socialMediaProfiles') {
        							iconMap = SocialMediaIconMap;
        						} 
        						else if(field === 'personalContacts') {
        							iconMap = PersonalContactIconsMap;
        						} 
        						else {
        							iconMap = ContactIconsMap;
        						}
        						li = loadHashMapSelector(fieldIdPrefix, field, iconMap , key , val );
        						ulHtml = ulHtml + li;
        					} else {
        						// in view, add text node
        						var icon = false;
        						if(field === 'socialMediaProfiles') {
        							icon = SocialMediaIconMap[key] ? SocialMediaIconMap[key] : false;
        						} 
        						else if(field === 'personalContacts') {
        							icon = PersonalContactIconsMap[key] ? PersonalContactIconsMap[key] : false;
        						} 
        						else {
        							icon = ContactIconsMap[key] ? ContactIconsMap[key] : false;
        						}
        						if(icon === false){
        							icon = ' <i class="fa fa-info-circle" aria-hidden="true"></i> ';
        						}
        						ulHtml = ulHtml + '<li class="list-group-item" > ' + icon + checkToAddUrlValueToDOM(val) + '</li>';
        					}
              			});
        				ulHtml += ' </ul> ';
        				$(this).html(ulHtml);
        			}
        			else if(fieldholder === 'html_hashset') {
        				var ulHtml = '<ul class="list-group" >';
        				_.forOwn(value,function(value, key) {
        					if(editable){
        						value = '<div class="col-md-11" ><input value="'+value+'" class="form-control" type="text" placeholder="Value" data-fieldholder="hashset" data-fieldtype="string" autocomplete="off"></div>';
        						value += '<div class="col-md-1" ><h4 onclick="$(this).parent().parent().parent().remove()" ><i class="fa fa-times-circle delete-item-btn" aria-hidden="true"></i></h4></div>';
        					} else {
        						if(fieldtype === 'system_user') {
        							value =  ' <div class="col-md-12" > <b> <i class="fa fa-info-circle" aria-hidden="true"></i></b> ' + buildSystemUserInfoLink(value) + '</div>';
        						}
        						else {
        							value =  ' <div class="col-md-12" > <b> <i class="fa fa-info-circle" aria-hidden="true"></i></b> ' + checkToAddUrlValueToDOM(value) + '</div>';
        						}
        					}
        					ulHtml = ulHtml + '<li class="list-group-item" > <div class="row" > ' + value + '</div></li>';
              			});
        				
        				ulHtml += '</ul>';
        				$(this).html(ulHtml)
        			}
        			else if(fieldholder === 'html_list_key_value') {
        				var ulHtml = '<ul class="list-group" >';
        				var keys = Object.keys(value);
        				keys.sort();
        				keys.forEach(function(key) {
    	    				var val = value[key];
        					ulHtml = ulHtml + '<li class="list-group-item" > ' + key + '  <i class="fa fa-arrow-right" aria-hidden="true"></i>  ' + val + '</li>';
    	       			});
        				ulHtml += '</ul>';
        				$(this).html(ulHtml)
        			}
        			else if(fieldholder === 'html_custom_field') {
        				var ulHtml = ' <ul class="list-group" > ';
        				_.forOwn(value,function(val, key) {
        					key = key.trim();
        					if(editable){
        						// in editor, add form node

        						var fieldIdPrefix =  field + '_' + key;
        						
        						var li = loadCustomFieldEditor(fieldIdPrefix, key, val );
        						ulHtml = ulHtml + li;
        					} else {
        						// in view, add text node
        						
        						ulHtml += '<li class="list-group-item" >';
        						ulHtml += '<div class="row" > <div class="col-md-3"> <b> ' + key + '</b> <i  style="font-size:20px" class="fa fa-equals" aria-hidden="true"></i> </div>';
        						ulHtml += '<div class="col-md-9">' + val + '</div> </div></li>';
        					}
              			});
        				ulHtml += ' </ul> ';
        				$(this).html(ulHtml);
        			}
        			else if(fieldholder === 'inputvalue'){
        				$(this).val(value)
        			}
        		} 
        		// begin data sub-field
        		else if(toks.length === 2) {
        			var obj = LeoCdpAdmin.routerContext.dataObject[toks[0]] || {};
        			var value = obj[toks[1]] || '';
        			if(fieldholder === 'html'){
        				$(this).html(value)
        			}
        			else if(fieldtype === 'date') {
    					value = moment.utc(value).local().format('YYYY-MM-DD');
    					$(this).html(value)
    				}
        			else if(fieldtype === 'date_time') {
    					value = moment.utc(value).local().format('YYYY-MM-DD HH:mm:ss');
    					$(this).html(value)
    				}
        			else if(fieldholder === 'inputvalue'){
        				$(this).val(value)
        			}
        			else if(fieldholder === 'url'){
        				$(this).html( checkToAddUrlValueToDOM(value) );
        			}
        			else if(fieldholder === 'eventlocation') {
        				var aNode = $('<a/>').attr('href','javascript:').html(value).click(function(){
        					var lc = obj.locationCode || '';
        					if(lc && lc.trim() != ''){
        						var url = 'https://plus.codes/'+ lc;
            					eModal.iframe(url, value)
        					}
        				})
        				$(this).html(aNode);
        			}
        		}
        		// begin data sub-sub-field
        		else if(toks.length === 3) {
        			var obj = LeoCdpAdmin.routerContext.dataObject[toks[0]] || {};
        			var subObj = obj[toks[1]] || {};
        			var value = subObj[toks[2]] || '';
        			
        			if(fieldholder === 'html'){
        				$(this).html(value)
        			}
        			else if(fieldholder === 'inputvalue'){
        				$(this).val(value)
        			}
        			else if(fieldholder === 'url'){
        				$(this).html( checkToAddUrlValueToDOM(value) );
        			}
        			else if(fieldholder === 'eventlocation') {
        				var aNode = $('<a/>').attr('href','javascript:').html(value).click(function(){
        					var lc = obj.locationCode || '';
        					if(lc && lc.trim() != ''){
        						var url = 'https://plus.codes/'+ lc;
            					eModal.iframe(url, value)
        					}
        				})
        				$(this).html(aNode);
        			}
        			
        			if(fieldtype === 'date') {
    					value = moment.utc(value).local().format('YYYY-MM-DD');
    					$(this).html(value)
    				}
        			else if(fieldtype === 'date_time') {
    					value = moment.utc(value).local().format('YYYY-MM-DD HH:mm:ss');
    					$(this).html(value)
    				}
        		}
        	}).promise().done( function() {
    			if(typeof callback === 'function') callback();
    	    });
        	
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    }, onErrorHandler);
}

var updateProfileDataToModel = function(urlStr, params, callback) {
	$('#data_model_holder').find('*[data-field]').each(function(){
     	var field = $(this).data('field'); 
     	var fieldholder = $(this).data('fieldholder'); 
     	var fieldtype = $(this).data('fieldtype'); 
		var type = $(this).attr('type'); 
     	var value = '';
        
        if(type === "checkbox"){
			 value = $(this).prop('checked');
		}
		else if(fieldholder === 'html'){
             value = $(this).html().trim(); 
        } 
        else {
             value = $(this).val();
        }
        
        // check type
        if(fieldtype === 'int') {
        	value = parseInt(value.replace(/,/g,''))
        }
        else if(fieldtype === 'float') {
        	value = parseFloat(value.replace(/,/g,''))
        }
        else if(fieldtype === 'date') {
        	value = new Date(value);
        } 
        else if(fieldtype === 'tel') {
        	value = value.replace(/\D/g,'');
        } 
        
        var toks = field.split('.');
 		if(toks.length === 1){
 			LeoCdpAdmin.routerContext.dataObject[toks[0]] = value;
 		}
 		else if(toks.length === 2){
 			LeoCdpAdmin.routerContext.dataObject[toks[0]][toks[1]] = value;
 		}
	}).promise().done(function() {   
        LeoAdminApiUtil.callPostAdminApi(urlStr, params, function (json) {
             if (json.httpCode === 0 && json.errorMessage === '') {
     			if(typeof callback === 'function') callback(json);
             } else {
                 LeoAdminApiUtil.logErrorPayload(json);
             }
        });
    });
}


var loadProfileEventDailyReportUnit = function(){
	var journeyMapId = currentJourneyMapId || '';
	var formatDateTime = 'YYYY-MM-DD HH:mm:ss';
	var params = getDateFilterValues();
	params['profileId'] = viewProfileId;
	params['journeyMapId'] = journeyMapId;
	
	var beginReportDate = params.beginFilterDate;
	$('#eventDataFromDate').text(new moment(beginReportDate).format(formatDateTime))
	
	var endReportDate = params.endFilterDate;
	$('#eventDataToDate').text(new moment(endReportDate).format(formatDateTime));
	
	var loader = $('#profile_event_report_loader').show();
	var urlStr = baseLeoAdminUrl + '/cdp/analytics360/event-report/profile';
	LeoAdminApiUtil.getSecuredData(urlStr, params , function (json) {
		loader.hide();
        if (json.httpCode === 0 && json.errorMessage === '') {
    		// render chart data
        	var chartId = 'timeseriesProfileEventChart';
        	if( window.profileTimeseriesChart !== false ){
        		renderTimeseriesChart(chartId, json.data , true, profileTimeseriesChart, true);
    		}
    		else {
    			window.profileTimeseriesChart = renderTimeseriesChart(chartId, json.data , false, false, true);
    		}	
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}

// JS for customer-profile-info 

function handleProfileTabBehavioralInfo() {
	$('#event_flow_tabs a:first').click();
	$('#event_flow_vertital_tabs div:first').addClass('active')
}

function handleProfileTabExtInfo(){
	$('#profile_ext_info_tabs a:first').click();
	$('#profile_ext_info_vertital_tabs div:first').addClass('active')
}

const loadRecommendedContentsOfProfile = function(queryProfileId, sortableList, callback) {
	var maxResults = defaultRecommendedItemForProfile;
	var recommendedContentHolder = $('#recommended_content_list').empty().html('<div class="loader"></div>');
	
	var onLoadedDone = function(json) { 
		// ----- CONTENT -----
		
		var contentGraphData = json.data;
		var len = contentGraphData.length;
		if(len > 0) {
			recommendedContentHolder.empty();
		}
		else {
			recommendedContentHolder.empty().html('<div class="list-group-item" > No data available </div>');
		}
		_.forEach(contentGraphData, function(e) {
			 var tpl = _.template( $('#recommended-content-tpl').html() ) ;
			 e.indexScore = recommendedContentHolder.find('> div').length + 1;
			 e.updatedAt = moment.utc(e.updatedAt).local().format('YYYY-MM-DD HH:mm:ss');
			 
			 var fullImgUrl = e.content.headlineImageUrl;
             fullImgUrl = (fullImgUrl == null || fullImgUrl === "") ? 'https://cdn-icons-png.flaticon.com/128/9872/9872434.png' : fullImgUrl;
			 e.content.headlineImageUrl = fullImgUrl;
			 
			 var landingPageUrl = (e.content.fullUrl == null || e.content.fullUrl === "") ? e.content.shortLinkUrl : e.content.fullUrl;
			 e.content.landingPageUrl = landingPageUrl;
			 
			 var html = tpl(e);
			 recommendedContentHolder.append(html);
		});
		
		if(sortableList === true){
			var onContentSorted = function(evt) {
				var newIndex = evt.newIndex;  // element's new index within new parent
				var itemEl = evt.item;
				var swapItemEl = evt.swapItem;
				var key = $(itemEl).data('key');
				var swapKey = $(swapItemEl).data('key');
				
				var oldRank = parseInt($('#rank_content_' + key).text());
				var newRank = newIndex + 1;
				$('#rank_content_' + key).text(newRank);
				$('#rank_content_' + swapKey).text(oldRank);
				
				var rdModel = 1;
		    	var rankingInfoList = {};
		    	// collect data from DOM
		    	$('#recommended_content_list span.rank_info').each(function(e){ 
		    		var key = $(this).data('key'); 
		    		var rank = parseInt($(this).text()); 
		    		rankingInfoList[key] = rank;
		    		console.log(rankingInfoList);
		    	}).promise().done( function(){ 
		    		var params = {'rankingInfoList' : JSON.stringify(rankingInfoList), 'graphName' : 'cdp_content_graph', 'profileId' : queryProfileId, recommendationModel: rdModel };
	    	    	LeoAdminApiUtil.callPostAdminApi('/cdp/profile/update-item-ranking', params, function (json) {
	    	            if (json.data) {
	    	            	iziToast.success({ title: 'Update Content Content Ranking', timeout: 3000, message: 'Data is saved successfully!' });
	    	            } else {
	    	                LeoAdminApiUtil.logErrorPayload(json);
	    	            }
	    	       	});
		    	});
			}
			new Sortable(recommendedContentHolder[0], { swap: true,  swapClass: 'sortable_highlighted_item', animation: 150, onEnd: onContentSorted });
		}
		// end processing items
		
		if(typeof callback === "function"){
			callback(len);
		}
	};
	
	LeoAdminApiUtil.getSecuredData('/cdp/profile/recommended-contents',{profileId: queryProfileId, startIndex:0, numberResult: maxResults}, onLoadedDone);
}

var loadRecommendedProductsOfProfile = function(queryProfileId, sortableList, callback) {
	var maxResults = defaultRecommendedItemForProfile;
	var recommendedProductHolder = $('#recommended_product_list').empty().html('<div class="loader"></div>');
	
	var onLoadedDone = function(json) { 
		// ----- RECOMMENDED PRODUCTS -----
		
		var productGraphData = json.data;
		
		var len = productGraphData.length;
		if(len > 0) {
			recommendedProductHolder.empty();
		}
		else {
			recommendedProductHolder.empty().html('<div class="list-group-item" > No data available </div>');
		}
		
		// create HTML from data model
		var tpl = _.template($('#recommended-product-tpl').html());
		_.forEach(productGraphData, function(e) {
			var p = e.product;
			if(typeof p === 'object'){
    			e.indexScore = recommendedProductHolder.find('> div').length + 1;
    			e.updatedAt = moment.utc(e.updatedAt).local().format('YYYY-MM-DD HH:mm:ss');
    			p.offerPrice = p.salePrice * (1 - e.discount);
    			p.discount =  e.discount * 100;
    			p.keywordsText = textTruncate(p.keywords.join(", "),6555);
    			
    			var himg = p.headlineImageUrl;
    			p.headlineImageUrl = (himg == null || himg === "") ? NO_IMAGE_URL : himg;
    			
    			// console.log(p)
    			var html = tpl(e);
    			recommendedProductHolder.append(html);
			}
		});
		
		// make the UI list can be sortable by drag & drop
		if(sortableList === true){
			var onProductSorted = function(evt) {
				var newIndex = evt.newIndex;  // element's new index within new parent
				var itemEl = evt.item;
				var swapItemEl = evt.swapItem;
				var key = $(itemEl).data('key');
				var swapKey = $(swapItemEl).data('key');
				
				var oldRank = parseInt($('#rank_product_' + key).text());
				var newRank = newIndex + 1;
				$('#rank_product_' + key).text(newRank);
				$('#rank_product_' + swapKey).text(oldRank);
				
				var rankingModel = 1;
		    	var rankingInfoList = {};
		    	// collect data from DOM
		    	recommendedProductHolder.find('span.rank_info').each(function(e){ 
		    		var key = $(this).data('key'); 
		    		var rank = parseInt($(this).text());  
		    		rankingInfoList[key] = rank;
		    		console.log(rankingInfoList);
		    	}).promise().done( function(){ 
		    		var params = {'rankingInfoList' : JSON.stringify(rankingInfoList), 'graphName' : 'cdp_product_graph', 'profileId' : queryProfileId, recommendationModel: rankingModel};
	    	    	LeoAdminApiUtil.callPostAdminApi('/cdp/profile/update-item-ranking', params, function (json) {
	    	            if (json.data) {
	    	            	iziToast.success({ title: 'Product Item Ranking', timeout: 2200, message: 'Data is saved successfully!' });
	    	            } else {
	    	                LeoAdminApiUtil.logErrorPayload(json);
	    	            }
	    	       	});
		    	});
			}
			new Sortable(recommendedProductHolder[0], {swap:true, swapClass:'sortable_highlighted_item', animation:150, onEnd: onProductSorted});
		}
		
		$('input.autoselectall').on("click", function () {
			 $(this).select();
		});
		// end processing items
		
		if(typeof callback === "function"){
			callback(len);
		}
	}
	LeoAdminApiUtil.getSecuredData('/cdp/profile/recommended-products',{profileId: queryProfileId, startIndex:0, numberResult: maxResults}, onLoadedDone);
}

var loadPurchasedItemsOfProfile = function() {
	var maxResults = 100;
	LeoAdminApiUtil.getSecuredData('/cdp/profile/get-purchased-items',{profileId: viewProfileId, startIndex:0, numberResult: maxResults}, function(json) { 
		// ----- PURCHASED PRODUCTS -----
		var purchasedProductHolder = $('#purchased_items_list');
		var purchasedProductsData = json.data;
		var len = purchasedProductsData.length;
		if(len > 0) {
			purchasedProductHolder.empty()
		}
		_.forEach(purchasedProductsData, function(e) {
			 var tpl = _.template( $('#purchased-product-tpl').html() ) ;
			 e.indexScore = purchasedProductHolder.find('> div').length + 1;
			 e.createdAt = moment.utc(e.createdAt).local().format('YYYY-MM-DD HH:mm:ss');
			 var html = tpl(e);
			 purchasedProductHolder.append(html);
		});
		
		/*
		$('#purchased_items_pagination').pagination({ dataSource: [],
		    callback: function(data, pagination) {
		        // TODO 
		    }
		});
		*/
	});
	return false;
}

var loadFinancialEventList = function() {
	if(! LeoCdpAdmin.routerContext.dataObject ){
		return;
	}
	var financeEventHolder = $('#financial_event_list');
	var financeCreditEvents = LeoCdpAdmin.routerContext.dataObject.financeCreditEvents || [];
	var len = financeCreditEvents.length;
	if(len > 0) {
		financeEventHolder.empty()
	}
	_.forEach(financeCreditEvents, function(e) {
		var tpl = _.template( $('#financial-event-tpl').html() ) ;
		 
		if(e.risk === "good") {
			e.riskIcon = "fa-thumbs-o-up";
		 	e.riskIconColor = "#45D50F";	
		} else {
			e.riskIcon = "fa-thumbs-o-down";
		 	e.riskIconColor = "#F40209";
		}
		 
		e.risk = e.risk.toUpperCase(); 
		var html = tpl(e);
		financeEventHolder.append(html);
	});
}

var emptyInfo = '<br> <div class="list-group-item" > No data available </div> <br>';

var loadEventsInProfileIndex = 0;
var loadEventsInProfileResult = 25;

var callApiToGetTrackingEventsOfProfile = function(searchValue, callback){
	var flowHolder = $('#profile_activitiy_flow');
	var flowContainer = $('#profile_activitiy_flow_container');
	if(loadEventsInProfileIndex === 0) {
		flowHolder.empty().css({'max-height':'660px','min-height':'80px'});
		flowContainer.css({'height':'680px'});
	}
	
	// check to skip call server
	if(flowHolder.find("li.all-loaded").length > 0 ){
		console.log("Loaded all data for loadEventsInProfile");
		flowHolder.show();
		return;
	}
	
	flowContainer.hide();
	var loader = $('#profile_activitiy_flow_loader').show();
	var params = { profileId: viewProfileId };
	params['startIndex'] = loadEventsInProfileIndex;
	params['numberResult'] = loadEventsInProfileResult;
	params['journeyMapId'] = currentJourneyMapId;
	params['searchValue'] = searchValue;
	
	LeoAdminApiUtil.getSecuredData('/cdp/profile/tracking-events', params, function(json) { 
		 //console.log(json.data) ;
		 loader.hide();
		 if(json.data.length === 0 ){
			if(loadEventsInProfileIndex === 0) {
				flowHolder.html(emptyInfo);
			 	$('#profileEventStatistics').html(emptyInfo);	
			}
			else {
				flowHolder.find('li:last-child').addClass("all-loaded");
				$("#profile_activitiy_flow_done").show();
				$('#btn_loadMoreEvents').attr('disabled','disabled').attr('title','All data is loaded');
				$('#btn_filterEvents').attr('disabled','disabled');
			}
		 }
	
		 // apply template
		 _.forEach(json.data,function(e) {
			try {
				if(e.srcTouchpoint != null) {
					e.srcTouchpoint.url = e.srcTouchpoint.url.replace('leosyn=','leovid=');
				 	e.srcTouchpoint.name = textTruncate(decodeURISafe(e.srcTouchpoint.name),120);
				 	e.srcTouchpoint.truncatedUrl = textTruncate(e.srcTouchpoint.url,120);
				}
				// convert to local date time
				e.createdAt = moment.utc(e.createdAt).local().format('YYYY-MM-DD HH:mm:ss'); 
				
				if( e.transactionValue > 0 && e.transactionPayment !== '' && e.transactionStatus !== '') {
					// has location code ?
					e.cssStyleDisplayTransactionData = "block";
				} else {
					e.cssStyleDisplayTransactionData = "none";
				}
				
				if( e.locationCode.length > 0 ) {
					// has location code ?
					e.cssStyleDisplayLocation = "block";
				} else {
					e.cssStyleDisplayLocation = "none";
				}
				
				if( _.size(e.eventData) > 0) {
					// has custom event data 
					e.cssStyleDisplayEventData = "block";
				} else {
					e.cssStyleDisplayEventData = "none";
				}
				
				if( _.size(e.orderedItems) > 0) {
					// has ordered items data 
					e.cssStyleDisplayOrderedItems = "block";
				} else {
					e.cssStyleDisplayOrderedItems = "none";
				}
				
				if( _.size(e.serviceItems) > 0) {
					// has ordered items data 
					e.cssStyleDisplayServiceItems = "block";
				} else {
					e.cssStyleDisplayServiceItems = "none";
				}
				 
				var journeyStage = e.journeyStage;
				var transactionId = e.transactionId || "";
				 
				var tplName;
				if(transactionId.length > 0){
	    			tplName = 'conversion-event-tpl';
	    		}
				else if(journeyStage >= 3){
	    			tplName = 'action-event-tpl';
	    		}
				else if(journeyStage === 2){
					tplName = 'key-info-view-event-tpl';
				}
				else {
					tplName = 'view-event-tpl';
				}
	    		 
	    		var eventTpl = _.template( $('#'+tplName).html() );
	    		var html = eventTpl(e)
				flowHolder.append(html);
			} catch(ex){
				console.error(ex, e);
			}
		});
		flowContainer.show();
		
		// done, apply callback
		if(typeof callback ==="function") {
			callback()
		}
		else {
			var hfh = flowHolder.height()+20;
			flowContainer.height(hfh).scrollTop(0);
			console.log("==> hfh = " + hfh)
		}
	});
}

var loadEventsInProfile = function(refresh, resetKeywordsBox, callback) {
	$('#btn_loadMoreEvents').removeAttr('disabled').attr("Load more events");
	$('#btn_filterEvents').removeAttr('disabled');
		
	if(refresh === true) {		
		loadEventsInProfileIndex = 0;	
		$("#profile_activitiy_flow_done").hide();
	}
	
	// reset keywords_filter_events if and only if the value of keywords has changed
	var eventFilterNode = $('#keywords_filter_events');
	if(resetKeywordsBox === true) {
		eventFilterNode.val('');	
	}
	// get data from server
	var searchValue = eventFilterNode.val().trim();
	callApiToGetTrackingEventsOfProfile(searchValue, callback);
}

function initProfileEventInStream() {
	loadEventsInProfile(true,true);
	// tracking event filter box
	$('#keywords_filter_events').keypress(function(event){
	  	var keycode = (event.keyCode ? event.keyCode : event.which);
	  	if(keycode == '13'){
			var keywords = $(this).val().trim();
			if(keywords.length > 0) {
				loadEventsInProfileIndex = 0;	
				loadEventsInProfile(false, false)
			}
			else {
				loadEventsInProfile(true, false)
			}
	  	}
	}).change(function(){
		$('#profile_activitiy_flow').empty();
		$('#profile_activitiy_flow_container').height(20).scrollTop(0);
		$('#btn_loadMoreEvents').attr('disabled','disabled');
	});
}

function loadMoreEventsInProfile(){
	loadEventsInProfileIndex = $('#profile_activitiy_flow > li').length; 
	if(loadEventsInProfileIndex === 0){
		return;
	}
	
	loadEventsInProfile(false, false, function(){
		$(document).scrollTop($(document).height()-100);
		
		var flowContainer = $("#profile_activitiy_flow_container");
		var h = flowContainer.prop("scrollHeight")+200;
		flowContainer.scrollTop(h);
	});
}

var loadFeedbacksInProfileIndex = 0;
var loadFeedbacksInProfileResult = 120;

function loadFeedbackEventsInProfile(){
	// load feedback-events
	$('#keyword_filter_feedbacks').val('')
	
	var feedbackFlowHolder = $('#profile_feedback_flow');
	if(loadEventsInProfileIndex === 0) {
		feedbackFlowHolder.empty();
		
		// tracking event filter box
		$('#keywords_filter_events').change( function(){
	   		 var keywords = $(this).val().trim().toLowerCase();
	   		 if(keywords.length < 2) {
	   			 $('#profile_activitiy_flow li').show();
	   		 }
	   	});
	}
	
	var url = '/cdp/profile/feedback-events';
	var loader = $('#profile_feedback_flow_loader').show()
	LeoAdminApiUtil.getSecuredData(url, {id: viewProfileId, startIndex:loadFeedbacksInProfileIndex, numberResult: loadFeedbacksInProfileResult}, function(json) { 
		 //console.log(json.data) ;
		 loader.hide();
		 var dataList = json.data;
		 if(dataList.length === 0){
			 feedbackFlowHolder.html('<li class="list-group-item view" > <div class="list-group-item" > No data available </div> </li>');
		 }
		 else {
			 feedbackFlowHolder.css('height','650px');
			 _.forEach(dataList,function(e,i){
    			 e.createdAt = moment.utc(e.createdAt).local().format('YYYY-MM-DD HH:mm:ss');
    			 
    			 var previewUrl = baseLeoObserverUrl + "/webform?&tplfbt=SURVEY&tplid=" + e.refTemplateId;
    			 
    			 if(e.feedbackType === "SURVEY") {
    				 e.previewUrl = previewUrl;
    			 } else {
    				 e.previewUrl = e.touchpointUrl;
    			 }
    			 
    			 var eventName = e.eventName;
        		 
        		 // render model to get html
        		 if(eventName === "submit-feedback-form"){
        			 e.eventIcon = "fa-commenting-o";
        		 }
        		 else if(eventName === "submit-rating-form"){
        			 e.eventIcon = "fa-star";
        		 }
        		 else {
        			 e.eventIcon = "fa-smile-o";
        		 }
        		 var templateSelector = e.feedbackType === "RATING" ? "#feedback-rating-event-tpl" : "#feedback-survey-event-tpl";
        		 var htmlTpl = $(templateSelector).html()
        		 var eventTpl = _.template(htmlTpl);
    			 feedbackFlowHolder.append(eventTpl(e));
   			}); 
		 }
	});
	// feedback event filter box
	$('#keyword_filter_feedbacks').change( function(){
   		 var keywords = $(this).val().trim().toLowerCase();
   		 if(keywords.length < 2) {
   			 $('#profile_feedback_flow li').show();
   		 }
   	});
}

function filterFeedbackEvent(){
	var keywords = $('#keyword_filter_feedbacks').val().trim().toLowerCase();
	var allNodes = $('#profile_feedback_flow li');
	if(keywords.length >= 2) {
		allNodes.each(function(){ 
   			 var title = $(this).find('h5.media-heading').text().toLowerCase();  
   			 if(title.indexOf(keywords)<0){
   				 $(this).hide();
   			 } else {
   				 $(this).show();
   			 }
   		 });
	} 
}

function removeCurrentProfile(){
	var profile = LeoCdpAdmin.routerContext.dataObject;
	if(profile){
		LeoCdpAdmin.navFunctions.removeCustomerProfile(profile.id);
	}
	else {
		notifyErrorMessage("LeoCdpAdmin.routerContext.dataObject is NULL !");
	}
}

function refreshCurrentProfile(){
	var profile = LeoCdpAdmin.routerContext.dataObject;
	if(profile){
		var t = new Date().getTime();
		location.hash = "#calljs-leoCdpRouter('Customer_Profile_Info','" + profile.id + "','_refresh_"+t+"')"
	}
	else {
		notifyErrorMessage("LeoCdpAdmin.routerContext.dataObject is NULL !");
	}
}

var deduplicateCurrentProfileDialog = function() {
	$('#deduplicateCurrentProfileDialog').modal({ backdrop: 'static', keyboard: false, focus: true });
}

function deduplicateCurrentProfile() {
	var profile = LeoCdpAdmin.routerContext.dataObject;
	if(profile){
		$('#deduplicateCurrentProfileDialog').modal('hide');
		iziToast.info({
    	    title: 'Profile Data Duplication',
    	    message: 'The system is processing for profile deduplication. It takes several minutes to do this job'
    	});
		LeoAdminApiUtil.callPostAdminApi('/cdp/profile/deduplicate', {'profileId' : profile.id}, function (json) {
	        if (json.httpCode === 0 && json.errorMessage === '') {
				if(typeof json.data === "object"){
					iziToast.info({
			    	    title: 'Merge Duplicate Profile',
			    	    message: 'Total processed profile: ' + json.data.count
			    	});
				}
				var t = new Date().getTime();
				location.hash = "#calljs-leoCdpRouter('Profile_Management','_refresh_"+t+"')"
	        } else {
	            LeoAdminApiUtil.logErrorPayload(json);
	        }
	   });
	}
}

function removeSelectedProfiles() {
  	 $('#delete_callback').val('');
	 $('#confirmDeleteDialog').modal({ focus: true });
	 var size = Object.keys( window.selectedProfileIdMap || {} ).length;
	 if (size > 0 && $("#filterProfileListByStatus").val() === '') {
		 // set text
		 $('#deletedInfoTitle').text("Total Selected Profile = " + size).show();
	     $('#deletedInfoMsg').text('Do you want to remove selected profiles?');
	     
		 // call server
	     var callback = "removeSelectedProfilesConfirmOk";
	     window[callback] = function () {
	    	 LeoAdminApiUtil.callPostAdminApi('/cdp/profiles/batch-update', selectedProfileIdMap, function (json) {
	             if (json.httpCode === 0 && json.errorMessage === '') {
	             	iziToast.info({ title: 'Customer Profile Management', message: 'Total removed profile is '+json.data+' !' });
	             	// _.forOwn(selectedProfileIdMap, function(v,k){  delete selectedProfileIdMap[k]; })		
					 window.selectedProfileIdMap = {}			
	             	resetProfileSearch();
	             } else {
	                 LeoAdminApiUtil.logErrorPayload(json);
	             }
	        });
	     }
	     $('#delete_callback').val(callback);
	 }
}

function deleteNotActiveProfile() {
	$('#delete_callback').val('');
	$('#deletedInfoTitle').empty().hide();
	
	var text = $('#filterProfileListByStatus option:selected').text().trim();
	$('#deletedInfoMsg').text('Do you want to delete ALL '+text+' ?');
	$('#confirmDeleteDialog').modal({ focus: true });

	var callback = "deleteNotActiveProfileConfirmOk";
	window[callback] = function () {
		var status = $("#filterProfileListByStatus").val();
		var statusValue = -4;
		if(status != ""){
			statusValue = parseInt(status);
		}
		LeoAdminApiUtil.callPostAdminApi('/cdp/profiles/delete-not-active', {'status' : statusValue}, function (json) {
           if (json.httpCode === 0 && json.errorMessage === '') {
        	  var rs = json.data; 
        	  iziToast.info({ title: 'Profile Data Management', message: 'Deleted <b>' + rs + '</b> profiles' });
           	  resetProfileSearch();
           } else {
               LeoAdminApiUtil.logErrorPayload(json);
           }
       });
   }
   $('#delete_callback').val(callback);
}


// FIXME show the flow of event, not the profile
function loadProfileJourneyFlowReport(profileId, journeyMapId) {
	var containerSelector= "#journeyFlowChart";
	$(containerSelector).parent().hide();
	
	if(journeyMapId === ""){
		journeyMapId = 'id_default_journey';
	}
	
	LeoAdminApiUtil.getSecuredData('/cdp/analytics360/journey-flow-report', { "journeyMapId" : journeyMapId, "profileId": profileId }, function(json){
		if(json.errorCode > 0){
			iziToast.error({
	    	    title: 'Data Journey Map',
	    	    message: json.errorMessage
	    	});
			return;
		}
	 
   		var journeyMap = json.data;
   		var defaultMetricName = journeyMap.defaultMetricName;
   		var journeyStages = journeyMap.journeyStages;
   		var journeyLinks = journeyMap.journeyLinks;
   		var journeyNodes =  journeyMap.journeyNodes;
   		var touchpointHubMap = journeyMap.touchpointHubMap;
   		
   		$(containerSelector).parent().show()
   		renderJourneyFlowChart(containerSelector, defaultMetricName, journeyStages, journeyNodes, journeyLinks, touchpointHubMap);
   	})
}

const renderProfileBarChart = function(containerId, eventMap) {
	if(eventMap){
		var labels = [];
	    var dataList = [];
	    $.each(eventMap, function(k,v){ 
 	    	labels.push(k);
 	    	dataList.push(v);
 	    });
	    renderVerticalBarChart(containerId, labels, dataList)
	}
	else {
		$("#" + containerId).html('<div class="list-group-item" > No data available </div>')
	}
}

const loadProfileEventMatrix = function(profileId, journeyMapId){
	var queryFilter = {};
	queryFilter['profileId'] = profileId;
	queryFilter['journeyMapId'] = journeyMapId;
	    	
	var urlStr = baseLeoAdminUrl + '/cdp/analytics360/event-matrix';
    LeoAdminApiUtil.getSecuredData(urlStr, queryFilter , function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
        	var data = json.data;
        	renderMatrixChart('Event Matrix Report', 'profile_event_matrix', data.xLabels, data.yLabels, data.dataList)
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}

const refreshProfileDataJourneyMap = function(){
	loadProfileDataJourneyReport(viewProfileId, currentJourneyMapId);
}

const loadJourneyReportForProfile = function(profileData) {
	setTimeout(function(){
		renderProfileBarChart('profileReferrerChannels', profileData.referrerChannels)
	},3100) 	
	
	var defaultJourneyId = currentJourneyMapId || 'id_default_journey';
	// Event Statistics
	var journeyEventStats = {};
	$.each(profileData.eventStatistics, function(k,v){ 
	    var i = k.indexOf("-"); 
	    if(i < 0){
	    	journeyEventStats[defaultJourneyId] = typeof journeyEventStats[defaultJourneyId] === "object" ? journeyEventStats[defaultJourneyId] : {};
	    	journeyEventStats[defaultJourneyId][k] = v ; 
	    }
	    else {
	    	var journeyId = k.substring(0,i).trim(); 
		    var evn = k.substring(i+1); 
		    journeyEventStats[journeyId] = typeof journeyEventStats[journeyId] === "object"  ? journeyEventStats[journeyId] : {};		    
		    journeyEventStats[journeyId][evn] = v ;
		    
		    // all data from all journey maps
		    journeyEventStats[""] = typeof journeyEventStats[""] === "object"  ? journeyEventStats[""] : {};
		    if(typeof journeyEventStats[""][evn] === "number"){
				journeyEventStats[""][evn] += v;
			}
			else {
				journeyEventStats[""][evn] = v;
			} 
	    }
	});
	profileData.journeyEventStats = journeyEventStats;
	
	var profileId = window.viewProfileId;
	// journey map list selection
	var whenChangeJourneyMap = function(selectedJourneyId, name){
		renderProfileBarChart('profileEventStatsChart', journeyEventStats[selectedJourneyId])	
		
		// event matrix
		loadProfileEventMatrix(profileId, selectedJourneyId);
		
		loadProfileDataJourneyReport(profileId, selectedJourneyId);
		loadEventsInProfile(true,true);
		
 		setTimeout(function(){
			// event timeseries
 			loadProfileEventDailyReportUnit();
 			// loadProfileJourneyFlowReport(profileId, selectedJourneyId)
		},1000)
	}
	var whenJourneyMapsLoaded = function(selectedJourneyId, name){
		renderProfileBarChart('profileEventStatsChart', journeyEventStats[selectedJourneyId]);
		
 		setTimeout(function(){
			// event timeseries
			window.profileTimeseriesChart = false;
			$('#timeseriesProfileEventChart').empty()
 			loadProfileEventDailyReportUnit();
		
			// event matrix
			loadProfileEventMatrix(profileId, selectedJourneyId);
 			// loadProfileJourneyFlowReport(profileId, selectedJourneyId)
		},5000) 		
	}
  	loadJourneyMapList(false, whenChangeJourneyMap, true, whenJourneyMapsLoaded);
    //$('#event_journey_funnel').parent().width(containerWidth);
    
	//loadMediaTouchpoints();
}

const loadProfileDataJourneyReport = function(profileId, selectedJourneyId){	
 	profileId = typeof profileId === 'string' ? profileId : viewProfileId;
	selectedJourneyId = typeof selectedJourneyId === 'string' ? selectedJourneyId : currentJourneyMapId;

	loadTouchpointFlowForProfile(profileId, selectedJourneyId);
 	loadTouchpointHubReportForProfile(profileId, selectedJourneyId);

	setTimeout(function(){
		$('#event_journey_funnel').empty();
	 	loadEventJourneyProfile(profileId, selectedJourneyId);
	},700);
}

const loadTouchpointHubReportForProfile = function(profileId, journeyMapId){
	var containerId = 'profileObserverChart';
    $('#'+containerId).html('<div class="loader"></div>');
    var params = {"profileId": profileId, "journeyMapId": journeyMapId}
    var urlStr = baseLeoAdminUrl + '/cdp/analytics360/touchpoint-hub-report';
    LeoAdminApiUtil.getSecuredData(urlStr, params, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
        	var list = json.data;
        	if(list.length > 0){
        		var labels = [];
        	    var dataList = [];
        	    list.forEach(function(e){
        	    	labels.push(e.name);
        	    	dataList.push(e.eventCount);
        	    })
        	    renderHorizontalBarChart(containerId, labels, dataList)
        	}
        	else {
        		$("#" + containerId).html('<h4 class="alert alert-info" role="alert"> No data available </h4>')
        	}
        	
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}

const loadProfileTopTouchpoints = function(){
	renderProfileTouchpointChart(viewProfileId, -1, 0, 100);
}

const renderProfileTouchpointChart = function(profileId, touchpointType, startIndex, numberResult){
	var containerId = 'profileTouchpointChart';
    $('#'+containerId).html('<div class="loader"></div>');
    var params = {"profileId": profileId, "touchpointType": touchpointType, "startIndex":startIndex, "numberResult": numberResult}
    var urlStr = baseLeoAdminUrl + '/cdp/analytics360/touchpoint-report';
    LeoAdminApiUtil.getSecuredData(urlStr, params, function (json) {
        if (json.httpCode === 0 && json.errorMessage === '') {
        	var list = json.data;
        	var engagedTouchpointHolder = $('#engaged_touchpoint_list');
        	if(list.length > 0){
				engagedTouchpointHolder.empty()
        		var labels = [];
        	    var dataList = [];
        	    list.forEach(function(e){
					var tp = e.touchpoint;
					var tpName = textTruncate(tp.name, 60) 
        	    	labels.push(tpName);
        	    	dataList.push(e.eventCount);
        	    	
        	    	var tpl = _.template( $('#engaged-touchpoint-tpl').html() ) ;
					tp.updatedAt = moment.utc(tp.updatedAt).local().format('YYYY-MM-DD HH:mm:ss');
					tp.truncatedUrl = textTruncate(tp.url,120);
					if(tp.locationCode === ""){
						tp.cssShowLocationCode = "none";	
					}
					tp.eventCount = e.eventCount;
					engagedTouchpointHolder.append(tpl(tp));
        	    })
        	    renderHorizontalBarChart(containerId, labels, dataList)
        	}
        	else {
        		$("#" + containerId).html("<h4> No data available </h4>")
        	}
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}

const renderTaxonomyTextData = function(profileData) {
	 // in Journey Maps
	 var inJourneyMapsNode = $('#profileInJourneyMaps');
	 var inJourneyMapsSize = profileData.inJourneyMaps.length;
	 profileData.inJourneyMaps.forEach(function(e){
		inJourneyMapsSize--;
		var isNotTheLast = inJourneyMapsSize > 0;
		var title = "In journey map: " + e.name ;
		var name = '<i class="fa fa-map-o"></i> ' + e.name;
		var ahtml = $('<a class="btn btn-taxonomy btn-sm" />').attr('title',title).attr('href',"#calljs-leoCdpRouter('Data_Journey_Map','"+e.id+"')").html(name)[0].outerHTML;
		var html = '<b>' + ahtml + '</b>';	
 		if(isNotTheLast){
 			 html += " ";
 		}
 		if(e.queryHashedId === ""){
 			inJourneyMapsNode.append(html);
 		}
  	 });
	
	 // in segments
	 var inSegmentsNode = $('#profileInSegments');
	 var inSegmentSize = profileData.inSegments.length;
		 profileData.inSegments.forEach(function(e){
  		 inSegmentSize--;
  		 var isNotTheLast = inSegmentSize > 0;
  		 var title = "In segment: " + e.name;
		 var name = '<i class="fa fa-users"></i> ' + e.name;
  		 var ahtml = $('<a class="btn btn-taxonomy btn-sm" />').attr('title',title).attr('href',"#calljs-leoCdpRouter('Segment_Details','"+e.id+"')").html(name)[0].outerHTML;
  		 var html = '<b>' + ahtml + '</b>';
  		 if(isNotTheLast){
  			 html += " ";
  		 }
  		 inSegmentsNode.append(html);
  	 });
	 
	 // data labels
	 var dataLabelsNode = $('#profileDataLabels');
	 var dataLabelsSize = profileData.dataLabels.length;
		 profileData.dataLabels.forEach(function(name){
  		 dataLabelsSize--;
		 var html = '<i class="fa fa-tag"></i> ' + name;
  		 var isNotTheLast = dataLabelsSize > 0;     	 
         var ahtml = $('<a class="btn btn-taxonomy btn-sm" />').attr('title',name).attr('href',"#calljs-leoCdpRouter('Customer_Profile_Filter','"+name+"')").html(html)[0].outerHTML;
     	 if(isNotTheLast){
     		ahtml += " ";
     	 }
     	 dataLabelsNode.append(ahtml);
  	 });

    //profileData.inAccounts.push({"id":123,"name":"USPA Technology Company"})
	 // in accounts
	 var inAccountsNode = $('#profileInAccounts');
	 var inAccountSize = profileData.inAccounts.length;
		 profileData.inAccounts.forEach(function(e){
  		 inAccountSize--;		 
  		 var isNotTheLast = inAccountSize > 0;
		 var name = '<i class="fa fa-object-group"></i> ' + e.name;
  		 var ahtml = $('<a class="btn btn-taxonomy btn-sm" />').attr('href',"#calljs-leoCdpRouter('Account_Details','"+e.id+"')").html(name)[0].outerHTML;
    	 var html = '<b>' + ahtml + '</b>';
         if(isNotTheLast){
    		html += "; ";
    	 }
    	 inAccountsNode.append(html);
  	 });
}

//journey stage report
const loadEventJourneyProfile = function(profileId, journeyMapId) {
	var containerId = '#event_journey_funnel';
	var container = $(containerId).parent();
	var containerWidth = container.width()*2/3;
	
	$('#event_journey_funnel_info').html('<div class="loader"></div>');
	var urlStr = baseLeoAdminUrl + '/cdp/analytics360/event-report/journey-profile';
	
	var params = {"profileId":profileId, "journeyMapId": journeyMapId};
    LeoAdminApiUtil.getSecuredData(urlStr, params , function (json) {    	
        if (json.httpCode === 0 && json.errorMessage === '') {
        	// render journet stage report
			var total = json.data.total;
        	
        	// render funnel report
        	var sentimentScore = 0;
			var funnelIndex = json.data.funnelIndex; 
			
			if(json.data.scoreCX){
				sentimentScore = json.data.scoreCX.sentimentScore;
			}
			
	    	var funneIndexNode = $('#profileJourneyFunnel');
		   	if(funnelIndex >= 90) {
		   		 funneIndexNode.attr("class","progress-bar progress-bar-striped active progress-bar-danger");
		   	}
		   	else if(funnelIndex >= 50 && funnelIndex < 90) {
		   		 funneIndexNode.attr("class","progress-bar progress-bar-striped active progress-bar-success");
		   	}
		   	else {
		   		 funneIndexNode.attr("class","progress-bar progress-bar-striped active progress-bar-primary");
		   	}

		   	$('#profileJourneySentimentScore').text(sentimentScore);
		   	$('#profileJourneyFunnelValue').text(funnelIndex);
		   	funneIndexNode.attr('aria-valuenow', funnelIndex ).css('width',funnelIndex+'%');
		   	
		   	$('#profile_funnel_report > div').removeClass("funnel-stage-active")
		   	var limit = Math.floor(funnelIndex / 10);
		   	for(var i=1; i<= limit; i++){
				$('#profile_funnel_stage_'+i).addClass("funnel-stage-active");
			}
			
			var nodataHTML = '<div class="list-group-item"> No data available </div>';
			if(limit === 0){
				$('#profile_funnel_report_div').html(nodataHTML)
			}
        	
		   	// show report of journey map with 5A framework
			if(total == 0){
				$('#event_journey_funnel_info').html(nodataHTML).show()
				$('#journeyFlowChart').hide()
				return;
			}
			else {
				$('#event_journey_funnel_info').empty().hide()
				$('#journeyFlowChart').show()
			}
			
			var journeyStatsData = json.data.reportData;
        	var dataModel = {
		        labels: journeyLabel5A,
		        subLabels: ['Behavioral Event'],
		        colors: [ ['#9ACAEF', '#67B0E9', '#309EF3'] ],
		        values: journeyStatsData
		    };
			
			var graph = new FunnelGraph({
		        container: containerId,
		        gradientDirection: 'vertical',
		        data: dataModel,
		        displayPercent: true,
		        direction: 'vertical',
		        width: containerWidth,
		        height: 480,
		        subLabelValue: 'raw'
		    });
			graph.draw();
			
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}

// touchpoint flow
const loadTouchpointFlowForProfile = function(profileId, journeyMapId) {
	var containerId = 'profileTouchpointFlow';
	var containerNode = $('#'+containerId);
	var noDataNode = $('#'+containerId + '_nodata');

	var urlStr = baseLeoAdminUrl + '/cdp/analytics360/touchpoint-flow-report';
	var beginFilterDate = "";
	var endFilterDate = "";
	var params = {"profileId":profileId, "journeyMapId": journeyMapId, "beginFilterDate":beginFilterDate, "endFilterDate": endFilterDate};
	
	containerNode.html('<div class="loader"></div>');
    LeoAdminApiUtil.getSecuredData(urlStr, params , function (json) {    	
        if (json.httpCode === 0 && json.errorMessage === '') {
			if(json.data.nodes.length > 0){
				containerNode.empty().show();
				noDataNode.hide()
			}
			else {
				containerNode.hide(); 
				noDataNode.show()
			}
        	var graphElements = { nodes: json.data.nodes, edges: json.data.edges }
        	var rootNodeId = json.data.rootNodeId;
        	window.profileTouchpointFlowObj =   renderDirectedGraph(containerId, graphElements, rootNodeId);
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
    });
}

const profileInfoLoadProcessor = function(data){
	 viewProfileId = data.id;
	 
	 var noinfo = '-';
	 
	 if(data.firstName !== "" || data.lastName !== ""){
		 data.fullName = data.firstName +  " " + data.middleName +  " " + data.lastName;
	 }
	 else {
		 data.fullName = "Web Visitor";
	 }
	 data.typeAsText = data.typeAsText.replaceAll('_',' ');
	 
	 if(data.age <= 0){
		 data.age = noinfo;
	 } 
	 if(data.livingLocation === ''){
		 data.livingLocation = "_";
	 }
	 if(data.primaryPhone === ''){
		 data.primaryPhone = noinfo;
	 }
	 if(data.firstName === ''){
		 data.firstName = noinfo;
	 }
	 if(data.lastName === ''){
		 data.lastName = noinfo;
	 }
	 if(data.primaryEmail === ''){
		 data.primaryEmail = noinfo;
	 }
	 if(typeof data.jobType === "number" ) {
		 var t = data.jobType;
		 if(t === 0){
			 data.jobTypeAsString = "UNSKILLED AND NON-RESIDENT";	 
		 }
		 else if(t === 1){
			 data.jobTypeAsString = "UNSKILLED AND RESIDENT";	 
		 }
		 else if(t === 2){
			 data.jobTypeAsString = "SKILLED";	 
		 }
		 else if(t === 3){
			 data.jobTypeAsString = "HIGHLY SKILLED";	 
		 }
		 else {
			 data.jobTypeAsString = noinfo;
		 }
	 }
	 
	 data.funnelStage = data.funnelStage ? data.funnelStage.toUpperCase() : noinfo;
	 
	 if(data.lastTrackingEvent){
		 data.lastTrackingEvent.createdAt = moment.utc(data.lastTrackingEvent.createdAt).local().format('YYYY-MM-DD HH:mm:ss');
		 data.lastTrackingEvent.fingerprintId = data.lastTrackingEvent.fingerprintId ? data.lastTrackingEvent.fingerprintId : "-";
	
		 if(data.lastTrackingEvent && data.lastTrackingEvent.refTouchpoint){
			 data.lastTrackingEvent.refTouchpoint.name = decodeURISafe(data.lastTrackingEvent.refTouchpoint.name);
		 }
	 }
	 else {
		 data.lastTrackingEvent.createdAt = noinfo;
	 }
	 
	 // decode for name of touchpoint
	 data.lastTouchpoint.name = decodeURISafe(data.lastTouchpoint.name);
	
	 return data;
}

const loadProfileListByFilter = function(rowSelectHandlerName, containerDomSel, loaderDomSel, tableDomSel, profileFilterParams, loadDataCallback, extAction) {
	var tableSchema = [
        {
            "data": "id" // 0
        },
        {
            "data": "firstName" // 1
        },
        {
            "data": "primaryEmail" // 2
        },
        {
            "data": "primaryPhone" // 3
        },
        {
            "data": "lastTouchpoint" // 4
        },
        {
            "data": "dataQualityScore" // 5
        },
        {
            "data": "totalLeadScore" // 6
        },
        {
            "data": "totalCLV" // 7
        },                
        {
            "data": "updatedAt" // 8
        },
        {
            "data": "status" // 9
        }
    ];
	
	var usersession = getUserSession();
    if (usersession) {
         var tableObj = $(tableDomSel).DataTable({
         	"lengthMenu": [[30, 50, 60], [30, 50, 60]],
         	'processing': true,
            'serverSide': true,
            'searching': false,
            'ordering': true,
            'serverMethod': 'POST',
             'ajax': {
                 url: baseLeoAdminUrl + '/cdp/profiles/filter',
                 contentType: 'application/json',
                 beforeSend: function (request) {
                 	$(loaderDomSel).show();
                 	$(containerDomSel).hide();
                     request.setRequestHeader("leouss", usersession);
                 },
                 data: function (d) {
                     // get data in DOM to build filtering params
         			d = profileFilterParams(d);
         			d.order.map(function(e){
		    			e.field = tableSchema[e.column] ? tableSchema[e.column].data : "";
		    		});
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
             		
             		// done, show UI
             		$(loaderDomSel).hide();
             		$(containerDomSel).show();
             		
             		return json.data;
                  }
             },
             'columnDefs': [
                 {
                     "render": function (id, type, row) {
                    	var checked = false;
                    	var html = "";
                    	if(typeof window[rowSelectHandlerName] === "function"){
							html = getTableRowCheckedBox(rowSelectHandlerName, tableDomSel, id, checked);
						}
                        return html;
                     },
                     "targets": 0,
                     "orderable": false
                 },
                 {
                     "render": function (data, type, row) {
                     	var fullname = 'Web Visitor';
                     	var cssClass = "datatable_text no_contact";
                     	var rowTitle = 'Data Profile of ';
                     	if(row.firstName.length > 0 || row.lastName.length > 0){
							var s = row.firstName + ' ' + row.lastName;
                     		fullname = textTruncate(s, 30);
                     		rowTitle = rowTitle + s;
                     		cssClass = "datatable_text has_contact";
                     	}
                     	else {
							rowTitle = rowTitle + fullname;
						}
                     	
                     	if(row.dataVerification) {
			        		 fullname = '<i class="fa fa-check-circle-o" aria-hidden="true" title="Verified Data" ></i> ' + fullname;
			        		 rowTitle = 'Verified ' + rowTitle;
			        	}
                        var callJsViewStr = "#calljs-leoCdpRouter('Customer_Profile_Info','" + row.id + "')";
                        return '<a class="' + cssClass + '" title="' + rowTitle + '" href="' + callJsViewStr + '" >' + fullname + '</a>';
                     },
                     "targets": 1,
                     "orderable": false
                 },
                 {
                     "render": function (data, type, row) {
                     	//format email
                     	if(typeof data === 'string' ) {							
                     		var callJsViewStr = "#calljs-leoCdpRouter('Customer_Profile_Info','" + row.id + "')";
                         	var link = '<a title="'+data+'" href="' + callJsViewStr + '" >' + textTruncate(data, 40) + '</a>';
                         	return '<div class="datatable_text ">'  + link + '</div>';
                     	}
                     	return '-';
                     },
                     "targets": 2,
                     "orderable": false
                 },
                 {
                     "render": function (data, type, row) {
                     	//format phone
                     	if(typeof data === 'string' ){							
                     		var callJsViewStr = "#calljs-leoCdpRouter('Customer_Profile_Info','" + row.id + "')";
                         	var link = '<a title="'+data+'" href="' + callJsViewStr + '" >' + textTruncate(data, 30) +'</a>';                         	
                         	return '<div class="datatable_text">'  + link + '</div>';
                     	}
                     	return '-';
                     },
                     "targets": 3,
                     "orderable": false
                 },
                 {
                     "render": function (data, type, row) {
                     	if(typeof row.lastTouchpoint !== "object") {
                     		row.lastTouchpoint = {"name":"No data"};
                     	}
                     	return '<div style="font-size:11.4px;" title="'+ row.lastTouchpoint.name +'">'  + textTruncate(row.lastTouchpoint.name, 50) + '</div>';
                     },
                     "targets": 4,
                     "orderable": false
                 },
                 {
                     "render": function (data, type, row) {
                     	var score =  new Number(data).toLocaleString();
                     	if( data <= 100  ) {
                     		return '<div class="datatable_text text-center">'  + score + ' </div>';
                     	}
                     	else if (data > 100 && data < 250) {
                     		return '<div class="highlight_text text-center profile_medium_score" >'  + score + '</div>';
                     	}
                     	else {
                     		return '<div class="highlight_text text-center profile_high_score"  >'  + score + '</div>';
                     	}
                     },
                     "targets": 5
                 },
                 {
                     "render": function (data, type, row) {
                     	var score =  new Number(data).toLocaleString();
                     	if(data < 20 ){
                     		return '<div class="datatable_text text-center">'  + score + ' </div>';
                     	}
                     	else if(data >= 20 && data < 100 ){
                     		return '<div class="highlight_text text-center profile_medium_score" >'  + score + '</div>';
                     	}
                     	else if(data >= 100){
                     		return '<div class="highlight_text text-center profile_high_score" >'  + score + '</div>';
                     	}
                     	return '<div class="highlight_text text-center" style="color:#404040!important;font-size:0.8em!important" >'  + score + ' </div>';
                     },
                     "targets": 6
                 },
                 {
                     "render": function (data, type, row) {
                     	var score =  new Number(data).toLocaleString();
                     	if(data < 20 ){
                     		return '<div class="datatable_text text-center">'  + score + ' </div>';
                     	}
                     	else if(data >= 20 && data < 100 ){
                     		return '<div class="highlight_text text-center profile_medium_score" >'  + score + '</div>';
                     	}
                     	else if(data >= 100){
                     		return '<div class="highlight_text text-center profile_high_score" >'  + score + ' </div>';
                     	}
                     	return '<div class="highlight_text text-center" style="color:#404040!important;font-size:0.8em!important" >'  + score + ' </div>';
                     },
                     "targets": 7
                 },
                 {
                     "render": function (data, type, row) {
                     	if(data){
                     		var date = moment.utc(data).local().format('YYYY-MM-DD - HH:mm:ss');
                             return '<div class="small text-center" style="color:#3300ff" >'  + date + '</div>';
                     	}
                     	return '-';
                     },
                     "targets": 8
                 },
                 {
                     "render": function (data, type, row) {
                     	var text = '<mark><i class="fa fa-check" aria-hidden="true"></i> ACTIVE </mark>';
                     	if(data === 0) {
                     		text = '<i class="fa fa-ban" aria-hidden="true"></i> INACTIVE';
                     	}
                     	else if(data === -1) {
                     		text = '<i class="fa fa-ban" aria-hidden="true"></i> INVALID';
                     	}
                     	else if(data === -4) {
                     		text = '<i class="fa fa-ban" aria-hidden="true"></i> REMOVED';
                     	}
                     	return '<div class="small text-center" style="color:#3300ff!important" title="The status of Profile">'  + text + '</div>';
                     },
                     "targets": 9
                 },
                 {
                     "render": function (data, type, row) {
                     	var callJsViewStr = "#calljs-leoCdpRouter('Customer_Profile_Info','" + row.id + "')";
                        var html = '<div style="width:76px;"> <a class="control" title="View Profile Report" href="' + callJsViewStr + '" > <i class="fa fa-info-circle" aria-hidden="true"></i> View</a>';
                     	if( $(tableDomSel).data('canEditData') ){
                     		var callJsEditStr = "#calljs-leoCdpRouter('Customer_Profile_Editor','" + row.id + "')";
                     		html += ' <br><a class="control" title="Edit Profile Data" href="' + callJsEditStr + '" > <i class="fa fa-pencil-square-o" aria-hidden="true"></i> Edit</a>';
                     	}
						if( typeof extAction === 'string') {
							html += extAction;
						}
						html += "</div>"
                        return html;
                     },
                     "targets": 10
                 }
             ],
             'columns': tableSchema,
             
             // sort by the updated datetime of profile
             'order': [[8, 'desc']]
         }); 
        return tableObj;
     }
     return false;
}


function loadDuplicatedProfiles() {
	selectedDuplicateProfileIdMap = {};
	
	var profileData = LeoCdpAdmin.routerContext.dataObject;
	
	var tableDomSel = "#duplicate_profile_table";
	var loaderDomSel = "#duplicate_profile_loader";
	var containerDomSel = "#duplicate_profile_container";
	
	var profileFilterParams = function(d) {
		d.excludeProfileId = profileData.id;
		
		d.emails = profileData.primaryEmail.length > 5 ? profileData.primaryEmail : '';
		d.emails = d.emails + ';' +  profileData.secondaryEmails.join(';'); 
		
		d.phones = profileData.primaryPhone.length > 3 ? profileData.primaryPhone : '';
		d.phones = d.phones + ';' +  profileData.secondaryPhones.join(';'); 
		
		d.visitorId = profileData.visitorId.length > 1 ? profileData.visitorId : '';
		d.crmRefId = profileData.crmRefId.length > 1 ? profileData.crmRefId : '';
		d.fingerprintId = profileData.fingerprintId.length > 1 ? profileData.fingerprintId : '';
		d.lastSeenIp = profileData.lastSeenIp.length > 1 ? profileData.lastSeenIp : '';
		d.lastUsedDeviceId = profileData.lastUsedDeviceId.length > 1 ? profileData.lastUsedDeviceId : '';
		return d;
	}
	
	// var extAction = ' <br><a class="control" title="Similarity Score" href="" > <i class="fa fa-star-half-o" aria-hidden="true"></i> Similarity </a>';
	var extAction = '';
	
	if(duplicateProfileTable !== false && typeof duplicateProfileTable.ajax === 'object'){
		duplicateProfileTable.ajax.reload();
	}
	else {
		duplicateProfileTable = loadProfileListByFilter(false, containerDomSel, loaderDomSel, tableDomSel, profileFilterParams, false, extAction);
	}	
}

// JS for resources/app-templates/leocdp-admin/modules/customer/customer-profile-info.html

function editCurrentProfile(){
    	gotoLeoCdpRouter('Customer_Profile_Editor', viewProfileId );
    }
    
function onErrorLoadProfileInfo(err){
	$('#data_model_holder').html(getHtmlErrorPanel(err));
	$('#btn_refreshCurrentProfile,  #btn_refreshCurrentProfile, #btn_editCurrentProfile').attr('disabled','disabled');
}

function profileInfoGoBack(){
	if(loadProfileInfoOk){
		history.back()
	}
	else {
		location.hash = "calljs-leoCdpRouter('Profile_Management')"
	}
}

function loadProfile360Analytics(profileId, crmRefId, visitorId) {
     console.log('loadProfile360Analytics profileId:' + profileId + " crmRefId:" + crmRefId  + " visitorId:" + visitorId);
     
     loadProfileDataIntoDOM(false, profileId, crmRefId, visitorId, profileInfoLoadProcessor, function(){
    	 loadProfileInfoOk = true;
    	 //console.log(LeoCdpAdmin.routerContext.dataObject);
		
    	 var profileData = LeoCdpAdmin.routerContext.dataObject;
    	 if(profileData.primaryAvatar != ''){
    		 $('#profile_avatar').attr('src', profileData.primaryAvatar);
    	 }
    	 var fullNameHtml = '';
    	 if(profileData.dataVerification) {
    		 fullNameHtml = '<i class="fa fa-check-circle-o" aria-hidden="true" title="Verified Data" ></i> ' + profileData.fullName;
    	 }
    	 else {
    		 fullNameHtml = profileData.fullName;
    	 }
    	 $('#profile_full_name').html(fullNameHtml);
    	 
    	 // journeyMaps, Segments, dataLabels
    	 renderTaxonomyTextData(profileData);
    	 
    	 // event flow
    	 loadEventsInProfileIndex = 0;

 		 loadProfileDataJourneyReport()
    	 
    	 // Profile Analytics Report
    	 loadJourneyReportForProfile(profileData);
    	 
    	 // init date time selector 
    	 var daysToQueryEventData = 30; // days
    	 initDateFilterComponent(true, null, null, daysToQueryEventData);
    	 
    	 setTimeout(function(){
    		 // feedback data from customer
        	 loadFeedbacksInProfileIndex = 0;
        	 loadFeedbackEventsInProfile();
    	 },5000);
    	 
    	 // timeseries
    	 setTimeout(function(){
        	 // Financial Events
        	 loadFinancialEventList();
		 },7000)
    	 
    	 //renderJourneyFlowChart();
    	 //loadProfileAttribution();
         //loadMediaTouchpoints();
         //personalityDiagram();
         
         // Deduplicate must worok with active profile only
         var btnDeduplicate = $('#btn_profile_deduplicate');
         if(profileData.status === 1){
        	btnDeduplicate.click(function(){ 
        		deduplicateCurrentProfileDialog(); 
        	});
         }
         else {
        	 btnDeduplicate.attr('disabled','disabled');
         }
         
         $('#profile_key_info').show();
     	 $('#profile_key_info_loader').hide();

		if(profileData.status != 1){
			$('#menu_duplicated_profiles').hide()
		}
     }, onErrorLoadProfileInfo);
} 


/////////////////////// JS for  profile-editor //////////////////////////

var saveProfileHandler = function () {
    iziToast.info({
    	title: 'Profile Editor',
	    message: 'System is processing ...',
	    timeout: 2000,
	});
	
	$('#data_model_holder').hide();
    $('#profile_editor_loader').show();
	$('#data_model_controller').find('button').attr('disabled','disabled')
	
	var callback = function(rs){
		if(rs != null){
			location.hash = "calljs-leoCdpRouter('Customer_Profile_Info','" + rs + "')";
			notifySavedOK('Profile');
		}
		else {
			console.error('saveProfileHandler ', rs)
		}
	};
	var data = buildUpdateProfileModel();
	console.log("buildUpdateProfileModel: ", data)
	var params = {'objectJson' : JSON.stringify(data) };
	LeoAdminApiUtil.callPostAdminApi('/cdp/profile/update', params, function (json) {
        if (json.httpCode === 0 && json.data !== '') {
			callback(json.data);
        } else {
            LeoAdminApiUtil.logErrorPayload(json);
        }
   	});
}

var formatSelectionList = function(selector) {
	$(selector + ":checked").parent().addClass('data_field');
	$(selector).click(function(){
		$(selector).parent().removeClass('data_field');
		$(this).parent().addClass('data_field');
	});
}


var loadProfileData = function (profileId) {
     
     var dataProcessor = function(data){
    	var formatDateTime = 'YYYY-MM-DD';
    	var dateOfBirth = false;
    	
    	if(data.dateOfBirth) {
    		dateOfBirth = data.dateOfBirth;
    	} 
    	else if(data.age > 0){
    		dateOfBirth = new moment().subtract(data.age, "years").toDate();
    	}
    	
    	var dateOfBirthUI = {
   	        useCurrent: false, 
   	        format: formatDateTime
   	    };
    	if(dateOfBirth !== false) {
    		dateOfBirthUI['defaultDate'] = moment.utc(dateOfBirth).local().format('YYYY-MM-DD');
    	}
		$('#profileDateOfBirth').datetimepicker(dateOfBirthUI).on('dp.change', function(e){ 
	        var age = moment().diff(e.date, 'years');
	        $('#profileAge').val(age);
	    });

    	return data;
     }
     
	 $('#data_model_holder').hide();
     $('#profile_editor_loader').show();
     loadProfileDataIntoDOM(true, profileId, false, false, dataProcessor, function(){
		 $('#data_model_holder').show();
     	 $('#profile_editor_loader').hide();
    	 //console.log(LeoCdpAdmin.routerContext.dataObject);
    	 editedProfileModel = LeoCdpAdmin.routerContext.dataObject;
    	 // avatar
    	 initProfileAvatarUploader(editedProfileModel);
     	 // profile type
     	 formatSelectionList('input[name="type"]')
     	 // profile status
     	 formatSelectionList('input[name="status"]')
     	 // profile gender
     	 formatSelectionList('input[name="gender"]')
     	 // profile maritalStatus
     	 formatSelectionList('input[name="maritalStatus"]')
     	 // set search link
     	 if(typeof editedProfileModel.livingLocation === "string") {
     		$('#locationCodeSearch').attr("href", 'https://www.google.com/maps/?q=' +  editedProfileModel.livingLocation);
     	 }
 		 $('#panel_profile_location').find('input[data-field="locationCode"]').change(function(){
			var newValue = $(this).val();
			// remove iframe tag in Google Map embedded HTML Code
			if( newValue.indexOf("<iframe") >= 0 ) {
				newValue = $(newValue).attr("src");
				$(this).val(newValue);
			}
		 });
		 $('#panel_profile_location').find('input[data-field="livingLocation"]').change(function(){ 
			// set new search link
			var newVal = encodeURIComponent($(this).val()) ;
			$('#locationCodeSearch').attr("href", 'https://www.google.com/maps/?q=' +  newVal);
		 })
		 var editable = false;
		 if(currentUserProfile.role === 6){
			 $('#profileJourneyMapEditorRow').show();
			 loadDataJourneyMapEditor('#profileJourneyMapEditor', editedProfileModel.inJourneyMaps);
			 
			 // data authorization
			 editable = true;
		 }
		 loadSystemUsersForDataAuthorization(editable, editedProfileModel, $('#authorizedProfileViewers'), $('#authorizedProfileEditors'));
     });
}

var buildUpdateProfileModel = function() {
	var dataDomHolder = $('#data_model_holder');
	// radio button
	dataDomHolder.find('input[data-field][type="radio"]:checked').each(function(){ 
		var field = $(this).data('field'); 
		var fieldtype = $(this).data('fieldtype'); 
		var val = $(this).val();
		if(fieldtype === 'int'){
			val = parseInt(val);
		} 
		else if(fieldtype === 'float'){
			val = parseFloat(val);
		}
		editedProfileModel[field] = val;
	});
	// checkbox
	dataDomHolder.find('input[data-field][type="checkbox"]').each(function(){ 
		var field = $(this).data('field'); 
		var checked = $(this).prop('checked');
		editedProfileModel[field] = checked;
	});
	// text input
	dataDomHolder.find('input[data-field][type="text"],input[data-field][type="email"],textarea').each(function(){ 
		var field = $(this).data('field'); 
		var val = $(this).val();
		editedProfileModel[field] = val;
	});
	// telephone input
	dataDomHolder.find('input[data-field][type="tel"]').each(function(){ 
		var field = $(this).data('field'); 
		var val = $(this).val().replace(/\D/g,'');
		editedProfileModel[field] = val;
	});
	// number input
	dataDomHolder.find('input[data-field][type="number"]').each(function(){ 
		var field = $(this).data('field'); 
		var fieldtype = $(this).data('fieldtype'); 
		var val = $(this).val();
		if(fieldtype === 'int'){
			val = parseInt(val);
		} else if(fieldtype === 'float'){
			val = parseFloat(val);
		}
		editedProfileModel[field] = val;
	});
	// hashset, hashmap
	dataDomHolder.find('p[data-field][data-editable]').each(function(){ 
		var pfield = $(this).data('field');  
		var pfieldtype = $(this).data('fieldtype'); 
		if(pfieldtype === 'hashset') {
			var list = [];
			$(this).find('input[type="text"]').each(function(){
				var fieldtype = $(this).data('fieldtype'); 
				var val = $(this).val();
				if(fieldtype === 'int'){
	    			val = parseInt(val);
	    		} else if(fieldtype === 'float'){
	    			val = parseFloat(val);
	    		}
				if(val.trim() !== ''){
					list.push(val);
				}
				console.log(val);
			})
			editedProfileModel[pfield] = list;
		}
		else if(pfieldtype === 'hashmap') {
			var map = {};
			var nodes = $('#'+ pfield).find('li');
			nodes.each(function(){
				var key = $(this).find('select').val(); 
				var value = $(this).find('input').val(); 
				if(key.trim() != '' && value.trim() != ''){
					map[key] = value;
				}
			});
			editedProfileModel[pfield] = map;
		}
		else if(pfieldtype === "custom_field" || pfieldtype === "custom_field_number") {
			var map = {};
			var nodes = $(this).find('li');
			nodes.each(function(){
				var fieldName = $(this).find("input.custom_field_name").val().trim();
				var fieldValue = $(this).find("input.custom_field_value").val().trim();
				if(pfieldtype === "custom_field_number"){
					if(fieldValue === ""){
						fieldValue = 0;
					}
					else {
						fieldValue = parseFloat(fieldValue);
					}
				}
				if(fieldName !== ''){
					map[fieldName] = fieldValue;
				}
				
			});
			editedProfileModel[pfield] = map;
		}
	});
	// date of birth
	var dateOfBirth = $('#profileDateOfBirth').data("DateTimePicker").date();
	if(dateOfBirth != null) {
		editedProfileModel.dateOfBirth = new moment.utc(dateOfBirth).local();
	}
	else {
		editedProfileModel.dateOfBirth = null;
	}
	
	// journey maps
	if(currentUserProfile.role === 6){
		editedProfileModel.inJourneyMaps = getJourneyMapsFromEditor('#profileJourneyMapEditor', editedProfileModel.inJourneyMaps);
		
		// data authorization
		editedProfileModel.authorizedViewers = $('#authorizedProfileViewers').val() || [];
		editedProfileModel.authorizedEditors = $('#authorizedProfileEditors').val() || [];
	}
	return editedProfileModel;
}

var initProfileAvatarUploader = function (profileData) {
	if(profileData.primaryAvatar === '' || profileData.primaryAvatar == null){
		var defaultAvatarUrl = baseLeoAdminUrl + '/public/images/avatars/profile-default-avatar.png';
		$('#profile_avatar_img').attr('src', defaultAvatarUrl);
	}
	else {
		 var avatarUrl = profileData.primaryAvatar;
		 $('#profile_avatar_img').attr('src', avatarUrl);
		 $('#profile_avatar_input').val(avatarUrl);
	}
	$('#profile_avatar_img').show();
	
	// profile avatar image uploader
	var refObjectClass = 'profile';
	var refObjectKey = 'avatar-'+ profileData.id;
    setupUploaderWidget(refObjectClass, refObjectKey, '#profile_avatar_uploader', function (rs) {
        if(rs.data.fileUrls.length > 0){
        	var imageFileUrl = rs.data.fileUrls[0];
        	$('#profile_avatar_img').attr('src', imageFileUrl);
       	 	$('#profile_avatar_input').val(imageFileUrl);
        }
    });
}

var getSelectedJourneyData = function(inJourneyMaps, checkJourney){
	for(var i=0; i< inJourneyMaps.length; i++) {
		var selectedJourney = inJourneyMaps[i];
		if(selectedJourney.id === checkJourney.id){
			return selectedJourney;
		}
	}
	return null;
}

var loadDataJourneyMapEditor = function(containerSelect, inJourneyMapsData) {
	var listNode = $(containerSelect);
	// request server
	LeoAdminApiUtil.getSecuredData('/cdp/journeymap/list', {}, function(json){
		if (json.httpCode === 0 && typeof json.data === 'object') {
			var loadedJourneyMaps = json.data;
        	
           	loadedJourneyMaps.forEach(function(journey){
           		var id = journey.id;
           		var name = journey.name;
           		var selectedStr = "", json = "";
           		var selectedInJourneyData = getSelectedJourneyData(inJourneyMapsData, journey);
           		
           		if(selectedInJourneyData != null){
           			if(selectedInJourneyData.queryHashedId === ""){
           				selectedStr = "selected"; 
           			}
           		}
           		else {
           			selectedInJourneyData = {
    		    		  "scoreCX": {
       		    		    "positive": 0,
       		    		    "neutral": 0,
       		    		    "negative": 0,
       		    		    "sentimentScore": 0,
       		    		    "sentimentType": 0,
       		    		    "positivePercentage": 0,
       		    		    "neutralPercentage": 0,
       		    		    "negativePercentage": 0,
       		    		    "happy": false
       		    		  },
       		    		  "funnelIndex": 1,
       		    		  "id": id,
       		    		  "name": name,
       		    		  "type": editedProfileModel.type > 0 ? "CONTACT" : "VISITOR",
       		    		  "indexScore": 1,
       		    		  "lastDataSynch": 0,
       		    		  "queryHashedId": ""
					};
           		}
           		json = encodeURIComponent(JSON.stringify(selectedInJourneyData));
           		var option = '<option id="profile_editor_journey_'+id+'" '+selectedStr+' value="'+ id +'" data-json="'+ json +'" >' + name + '</option>';
				listNode.append(option);
           	});
           	
           	listNode.chosen({
                width: "100%",
                no_results_text: "Oops, nothing found!"
            });
           	
        } else {
        	LeoAdminApiUtil.logErrorPayload(json);
        }
	});
}

var getJourneyMapsFromEditor = function(containerSelect, inJourneyMapsData) {
	var finalSelectedJourneys = [];
	$(containerSelect).find('option').each(function(){  
		var selected = $(this).is(':selected');
		console.log( $(this).val() +  " selected " + selected)
		
		var json = decodeURIComponent($(this).data('json')); 
		var journeyData = JSON.parse(json); 

		var oldSelectedInJourneyData = getSelectedJourneyData(inJourneyMapsData, journeyData);
		
		if(selected) {
			journeyData.queryHashedId = "";
			finalSelectedJourneys.push(journeyData) ;
		}
		else if(oldSelectedInJourneyData != null){
			oldSelectedInJourneyData.queryHashedId = "removed";
			finalSelectedJourneys.push(oldSelectedInJourneyData) ;
		}
	})
	return finalSelectedJourneys;
}
