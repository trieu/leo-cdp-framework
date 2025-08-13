
const setupImageUrlInput = function(inputSelector, imgSelector, defaultImgUrl){
	// set default when empty
	var emptyImgUrl = 'https://cdn-icons-png.flaticon.com/512/3875/3875148.png';
	defaultImgUrl = (defaultImgUrl == null || defaultImgUrl === "") ? emptyImgUrl : defaultImgUrl;
    
    // set Image src
	$(imgSelector).attr("src", defaultImgUrl).error(function(){
    	notifyErrorMessage($(this).attr('src') + " is not a valid image URL");
    	$(this).attr("src", emptyImgUrl);
    });

	// set input url for the image src
	$(inputSelector).val(defaultImgUrl).change(function(){
    	var newSrc = $(this).val().trim();
    	newSrc = (newSrc === "") ? emptyImgUrl : newSrc;
    	$(imgSelector).attr("src",newSrc);
    });
}

function insertTextAtCursorInCodeMirror(editor, text) {
    var doc = editor.getDoc();
    var cursor = doc.getCursor();
    doc.replaceRange(text, cursor);
}

function getHeadLinesImagsObject() {
    var obj = {};
    $('#headline_images .thumbnail').each(function () {
        var src = $(this).find('img').attr('src');
        var caption = "";
        var node = $(this).find('p.editable');
        if (!node.hasClass('editable-empty')) {
            caption = node.text().trim();
        }
        obj[src] = caption;
    });
    return obj;
}

const setupImageInputForContent = function(inputSelector, defaultImgUrl){
	// set default when empty
	var emptyImgUrl = 'https://cdn-icons-png.flaticon.com/512/3875/3875148.png';
	defaultImgUrl = (defaultImgUrl == null || defaultImgUrl === "") ? emptyImgUrl : defaultImgUrl;
    
	// set input url for the image src
	$(inputSelector).val(defaultImgUrl).change(function(){
    	var newSrc = $(this).val().trim();
    	newSrc = (newSrc === "") ? emptyImgUrl : newSrc;
    	
    	var newSrcCheck = $('#headline_images').find('img[src="'+newSrc+'"]').length === 0;
    	if( newSrcCheck){
			var data = {};
    		data[newSrc] = '';
    		loadHeadLineImageObjectToView(data, false, true);
		}
    });
    
    setTimeout(function(){
		var isEmptyCheck = $('#headline_images').show().find('img').length === 0;
	    if( isEmptyCheck ){
			var data = {};
			data[defaultImgUrl] = '';
			loadHeadLineImageObjectToView(data, false, true);
		}
	},1200)
}
    
const updateBreadcrumbInAssetGroup = function(assetCategories, groupTitle){
	for (var categoryId in assetCategories) {
	    var category = assetCategories[categoryId];
	    var name = category.name;
	    // groups in category
     	var params = leoCdpRouterParams([categoryId, category.assetType, category.name ]);
        $('#tnv_Asset_Groups').attr('href',"#calljs-leoCdpRouter('Asset_Groups','" + params + "')").text(name).attr('title', name);
	}
	$('#tnv_Asset_Group_Details').text(groupTitle);
}

const updateBreadcrumbInAssetPost = function(assetCategories, assetGroups, postTitle){
	for (var categoryId in assetCategories) {
	    var category = assetCategories[categoryId];
	    var name = category.name;
	    // groups in category
     	var params = leoCdpRouterParams([categoryId, category.assetType, category.name ]);
        $('#tnv_Asset_Groups').attr('href',"#calljs-leoCdpRouter('Asset_Groups','" + params + "')" ).text(name).attr('title', name);
	}
	
	for (var groupId in assetGroups) {
	    var group = assetGroups[groupId];
     	var title = group.title;
        $('#tnv_Asset_Group_Details').attr('href',"#calljs-leoCdpRouter('Asset_Group_Details','" + groupId + "')" ).text(title).attr('title', title);
	}
	$('#tnv_Content_Post').text(postTitle).attr('title', postTitle);	
}

