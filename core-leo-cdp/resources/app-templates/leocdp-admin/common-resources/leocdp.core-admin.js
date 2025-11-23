/**
 * 
 * @author tantrieuf31 (Thomas)
 * 
 * this script contains all functions for admin
 * TODO need to refactoring code
 * 
 */

const prefixCallJs = '#calljs-';
const pageDomSelector = '#page_main_content';
const mainNavigationBarSelector = '#main-navbar';
const MAIN_VIEW_HTML_URI = '/view/main-view.html?admin=1';

$(document).ready(function () {
	if(window.innerWidth < 1200 ){
		iziToast.warning({
		    title: 'Warning',
		    message: "<b> Your screen size is too small ! <br> The screen's width for Leo CDP Admin needs to be more than 1200 px </b>"
		});
	}
});

function loadLeoSystemMainView() {
	if(systemDbReady) {
		if (LeoAdminApiUtil.isLoginSessionOK()) {
	        //find action js
	        var idx = location.hash.indexOf(prefixCallJs);
	        if (idx >= 0) {
	            LeoCdpAdmin.loadView(MAIN_VIEW_HTML_URI, '#wrapper', function () {
	                mainViewReady();
	                setTimeout(function () {
	                    try {
	                        var jsCode = decodeURIComponent(location.hash.substring(idx + prefixCallJs.length).trim());
	                        if(jsCode.indexOf('leoCdpRouter') === 0){
								eval(jsCode);	
							}	                        
	                        $(pageDomSelector).show();
							$(mainNavigationBarSelector).css('visibility','visible')
	                    } catch (error) {
	                        alert(error);
	                    }
	                }, 1000);
	            }, true);
	        } else {
	            var uri = location.hash.length === 0 ? MAIN_VIEW_HTML_URI : location.hash.substring(1);
	            LeoCdpAdmin.loadView(uri, '#wrapper', function () {
	                //default view
	                mainViewReady();
	                
	                setTimeout(function(){
	                	leoCdpRouter('defaultRouter');
						$(pageDomSelector).show();
						$(mainNavigationBarSelector).css('visibility','visible')
	                },1000)  
	                
	            }, true);
	        }
	    } 
	    else {
			if(ssoLogin){
				LeoCdpAdmin.loadView('/view/login-sso.html?admin=1', '#wrapper');
			}
			else {
				LeoCdpAdmin.loadView('/view/login.html?admin=1', '#wrapper');
			}	        
	    }
	}
	else {
		LeoCdpAdmin.loadView('/view/setup.html?admin=1', '#wrapper');
	}
}

$(window).on('hashchange', function () {
    var idx = location.hash.indexOf(prefixCallJs);
    console.log("==> window.hashchange: " + location.hash + " idx:" + idx);
    if (idx >= 0) {        
        try {
			var jsCode = decodeURIComponent(location.hash.substring(idx + prefixCallJs.length)).trim();
			if(jsCode.indexOf('leoCdpRouter') === 0){
				eval(jsCode);	
			}            
        } catch (error) {
            console.error(error);
        }
    } else {
        LeoCdpAdmin.loadView(location.hash, '#wrapper');
    }
});

//////////////////////////////////////////////////// COMMON ////////////////////////////////////////////////

function loadMediaInfoView(mediaInfo, type, editMode) {
    var html = mediaInfo.trim();
    $('#mediaInfoDowdloadUrl').hide();
    if (type === 3) {
        // OFFICE_DOCUMENT
        if (mediaInfo.indexOf('http') != 0) {
            //FIXME
            mediaInfo = window.baseLeoAdminUrl + mediaInfo;
        }
        $('#mediaInfoDowdloadUrl').show().find('a').attr('href', mediaInfo);

        if (mediaInfo.indexOf('.pdf') > 0) {
            html = '<iframe width="100%" height="800" frameborder="0" src="public/js/doc-viewerjs/index.html#' + mediaInfo + `"></iframe>`;
        } else if (
            mediaInfo.indexOf('.docx') > 0 || mediaInfo.indexOf('.doc') > 0 || mediaInfo.indexOf('.docm') > 0 ||
            mediaInfo.indexOf('.pptx') > 0 || mediaInfo.indexOf('.ppt') ||
            mediaInfo.indexOf('.xls') || mediaInfo.indexOf('.xlsx') > 0) {
            var url = encodeURIComponent(mediaInfo);
            html = '<iframe width="100%" height="650" frameborder="0" src="https://view.officeapps.live.com/op/embed.aspx?src=' +
                url + '"></iframe>';
        } else if (mediaInfo.indexOf('.png') > 0 || mediaInfo.indexOf('.jpg') > 0) {
            html = '<img src="' + mediaInfo + '" style="max-width:100%;max-height:600px;" />';
        }
    } else if (type === 4) {
        //VIDEO from Google Drive
        if (mediaInfo.indexOf('https://drive.google.com/open') >= 0) {
            var vid = getQueryMapFromUrl(postModel.mediaInfo).id;
            html = '<div class="embed-responsive embed-responsive-4by3"><iframe class="embed-responsive-item" frameborder="0" src="https://drive.google.com/file/d/' +
                vid + '/preview"></iframe></div>';
        } else if (mediaInfo.indexOf('https://drive.google.com/file/d/') >= 0) {
            var url = mediaInfo.replace('/view', '/preview');
            html =
                '<div class="embed-responsive embed-responsive-4by3"><iframe class="embed-responsive-item" frameborder="0" src="' + url + '"></iframe></div>';
        }
        //VIDEO from uploaded or YouTube
        else if (mediaInfo.indexOf('.mp4') >= 0 ||
            mediaInfo.indexOf('https://youtu.be') >= 0 ||
            mediaInfo.indexOf('https://www.youtube.com') >= 0) {

            var placeHolderId = 'videoPlaceholder' + new Date().getTime();
            html = '<div id="' + placeHolderId + '" class="videoholder" style="width: 100%;"></div>';

            setTimeout(function () {
                var autoplay = true;
                var onReady = function (player) {
                    player.volume(0);
                }
                if (mediaInfo.indexOf('http') != 0) {
                    //FIXME
                    mediaInfo = window.baseLeoAdminUrl + mediaInfo;
                }
                MediaPlayerOne.create(autoplay, placeHolderId, mediaInfo, '', [], 0, 0, onReady);
            }, 360);

            $('#mediaInfoDowdloadUrl').show().find('a').attr('href', mediaInfo);
        }
    }
    if (html === '') {
        html = '<div class="alert alert-info"></div>';
    }
    if ( (type === 1 || type === 9) && editMode) {
        $('#mediaInfoPreview').hide();
    } else {
        $('#mediaInfoPreview').html(html);
    }
}


function searchContent(keywords) {
    $('#search_campaign_box').val(keywords);
    window.currentSearchKeywords = keywords;
    LeoCdpAdmin.loadView('/view/search-view.html?admin=1', pageDomSelector, function () {
        loadSearchingByKeywords(keywords);
    });
}

window.currentSearchKeywords = window.currentSearchKeywords || '';

function searchingByKeywords() {
    var k = $("#search_campaign_box").val();
    if (window.currentSearchKeywords != k) {
        window.currentSearchKeywords = k;
        location.href = '#calljs-searchContent("' + encodeURIComponent(k) + '")';
    }
}

function initMainSeach() {
    var domSelector = "#search_campaign_box";
    var usersession = lscache.get('usersession');
    var optionsAutocomplete = {
        url: window.baseLeoAdminUrl + "/cdp/profiles/search-suggestion",
        ajaxSettings: {
        	beforeSend: function (xhr) {
				xhr.setRequestHeader('leouss', usersession);
			}
        },
        getValue: "name",
        list: {
            match: {
                enabled: true
            },
            maxNumberOfElements: 10,
            showAnimation: {
                type: "slide",
                time: 200
            },
            hideAnimation: {
                type: "slide",
                time: 200
            },
            sort: {
                enabled: true
            },
            onSelectItemEvent: function () {
                var node = $(domSelector);
                var value = node.getSelectedItemData().name;
                if (value) {
                    node.val(value).trigger("change");
                }
            },
            onChooseEvent: function () {
                searchingByKeywords();
            }
        },
        theme: "round"
    };

    $(domSelector).easyAutocomplete(optionsAutocomplete).on('keyup', function (e) {
        if (e.keyCode == 13) {
            searchingByKeywords();
            $('.easy-autocomplete-container > ul').hide();
        }
    });
}

function toggleDiv(aNode, divSelector) {
    var node = $(divSelector);
    if (node.is(":visible")) {
        node.hide();
        var s = $(aNode).html().replace('Hide', 'Show');
        $(aNode).html(s);
    } else {
        node.show();
        var s = $(aNode).html().replace('Show', 'Hide');
        $(aNode).html(s);
    }
}