const getAssetEditorCallJs = function(assetType, params) {
	var calljs = '';
	
	if(assetType === 2  ){
		// PRODUCT_ITEM_CATALOG product item editor 
		calljs =  "calljs-leoCdpRouter('Asset_Product_Item_Editor','" + params + "')";
	}
	else if(assetType === 3  ){
		// SUBSCRIPTION_ITEM_CATALOG
		calljs =  "calljs-leoCdpRouter('Asset_Service_Item_Editor','" + params + "')";
	}
	else if(assetType === 4 || assetType === 6 || assetType === 7){
		// template editor: EMAIL_CONTENT, PUSH_MESSAGE_CONTENT, WEB_HTML_CONTENT
		calljs =  "calljs-leoCdpRouter('Asset_Template_Editor','" + params + "')";
	}
	else if(assetType === 13  ){
		// FEEDBACK_FORM 
		calljs =  "calljs-leoCdpRouter('Asset_Feedback_Form_Editor','" + params + "')";
	}
	else if(assetType === 15  ){
		// SHORT_URL_LINK 
		calljs =  "calljs-leoCdpRouter('Asset_Short_Link_Editor','" + params + "')";
	}
	else if(assetType === 16  ){
		// PRESENTATION_ITEM_CATALOG using revealjs and Markdown
		calljs =  "calljs-leoCdpRouter('Asset_Presentation_Editor','" + params + "')";
	}
	// TODO for AssetType
	else {
		// CONTENT_ITEM_CATALOG , SOCIAL_EVENT, KNOWLEDGE_HUB
    	calljs =  "calljs-leoCdpRouter('Asset_Content_Editor','" + params + "')";
	}
	return calljs;
}

function getYouTubeVideoId(currentVal){
    var newval = '', vid = '';
    if (newval = currentVal.match(/(\?|&)v=([^&#]+)/)) {
        vid = (newval.pop());
    } else if (newval = currentVal.match(/(\.be\/)+([^\/]+)/)) {
        vid = (newval.pop());
    } else if (newval = currentVal.match(/(\embed\/)+([^\/]+)/)) {
        vid = (newval.pop().replace('?rel=0',''));
    }
    return vid.split('&')[0];
}

function checkPostTypeAndSetupMediaEditor(postType, value) {
    // TEXT , RICH_MEDIA, SLIDE_SHOW, IMAGE_SLIDESHOW, VIDEO_AND_BLOG
	console.log('checkPostTypeAndSetupMediaEditor postType ' + postType)
    if (postType === 1  || postType === 2 || postType === 6 || postType === 8 || postType === 9 ) {
        // human content editor
        $('#content_div').html(editorViewTpl);
        
        // global editor for current view
        window.trixElement = document.querySelector('trix-editor[input="mediaInfo"]');
        trixElement.editor.insertHTML(value);  
        
        // focus 
        $(trixElement).focus(function(){ trixElement = $(this)[0]; });
        
        trixElement.addEventListener("trix-attachment-add", function(event) {
            var attachment = event.attachment;
            if (attachment.file) {
                /*
                var editor = event.target.editor
                var originalRange = editor.getSelectedRange()
                var attachmentRange = editor.getDocument().getRangeOfAttachment(attachment)
                
                editor.setSelectedRange(attachmentRange)
                editor.activateAttribute("caption", "<Insert Caption>")
                editor.setSelectedRange(originalRange)
                */
            }
        });
        
		addEventListener("trix-initialize", function(event) {
		  	if($('#trix_btn_md_to_html').length === 0) {
				var btn_md_to_html = $('<button id="trix_btn_md_to_html" type="button" ><i class="fa fa-code"></i> Format Markdown Code</button>');
				$(".trix-button-group--text-tools").append(btn_md_to_html);
				btn_md_to_html.click(function(){
					var content = trixElement.editor.getDocument().toString();
					var html = DOMPurify.sanitize(marked.parse(content))
					trixElement.editor.loadHTML(html);  
				})
		 	}  
		})
    } else {
        var node = $(documentUrlViewTpl).val(value);
        $('#content_div').html(node);
        node.change(function () {
            renderPostMediaInfo(node.val().trim());
        });
    }
}

function normalizeMediaHtml(content){
    var tempNode = $('<div/>').html(content);
    tempNode.find('img').each(function(){
        var width = parseInt($(this).attr('width'));
        if(width > 320){
            $(this).attr('style','height:100%!important;width:100%!important;object-fit:contain!important');
        }
    });  
    return tempNode.html(); 
}

function showRawTextHtmlEditor(){
	if(trixElement && rawHtmlEditor){
		var w = $('#raw_media_container').show().width() - 10;
		var rawHtml = $('#mediaInfo').val()
        rawHtmlEditor.setValue(rawHtml);
        rawHtmlEditor.setSize(w, 600);
        CodeMirrorAutoFormat(rawHtmlEditor);        	
	}
	else {
		alert('trixElement or rawHtmlEditor is NULL !')
	}
}

function showTextHtmlEditor(){
	if(trixElement && rawHtmlEditor){
		var content = rawHtmlEditor.getValue();
		var html = DOMPurify.sanitize(content)
		trixElement.editor.loadHTML(html);          	
	}
	else {
		alert('trixElement or rawHtmlEditor is NULL !')
	}
}

function newAssetItemEditor(assetType, assetGroupId, assetCategoryId){
	var params = leoCdpRouterParams(['', assetGroupId, assetCategoryId ]) ;
	location.hash = getAssetEditorCallJs(assetType, params);
}

function gotoAssetItemView(itemId, groupId, categoryId) {
	var params = leoCdpRouterParams([itemId, groupId, categoryId]) ;
	location.hash = "calljs-leoCdpRouter('Asset_Content_View','" + params + "')";
}

function getFeedbackFormTypeAsString(templateType){
   	if(templateType === 0){
   		return "SURVEY";
   	}
   	else if(templateType === 1){
   		return "CONTACT";
   	}
   	else if(templateType === 2){
   		return "RATING";
   	}
   	else if(templateType === 3){
   		return "CES";
   	}
   	else if(templateType === 4){
   		return "CSAT";
   	}
   	else if(templateType === 5){
   		return "NPS";
   	}
	return "";
}

function refreshCodeMirrorEditor(){
	$('.CodeMirror').each(function(i, el){
	    el.CodeMirror.refresh(); el.CodeMirror.execCommand('selectAll');
	});
}

function initEmbeddedFeedbackForm(leoObserverId, idDialog, cxFeedbackTpl, iFrameHeight) {
	iFrameHeight = typeof iFrameHeight === "number" ? iFrameHeight : 350;
	
	var sharedDeviceMode = $('#leo_survey_in_shared_devices').is(":checked");
	var feedbackFormTitle = cxFeedbackTpl.title;
	var scriptNodeId = "leoFeedbackForm_" + cxFeedbackTpl.id;
	var params = sharedDeviceMode ? 'shared-devices=1&tplid=' : 'tplid=';
	var fullUrl = "https://" + baseLeoObserverDomain + "/webform?" + params;
	var tplFeedbackType = getFeedbackFormTypeAsString(cxFeedbackTpl.templateType);
	var eventName = tplFeedbackType + " Form";
	
	//init code preview editor
	var codeHolder = $('#feedback_form_code_holder');
	codeHolder.css("visibility","hidden");
	
	$('#feedback_form_code_holder .CodeMirror').remove();
	var editorSelector = codeHolder.find('textarea.code');
	var codeEditor = CodeMirror.fromTextArea(editorSelector[0], {
		mode:  "htmlmixed",
		lineNumbers: true,
        styleActiveLine: true,
        matchBrackets: true,
        readOnly: false,
    });
	
	codeEditor.setSize(null, 430);
	setTimeout(function() {
		$('#'+idDialog+' .eventName').text(eventName);
		var iframeHeight = (feedbackFormTitle.indexOf('Rating') > 0) ? 100 : 800;
		
		//copy from textarea template into editor, then selectAll for copy
		var tpl = editorSelector.val().trim();
		var code = tpl.replaceAll('__feedbackFormTitle__', feedbackFormTitle)
		code = code.replaceAll('__eventName__', eventName);
		code = code.replaceAll('__scriptNodeId__', scriptNodeId);
		code = code.replaceAll('__tplFeedbackType__', tplFeedbackType);
		code = code.replaceAll('__feedbackFormFullUrl__', fullUrl);
		code = code.replaceAll('__iFrameHeight__', iFrameHeight);
		code = code.replaceAll('__observerId__', leoObserverId);
		code = code.replaceAll('__iframeHeight__', iframeHeight);
		code = code.replaceAll('__templateId__', cxFeedbackTpl.id);
		
		codeEditor.getDoc().setValue( '<script id="'+scriptNodeId+'" >\n' + code + '\n<\/script>' )
		codeEditor.refresh();
		codeEditor.execCommand('selectAll');
		
		// copy code into clipboard
		var buttonSelector = '#'+idDialog+ ' button.btn-copy-code';
		addHandlerCopyCodeButton(editorSelector, buttonSelector);
		codeHolder.css("visibility","visible");
	},300);
	
	// setQrImageData 
	var imageUrl = 'https://' + window.baseUploadDomain + cxFeedbackTpl.qrCodeUrl.replace('./','/');
	$('#cx_qrHrefUrl').attr("href",imageUrl);
	$('#cx_qrImgSrc').attr("src",imageUrl);
	$('#cx_qrCodeImageURL').val(imageUrl);
	// form landing page URL
	var formUrl = "https://" + baseLeoObserverDomain + "/webform?" + params + cxFeedbackTpl.id + "&obsid=" + leoObserverId;
	$('#cx_qrFormURL').val(formUrl);
}