const setupTabPanels = function(showAllAdminTabs){
	$('ul[class="nav nav-tabs"]').each(function(){
		var ulNode = $(this);
		var targetTabsId = $(this).data('tab-content');
		
		// a[href] click handler
		ulNode.find('a').click(function(){ 
			//clear active element
			ulNode.find('li').removeClass('active'); 
	        $('#'+targetTabsId).find('div[class*="tab-pane"]').removeClass('active');
	        
	        //set active element    
	        $(this).parent().addClass('active');
	        var targetTab = $(this).data('target-tab');
	        $('#'+targetTab).addClass('active');
		});
		
		if(showAllAdminTabs === true) {
			// show tabs for superadmin only
			if(currentUserProfile.role === 6) {
				ulNode.find('li[data-for-superadmin="true"]').show()
			}
		}
	});
}

// load HTML and append into DOM 
const loadModalboxHtml = function (uri) {
    $.ajax({
        url: uri,
        type: 'GET',
        success: function (html) {
            $('#common_modalbox_html').append(html);
        },
        error: function (data) {
            console.error("loadModalboxHtml.error: ", data);           
        }
    });
}

const openLocationUrl = function(aThis) {
	var url = $(aThis).data('url');
	eModal.iframe(url,"Location Map");
}

const makeNodeEditable = function(selector){
    selector.attr('title','Editor').editable({
        type: 'textarea',
        rows: 3,
        inputclass: 'editable_text_editor'
    })
}

const defaultDateFormat = 'YYYY-MM-DD';
const defaultDateTimeFormat = 'YYYY-MM-DD HH:mm:ss';

const initDateFilterComponent = function(withTime, beginDateTime, endDateTime, daysGoBack){
	var formatDateTime = withTime === true ? defaultDateTimeFormat : defaultDateFormat;
	var beginFilterDateFormat = formatDateTime;
	var endFilterDateFormat = formatDateTime;
	
	var subtractValue = typeof daysGoBack === "number" ? daysGoBack : 90;
	var end = new moment().add(1,'days').format(formatDateTime);
	var begin = new moment().subtract(subtractValue, 'days').format(formatDateTime);
	
	if(beginDateTime != null){
		
		$('#beginFilterDate').datetimepicker({
	        useCurrent: false, 
	        format: beginFilterDateFormat,
	        defaultDate: new moment(beginDateTime).format(beginFilterDateFormat)
	    });
	} else {
		$('#beginFilterDate').datetimepicker({
		    format: beginFilterDateFormat,
		    defaultDate: begin
		});
	}
	
	if(endDateTime != null){
		$('#endFilterDate').datetimepicker({
	        useCurrent: false, 
	        format: endFilterDateFormat,
	        defaultDate: new moment(endDateTime).format(endFilterDateFormat)
	    });
	} else {
		$('#endFilterDate').datetimepicker({
	        useCurrent: false, 
	        format: endFilterDateFormat,
	        defaultDate: end
	    });
	}
    
}

const getDateFilterValues = function(){
	var checked = $('#disable_date_filter:checked').length === 1;
	if(checked) {
		return { beginFilterDate : '', endFilterDate : '' }
	} else {
		var bDate = $('#beginFilterDate').data("DateTimePicker").date().format(defaultDateFormat + 'T00:00:00Z');
		var eDate = $('#endFilterDate').data("DateTimePicker").date().format(defaultDateFormat + 'T23:59:59Z');
		return { beginFilterDate : bDate, endFilterDate : eDate }
	}
}

const getRawDateFilterValues = function(){
	var checked = $('#disable_date_filter:checked').length === 1;
	if(checked) {
		return { beginFilterDate : '', endFilterDate : '' }
	} else {
		var bDate = $('#beginFilterDate').data("DateTimePicker").date();
		var eDate = $('#endFilterDate').data("DateTimePicker").date();
		return { beginFilterDate : bDate, endFilterDate : eDate }
	}
}

// for file uploader in trix editor
document.addEventListener("trix-file-accept", function(event) {
  event.preventDefault();
});

const getOperatorsForStringField = function(){
	var operators = ["equal", "not_equal", "is_null", "is_not_null","begins_with","not_begins_with","contains","not_contains","ends_with","not_ends_with","is_empty","is_not_empty"];
	return operators;
}

const getOperatorsForStringEqualityField = function(){
	var operators = ["equal", "not_equal"];
	return operators;
}

const getOperatorsForNumberField = function(){
	var operators = ["equal", "not_equal","less","less_or_equal","greater","greater_or_equal","between","not_between" ];
	return operators;
}

const getOperatorsForDateField = function(){
	var operators = ["is_today", "is_valid_date", "is_not_valid_date", "equal","not_equal","less","less_or_equal","greater","greater_or_equal", 
				"between","not_between", "compare_month_and_day", "compare_month_and_day_between" ,
				"compare_month_and_day_with_now","compare_year_month_day_with_now"];
	return operators;
}

function roundNumber(value, decimals) {
    return Number(Math.round(value + 'e' + decimals) + 'e-' + decimals);
}

function getCheckedBoxIcon(check){
	var html = '<i style="font-size:1.5em;color:#3300ff" class="fa fa-check-square-o" aria-hidden="true"></i>';
	if( ! check ){
		html = '<i style="font-size:1.5em;color:#3300ff" class="fa fa-square-o" aria-hidden="true"></i>';
	}
	return html;
}

function getTableRowCheckedBox(rowSelectHandlerName, tableDomSel, id, checked){
	var cbId = tableDomSel.substring(1) + '_' + id;
	var cssClass = checked ? 'fa fa-check-square-o' : 'fa fa-square-o' ; 
	var selected = checked ? "1" : "0";
	var icon = '<i id="row_checkbox_'+cbId+'" data-item-id="'+id+'" data-handler-name="'+rowSelectHandlerName+'" class="'+cssClass+'" aria-hidden="true" data-selected="'+selected+'" ></i>';
	var html = '<div class="datatable_text text-center datatable_row_checkbox" onclick="datatableRowCheckboxHandler(this)" data-row-checkbox-id="'+cbId+'" >' + icon + '</div>'
	return html;
}

function datatableRowCheckboxHandler(thisNode){
	var rowId = $(thisNode).data('row-checkbox-id');
	var node = $('#row_checkbox_' + rowId);
	var selected = node.data("selected");
	var rowSelectHandler = node.data("handler-name");
	var itemId = node.data("item-id");
	var handler = window[rowSelectHandler];
	if(selected == "0"){
		node.attr('class','fa fa-check-square-o').data('selected','1');
		if(typeof handler === 'function'){
			handler(true, itemId)
		}
	}
	else {
		node.attr('class','fa fa-square-o').data('selected','0');
		if(typeof handler === 'function'){
			handler(false, itemId)
		}
	}
}

function checkToAddUrlValueToDOM(value){
	if(typeof value === 'string' && value.trim().indexOf('http') === 0 ){
		return $('<a/>').attr('href',value).attr('target','_blank').html(value)[0].outerHTML;
	}
	return value;
}

function buildSystemUserInfoLink(userLogin){
	var callJsStr = "#calljs-leoCdpRouter('User_Login_Report','" + userLogin + "')";
	return $('<a/>').attr('href',callJsStr).html(userLogin)[0].outerHTML;
}


const mapClipboardJS = {};
function addHandlerCopyCodeButton(editorSelector, buttonSelector){
	if(mapClipboardJS[buttonSelector] == null){
		mapClipboardJS[buttonSelector] = true;
		new ClipboardJS(buttonSelector, {
		    text: function(trigger) {
		    	notifySuccessMessage("Successfully copied to clipboard!")
		        return getCodeMirrorNative(editorSelector).getDoc().getValue();
		    }
		})
		return true;
	}
	return false;
}

function getCodeMirrorNative(target) {
    var _target = target;
    if (typeof _target === 'string') {
        _target = document.querySelector(_target);
    }
    if (_target === null || !_target.tagName === undefined) {
        throw new Error('Element does not reference a CodeMirror instance.');
    }

    if (_target.className.indexOf('CodeMirror') > -1) {
        return _target.CodeMirror;
    }

    if (_target.tagName === 'TEXTAREA') {
        return _target.nextSibling.CodeMirror;
    }

    return null;
}

function openUrlInNewTab(e, node){
	e.stopPropagation();
	window.open($(node).attr("title"), '_blank');
	return false;
}

function stringToSlug(str) {
	// remove accents
	var from = "àáãảạăằắẳẵặâầấẩẫậèéẻẽẹêềếểễệđùúủũụưừứửữựòóỏõọôồốổỗộơờớởỡợìíỉĩịäëïîöüûñçýỳỹỵỷ";
	var to = "aaaaaaaaaaaaaaaaaeeeeeeeeeeeduuuuuuuuuuuoooooooooooooooooiiiiiaeiiouuncyyyyy";
	for (var i = 0, l = from.length; i < l; i++) {
		str = str.replace(RegExp(from[i], "gi"), to[i]);
	}
	str = str.toLowerCase().trim().replace(/[^a-z0-9\-]/g, '-').replace(/-+/g,'-');
	return str;
}

////////////////////////// jsGrid: common types and plugins /////////////////////

// checkbox type
const CheckBoxIcon = function(config) {
    jsGrid.Field.call(this, config);
};
CheckBoxIcon.prototype = new jsGrid.Field({
    sorter: function(date1, date2) {
        return 0;
    },

    itemTemplate: function(isChecked) {
    	return getCheckedBoxIcon(isChecked);
    },
    
    insertTemplate: function(value) {
        return this._insertCode = value;
    },

    editTemplate: function(value) {
        return this._editCode = value;
    },

    insertValue: function() {
        return this._insertCode;
    },

    editValue: function() {
        return this._editCode;
    }
});
jsGrid.fields.CheckBoxIcon = CheckBoxIcon;

// CustomCheckBox in grid
var CustomCheckBox = function(config) {
    jsGrid.Field.call(this, config);
};

CustomCheckBox.prototype = new jsGrid.Field({
    sorter: function(date1, date2) {
        return 0;
    },
    itemTemplate: function(isChecked) {
    	return '<div class="text-center">' + getCheckedBoxIcon(isChecked) + '</div>';
    },
    insertTemplate: function(value) {
    	var inputNode = $('<input type="checkbox" >');
    	if(value){
    		inputNode.attr('checked', 'checked');
    	}
    	return this._insertCheckbox = $('<div class="text-center">' + inputNode[0].outerHTML + '</div>');
    },
    editTemplate: function(value) {
    	var inputNode = $('<input type="checkbox" >');
    	if(value){
    		inputNode.attr('checked', 'checked');
    	}
        return this._editCheckbox = $('<div class="text-center">' + inputNode[0].outerHTML + '</div>');
    },
    insertValue: function() {
    	return this._insertCheckbox.find('input:checked').length > 0;
    },
    editValue: function() {
		console.log(this._insertCheckbox)
        return this._editCheckbox.find('input:checked').length > 0;
    }
});
jsGrid.fields.CustomCheckBox = CustomCheckBox;

// Url Link in grid
const UrlLink = function(config) {
    jsGrid.Field.call(this, config);
};
UrlLink.prototype = new jsGrid.Field({
    sorter: function(date1, date2) {
        return 0;
    },
    itemTemplate: function(url) {
    	var urlText = (url.length > 45) ? (url.substring(0,45)+"...") : url;
    	if( url.indexOf('https://plus.codes/') === 0 ) {
    		return '<a href="javascript:" style="font-size:11.6px" data-url="' + url + '" onclick="openLocationUrl(this)" >' + urlText + '</a>';
    	}
    	return '<a href="' + url + '"  style="font-size:11.6px" target="_blank" >' + urlText + '</a>';
    },
    insertTemplate: function(value) {
        return this._insertCode = value;
    },
    editTemplate: function(value) {
        return this._editCode = value;
    },
    insertValue: function() {
        return this._insertCode;
    },
    editValue: function() {
        return this._editCode;
    }
});
jsGrid.fields.UrlLink = UrlLink;

//bold text
const BoldText = function(config) {
    jsGrid.Field.call(this, config);
};
BoldText.prototype = new jsGrid.Field({
    sorter: function(date1, date2) {
        return 0;
    },
    itemTemplate: function(text) {
    	return '<b>' + text + '</b>';
    },
    insertTemplate: function(value) {
        return this._insertCode = value;
    },
    editTemplate: function(value) {
        return this._editCode = value;
    },
    insertValue: function() {
        return this._insertCode;
    },
    editValue: function() {
        return this._editCode;
    }
});
jsGrid.fields.BoldText = BoldText;

// from GMT time to local time
const JsGridLocalTime = function(config) {
    jsGrid.Field.call(this, config);
};
JsGridLocalTime.prototype = new jsGrid.Field({
    sorter: function(date1, date2) {		
		if (date1 < date2) {
			return 1;
		} else if (date1 > date2) {
			return -1;
		} else {
			return 0;
		}
	},
    itemTemplate: function(time) {
		var text = toLocalDateTime(time);
    	return '<span>' + text + '</span>';
    },
    insertTemplate: function(value) {
        return this._insertCode = value;
    },
    editTemplate: function(value) {
        return this._editCode = value;
    },
    insertValue: function() {
        return this._insertCode;
    },
    editValue: function() {
        return this._editCode;
    }
});
jsGrid.fields.JsGridLocalTime = JsGridLocalTime;
