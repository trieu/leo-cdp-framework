
// current URL
// const DEMO_DOMAIN = "datahub4vnuk.leocdp.net";
const DEMO_DOMAIN = "obs.example.com";

var crUrl = location.href;
var referrerUrl = document.referrer;
var skipTracking = crUrl.indexOf("https://demo.leocdp.net") === 0 || crUrl.indexOf("https://admin.leocdp.com") === 0 || typeof window.leoObserverId === "string";

function parseQueryData() {
	if(location.search.length > 4){
		var search = location.search.substring(1);
		return JSON.parse('{"' + decodeURI(search).replace(/"/g, '\\"').replace(/&/g, '","').replace(/=/g, '":"') + '"}');
	}
	return {};
}

/// LEO OBSERVER CODE

(function() {
	if(skipTracking){
		console.log("SKIP TESTING LEO CDP with CHROME, there are a LEO CDP Observer code " + window.leoObserverId)
		return;
	}
	
	var queryData = parseQueryData();
	
    // LEO Web Code for channel: Affiliate Marketing
	window.leoObserverLogDomain = decodeURIComponent(queryData._leoObserverLogDomain || DEMO_DOMAIN);
	window.leoObserverCdnDomain = decodeURIComponent(queryData._leoObserverCdnDomain ||(DEMO_DOMAIN + "/public"));
	
	// observer ID
	window.leoObserverId = decodeURIComponent(queryData._leoObserverId || "" );
	window.leoStarRatingTemplateId = decodeURIComponent(queryData._leoStarRatingTemplateId || "" );
	
	window.srcTouchpointName = encodeURIComponent(document.title);
	window.srcTouchpointUrl = encodeURIComponent(location.href);
	
	chrome.storage.sync.get(['leoObserverId', 'leoStarRatingTemplateId'], function(items) {
      	console.log('LEO CDP leoProxyJs loader, data retrieved', items);
      	
      	// try to get from query string, it no data, try to load from localStorage
      	window.leoObserverId = window.leoObserverId !== "" ? window.leoObserverId : items.leoObserverId;
      	window.leoStarRatingTemplateId = window.leoStarRatingTemplateId !== "" ? window.leoStarRatingTemplateId : items.leoStarRatingTemplateId;
      	
      	console.log("[LEO CDP] Chrome Ext, window.leoObserverId " , window.leoObserverId)
      	console.log("[LEO CDP] Chrome Ext, window.leoStarRatingTemplateId " , window.leoStarRatingTemplateId)
      	if(typeof window.leoObserverId === "string" && window.leoObserverId !== "" ) {
      		// load JS
			leoProxyJs();
			// update
			chrome.storage.sync.set({ 'leoObserverId':  window.leoObserverId }, function() {});
		}
		else {
			console.log("[LEO CDP] Chrome Ext, please set observerId to test ! ")	
		}
    });	
})();

function dataTracking() {
	if(typeof window.leoObserverId !== "string") {
		console.log("[LEO CDP] Chrome Ext, please set observerId to test dataTracking ! ")	
		return;
	}
	
	// facebook
	if(crUrl.indexOf('facebook.com') > 0 ) {
		window.leoObserverId = "4HiTOTdPShT9cyabqVNRnn";
		leoTrackEventPageView();
		
		setTimeout(function(){
			var html = '<h1 style="padding: 80px; text-align: center;"> Stay focus to build your dream ! </h1>';
			html += '<center> <img style="width:64%" src="https://gcore.jsdelivr.net/gh/trieu/leo-cdp-free-edition@latest/leo-cdp-how-it-works.png"/> </center>';
			document.write(html)
		}, 4000)
	}
	// Amazon 
	else if(crUrl.indexOf('.amazon.com') > 0 ) {
		window.leoObserverId = "7LhsRE07sHOamtC1aRppKk";
		// item-view tracking
		
		var placeYourOrder = jQuery('#placeYourOrder');
		var sessionProductId = localStorage.getItem('sessionProductId')
		var sessionIdType = localStorage.getItem('sessionIdType')
		
		if(crUrl.indexOf('/gp/product/') > 0 || crUrl.indexOf('/dp/') > 0 ) {
			var productId = jQuery('#printEditionIsbn_feature_div > div > div:nth-child(1) > span:nth-child(2)').text().trim();
			var idType = 'ISBN-13';
			if(productId === '') {
				var arr = jQuery('#detailBullets_feature_div ul:first li').text().trim().split('\n');
				arr.forEach(function(item, index){
					var toks = item.split('-');
					if( toks.length === 2 && item.length === 14 ) {
						if( parseInt(toks[0]) > 0) {
							productId = item;
						}
					}
				});
				if(productId === ''){
					productId = jQuery('input[name^="ASIN"]').val();
					idType = 'ASIN';
				}
			}
			console.log("Amazon productId " + productId)
			if(productId !== '') {
				
				leoTrackEventProductView([productId], idType);
				
				jQuery('#add-to-wishlist-button-submit').click(function(){
					leoTrackEventAddWishList([productId], idType)
				})
				jQuery('#add-to-cart-button').click(function(){
					leoTrackEventAddToCart([productId], idType)
					localStorage.setItem('sessionProductId',productId)
					localStorage.setItem('sessionIdType',idType)
				});
				
				// Feedback rating plugin
				jQuery('#title').append('<div id="leocdp_feedback_holder"></div>');
				
				var iframeWidth = "90%";
				if( jQuery('#leocdp_feedback_holder').width() < 600) {
					iframeWidth = false;
				}
				addFeedbackPlugin('leocdp_feedback_holder', productId, idType, iframeWidth);
				
				// add test buy button
				var btnSelector = jQuery('#buyNow').parent().empty();
				if(btnSelector.length === 0){
					btnSelector = jQuery('#checkoutButtonId').parent();
				}
				addBuyButtonOnAmazon('amazon', productId, idType, btnSelector);
				
			} else {
				leoTrackEventPageView();
			}
		} 
		else if(crUrl.indexOf('handlers/display.html')>0 && placeYourOrder.length === 1 
				&& typeof sessionProductId === 'string' && typeof sessionIdType === 'string') {
				addBuyButtonOnAmazon('amazon', sessionProductId, sessionIdType, placeYourOrder.parent());
				placeYourOrder.remove()
				localStorage.removeItem('sessionProductId');
				localStorage.removeItem('sessionIdType');
		}
		// listing page tracking
		else {
			leoTrackEventPageView();
		}
		return true;
	} 
	
	// Neta Book 
	else if (crUrl.indexOf('.netabooks.vn') > 0 ) { 
		// item-view tracking
		window.leoObserverId = "1EkAqnEezGO3qqJd9KPcx2";
		var dataModel = JSON.parse(jQuery('script[type="application/ld+json"]').html());
		
		if( typeof dataModel.offers === "object" && typeof dataModel.sku === "string" ) {
			
			console.log("dataLayer for ecomm ",dataModel) 
			
			var idType = "item_ID";
			var productId = dataModel.sku;
			var productPrice = dataModel.offers.price;
			
			leoTrackEventProductView([productId], idType);
			
			// Feedback rating plugin
			jQuery('div.product-intro').append('<div id="leocdp_feedback_holder"></div>')
			addFeedbackPlugin('leocdp_feedback_holder', productId, idType);

			jQuery("a.btn-buy").click(function(){
				leoTrackEventAddToCart([productId], idType);
				addBuyButtonOnAmazon('netabooks', productId, idType, jQuery(this).parent());
			})

		} 
		// listing page tracking
		else {
			leoTrackEventPageView();
		}
		return true;
	}
	
	// [Batdongsan website]
	else if (crUrl.indexOf('batdongsan.com') > 0) {
		var prid = jQuery('div[prid][id="product-detail-web"]').attr("prid") || "";
		
		jQuery('span[mobile][class="phoneEvent"]:first').click(function(e){ 
			var send = jQuery(this).attr("send") === "true"; 
			if(!send) {
				console.log("ASK"); 
				jQuery(this).attr("send","true");
				
				var mobile = jQuery(this).attr("mobile") || "";
				var eventData = {"mobile":mobile}
				LeoObserverProxy.recordViewEvent("click-show-phone",eventData);
			}  
		})
		
		// item-view tracking
		if(prid !== "") {
			var idType = 'item_ID';
			var eventData = {"productId":prid, "idType":idType}
			LeoObserverProxy.recordViewEvent("item-view",eventData);
		}
		// listing page tracking
		else {
			leoTrackEventPageView();
		}
		return true;
	}
	
	// [pvcfc.com.vn website]
	else if (crUrl.indexOf('pvcfc.com.vn') > 0) {
		var pathname = location.pathname;
		
		// item-view tracking
		if(location.href.indexOf('/san-pham/') > 0) {
			var idType = 'URL';
			var eventData = {"productId":pathname, "idType":idType}
			LeoObserverProxy.recordViewEvent("item-view",eventData);
		}
		// listing page tracking
		else {
			leoTrackEventPageView();
		}
		return true;
	}

	
	// Udemy
	else if (crUrl.indexOf('udemy.com') > 0) {
		
		var page_key = UD.userAgnosticTrackingParams ? UD.userAgnosticTrackingParams.page_key || "" : "";
		
		// item-view tracking
		if(page_key === "course_landing_page") {
			var idType = 'web_uri';
			var courseId = location.href.replace('https://www.udemy.com/course/','').split('/')[0];
			window.srcTouchpointName = encodeURIComponent(document.title);
			
			var eventData = {"courseId":courseId,"idType":idType}
			LeoObserverProxy.recordViewEvent("course-view",eventData);
		}
		// listing page tracking
		else {
			leoTrackEventPageView();
		}
		return true;
	}
	
	
	// mafc.com.vn
	else if (crUrl.indexOf('mafc.com.vn') > 0) {
		if(location.href.indexOf("vay-tin-chap-tien-mat-nhanh") > 0) {
			LeoObserverProxy.recordViewEvent("item-view",{"purpose":"cash"});
		}
		else if(location.href.indexOf("vay-mua-dien-may") > 0) {
			LeoObserverProxy.recordViewEvent("item-view",{"purpose":"electronics"});
		}
		else if(location.href.indexOf("vay-mua-xe") > 0) {
			LeoObserverProxy.recordViewEvent("item-view",{"purpose":"car-and-motorbike"});
		}
		else if(location.href.indexOf("vay-giao-duc-lam-dep") > 0) {
			LeoObserverProxy.recordViewEvent("item-view",{"purpose":"beauty-healthcare"});
		}
		// listing page tracking
		else {
			leoTrackEventPageView();
		}
		return true;
	}
	
	// Shopee
	else if (crUrl.indexOf('https://shopee.vn') === 0) {
		
		var ldNodes = jQuery('script[type="application/ld+json"]');
		
		// item-view tracking
		if(ldNodes.length > 1) {
			var productMetadata = JSON.parse(ldNodes[1].innerHTML)
			var idType = 'item_ID';
			var productId = productMetadata.productID;
			window.srcTouchpointName = encodeURIComponent(document.title);
			
			leoTrackEventProductView([productId], idType);
		}
		// listing page tracking
		else {
			leoTrackEventPageView();
		}
		
		initCaptureAllClickEventsInPage();
		return true;
	}
	
	// jysk
	else if (crUrl.indexOf('https://jysk.vn/') === 0) {
		
		var ldNodes = jQuery('script[type="application/ld+json"]');
		
		// item-view tracking
		if(ldNodes.length > 1) {
			var productMetadata = JSON.parse(ldNodes[1].innerHTML)
			var idType = 'item_ID';
			var productId = productMetadata.productID;
			window.srcTouchpointName = encodeURIComponent(document.title);
			
			leoTrackEventProductView([productId], idType);
		}
		// listing page tracking
		else {
			leoTrackEventPageView();
		}
		
		initCaptureAllClickEventsInPage();
		return true;
	}
	
	// Manning Book Publisher
	else if (crUrl.indexOf('https://www.manning.com') === 0 ) { 
		// item-view tracking
		if(crUrl.indexOf('https://www.manning.com/books/') === 0 ) {
			if(typeof viewContentPayload === "object" ) {
				var idType = 'ISBN-13';
				var productId = viewContentPayload.isbn;
				
				leoTrackEventProductView([productId], idType);
				
				jQuery('button.add-to-cart').click(function(){
					leoTrackEventAddToCart([productId], idType)
					addBuyButtonOnAmazon('manning', productId, idType);
				});
			}
		} 
		// listing page tracking
		else {
			leoTrackEventPageView();
		}
		return true;
	}
	
	
	// Phong Vu
	else if (crUrl.indexOf('https://phongvu.vn') === 0) {
		
		if(crUrl.indexOf('.html') > 0 ) {
			var idType = 'SKU';
			var ldJson = JSON.parse(jQuery('script[type="application/ld+json"]').text());
			var productId = ldJson.sku;
			if(productId !== '' ) {
				leoTrackEventProductView([productId], idType);
				jQuery('button[data-content-name="addToCart"]').click(function(){
					leoTrackEventAddToCart([productId], idType);
					
					addBuyButtonOnAmazon('phongvu', productId, idType);
				});
			}
		} else {
			leoTrackEventPageView();
		}
		return true;
	}
	
	// YouTube
	else if (crUrl.indexOf('https://www.youtube.com') === 0) {
		if(crUrl.indexOf('https://www.youtube.com/watch') === 0 ) {
			var idType = 'YOUTUBE_VID';
			var arr = location.href.split('v=');
			if(arr.length > 0) {
				var videoId = arr[1].trim();
				
				if("G6eeR7wd58w" === videoId){
					var aNode = jQuery('<br> <a style="color:yellow" />').text("Aqua City").attr("href","https://obs.example.com/ct/2nAhK6vM4doyZy9rZrMNjd");
					jQuery('h1[class="title style-scope ytd-video-primary-info-renderer"]').append(aNode);
				}
				
				var eventData = {"videoId": videoId,"idType":idType};
				
				leoTrackEventContentView(eventData);
				
				jQuery('#top-level-buttons .force-icon-button').first().click(function(){ 
					LeoObserverProxy.recordActionEvent("like", eventData);
				})
			}
		} else {
			leoTrackEventPageView();
		}
		return true;
	}
	
	// EDX
	else if( crUrl.indexOf('edx.org') > 0 ) {
		// demo for https://learning.edx.org/course/course-v1:AdelaideX+BigDataX+3T2017/home
		setTimeout(function(){
			if( crUrl.indexOf('learning.edx.org/course') > 0 ) {
				var npsBtn = '<a class="nav-item flex-shrink-0 nav-link" href="javascript:loadLeoFormNPS()" style="">NPS Survey</a>';
				jQuery('#main-content > div.course-tabs-navigation.mb-3 > div > nav').append(jQuery(npsBtn));
				// 
				if(jQuery('#unit-iframe').length === 1){
					loadLeoFormCSAT();
				}
			}
		},3000);
		
		//
		leoTrackEventPageView();
	}
	
	// klook.com
	else if( crUrl.indexOf('klook.com') > 0 ) {
		var content = jQuery('meta[property="og:type"]').attr('content');
		var ldNodes = jQuery('script[type="application/ld+json"]');
		
		window.leoObserverId = "5ZtxqFGjVI2TdyiOVLX4nT";
		if(content === "product" && ldNodes.length > 0){
			var idType = 'SKU';
			var meta = document.querySelector('meta[name="twitter:app:url:iphone"]');
			var productId = meta ? meta.content.replace('klook://activity/','') : "";
			window.srcTouchpointName = encodeURIComponent(document.title);
			leoTrackEventProductView([productId], idType);	
		}
		else {
			leoTrackEventPageView();
		}
	}
	
	// --------- NEWS -----------
	// sachhay24h
	else if( crUrl.indexOf('sachhay24h.com') > 0 ) {
		window.leoObserverId = "4YN4vSRFxrFbAto8Kb8H5T";
		leoTrackEventPageView();
		
		setTimeout(function(){
			jQuery('#content > div.right-content > div:nth-child(2)').css("margin-top","100px");
			jQuery('#content > div.right-content > div.banner').css("top","10px");
		},500)
		
		showRecommenderBox("#content > div.right-content > div.banner")
	}
	
	// bonobos.com
	else if( crUrl.indexOf('bonobos.com') > 0 ) {
		
		var node = document.querySelector('script[type="application/ld+json"]'); 
		if(node) {
			var json = node.innerHTML; 
			var data = JSON.parse(json);
				
			var type = data['@type'];
			if(type === "Product"){
				var productId = data.sku || "";
				window.srcTouchpointName = encodeURIComponent(data.name || "");
				leoTrackEventProductView([productId], "SKU");
			}
			else {
				leoTrackEventPageView();
			}
		}
		else {
			leoTrackEventPageView();
		}
	}

	else if(!skipTracking) {
		leoTrackEventPageView();
	}
	return false;
}

function leoObserverProxyReady(session) {
   //LEO JS is ready
   if( dataTracking() ) {
	   console.log("LEOCDP Chrome Test App, dataTracking is called OK ")
   } else {
	   console.log("LEOCDP Chrome Test App, dataTracking is called FAILED ")
   }
}

var parseDataUTM = window.parseDataUTM || function () {
    if (location.search.indexOf('utm_') > 0) {
        var search = location.search.substring(1);
        return JSON.parse('{"' + decodeURI(search).replace(/"/g, '\\"').replace(/&/g, '","').replace(/=/g, '":"') + '"}');
    }
}

function leoTrackEventPageView() {
	LeoObserverProxy.recordViewEvent("page-view", parseDataUTM() );
}

function leoTrackEventContentView(eventData) {
	if(typeof eventData === "object") {
		LeoObserverProxy.recordViewEvent("content-view", eventData);
	} else {
		console.log('Invalid params for leoTrackEventContentView')
	}
}

function leoTrackEventProductView(productIdList, idType) {
	if(typeof productIdList === "object" && typeof idType === "string") {
		var productIds = productIdList.join(";");
		var eventData = {"productIds": productIds, "idType":idType};
		console.log('leoTrackEventProductView', eventData)
		LeoObserverProxy.recordActionEvent("item-view", eventData);
	} else {
		console.log('Invalid params for leoTrackEventProductView')
	}
}

function leoTrackEventAddWishList(productIdList, idType) {
	if(typeof productIdList === "object" && typeof idType === "string" ) {		
		var productIds = productIdList.join(";");
		var eventData = {"productIds": productIds, "idType":idType};
		var shoppingCartItems = [];
		productIdList.forEach(function(productId) {
			shoppingCartItems.push({"itemId": productId, "idType" : idType, quantity : 1})
		})
		LeoObserverProxy.recordConversionEvent("add-wishlist", eventData , "", shoppingCartItems, 0, "USD");
		console.log('leoTrackEventAddToCart', shoppingCartItems)
	} else {
		console.log('Invalid params for leoTrackEventAddToCart')
	}
}

function leoTrackEventAddToCart(productIdList, idType, quantityNum) {
	quantityNum = typeof quantityNum === "number" ? quantityNum : 1;
	if(typeof productIdList === "object" && typeof idType === "string" ) {		
		var productIds = productIdList.join(";");
		var eventData = {"productIds": productIds, "idType":idType};
		var shoppingCartItems = [];
		productIdList.forEach(function(productId) {
			shoppingCartItems.push({"itemId": productId, "idType" : idType, quantity : 1})
		})
		LeoObserverProxy.recordConversionEvent("add-to-cart", eventData , "", shoppingCartItems, 0, "USD");
		console.log('leoTrackEventAddToCart', shoppingCartItems)
	} else {
		console.log('Invalid params for leoTrackEventAddToCart')
	}
}

function leoTrackEventOrderCheckout(productIdList, idType, quantityNum) {
	quantityNum = typeof quantityNum === "number" ? quantityNum : 1;
	if(typeof productIdList === "object" && typeof idType === "string" ) {
		var productIds = productIdList.join(";");
		var eventData = {"productIds": productIds, "idType":idType};
		var shoppingCartItems = [];
		productIdList.forEach(function(productId) {
			shoppingCartItems.push({"itemId": productId, "idType" : idType, quantity : 1})
		})
		LeoObserverProxy.recordConversionEvent("order-checkout", eventData , "", shoppingCartItems, -1, "USD");
		console.log('leoTrackEventOrderCheckout', shoppingCartItems)
	} else {
		console.log('Invalid params for leoTrackEventOrderCheckout')
	}
}

function leoTrackEventPurchasedOK(transactionId, shoppingCartItems) {
	if( typeof transactionId === "string"  && typeof shoppingCartItems === "object") {
		console.log('leoTrackEventPurchasedOK', transactionId, shoppingCartItems)
		LeoObserverProxy.recordConversionEvent("purchase", {} , transactionId, shoppingCartItems, -1, "USD");
	} else {
		console.log('Invalid params for leoTrackEventPurchasedOK')
	}
}

function loadLeoFormCES(){
	var svf = document.title;
	var tprefurl = location.href;
	var url  = 'https://obs.example.com/webform?tprefurl=' + tprefurl + '&svt=CES&_t=' + new Date().getTime();
	var iframe = '<div class="leocdp_iframe" > <iframe class="leocdp_iframe_small" src="'+url+'"></iframe> </div>';
	jQuery('#kingster-page-wrapper > div.kingster-bottom-page-builder-container.kingster-container > div > div > div > div.kingster-single-social-share.kingster-item-rvpdlr').append(iframe)
}

function loadLeoFormCSAT(){
	var svf = document.title;
	var tprefurl = location.href;
	var url  = 'https://obs.example.com/webform?tprefurl=' + tprefurl + '&svt=CSAT&_t=' + new Date().getTime();
	var iframe = '<div class="leocdp_iframe_container"> <iframe class="leocdp_iframe_responsive" src="'+url+'"></iframe> </div>';
	
	jQuery('div.unit-iframe-wrapper').append(iframe)
}

function loadLeoFormNPS(){
	var svf = document.title;
	var tprefurl = location.href;
	var url  = 'https://obs.example.com/webform?tprefurl=' + tprefurl + '&svt=NPS&_t=' + new Date().getTime();
	var iframe = '<div class="leocdp_iframe_container"> <iframe class="leocdp_iframe_responsive" src="'+url+'"></iframe> </div>';
	jQuery('#main-content > div.container-fluid').html(iframe)
}


function initCaptureAllClickEventsInPage() {
	window.captureEvents(Event.CLICK);
  	window.onclick = function(e){
  		var domain = 'https://shopee.vn';
  		if(e.path[0] && crUrl.indexOf(domain) === 0 ) {
  			var clickPath = getDomNodeSelector(e.path[0]);
  			console.log( clickPath )
  			var html = "<h3 style='font-size:18px'>Leo Observer, tracking clicked element: "+clickPath+"</h3>";
  	  	  	jQuery("body").prepend(html)
  	  	  			
  			var idxBtn =  clickPath.lastIndexOf("button>");
  			if(idxBtn >= 0){
  				idxBtn += "button".length;
  				var clickPathButton = clickPath.substr(0,idxBtn);
  				console.log( clickPathButton )
  	  	  		// TODO
  	  	  		var autoClickTrackingMap = {"html>body>div>div>div>div>div>div>div>div>div>div>div>button":true}
  	  	  		if( autoClickTrackingMap[clickPathButton]) {
  	  	  			var html = "<h1 style='font-size:24px'>Leo Observer, tracking clicked button : "+clickPathButton+"</h1>";
  	  	  			jQuery("body").prepend(html);
  	  	  			
  	  	  			// Shopee add-to-cart
	  	  	  		
	  	  			var ldNodes = jQuery('script[type="application/ld+json"]');
	  	  			// item-view tracking
	  	  			if(ldNodes.length > 1) {
	  	  				var productMetadata = JSON.parse(ldNodes[1].innerHTML)
	  	  				var idType = 'item_ID';
	  	  				var productId = productMetadata.productID;
	  	  				window.srcTouchpointName = encodeURIComponent(document.title);
	  	  				leoTrackEventAddToCart([productId], idType);
	  	  			}
		  	  		
  	  	  		}
  			}
  	  		
  		}
  	}
}
	
function getDomNodeSelector(context) {
	let index, pathSelector, localName;

  	if (context == null || context == "null") throw "not an dom reference";
  	// call getIndexDomNode function
  	index = getIndexDomNode(context);

  	while (context.tagName) {
	  	// selector path
	  	pathSelector = context.localName + (pathSelector ? ">" + pathSelector : "");
    	context = context.parentNode;
  	}
  	// selector path for nth of type
  	pathSelector = pathSelector + `:nth-of-type(${index})`;
  	return pathSelector;
}

// get index for nth of type element
function getIndexDomNode(node) {
	let i = 1;
	let tagName = node.tagName;
	while (node.previousSibling) {
		node = node.previousSibling;
		var check = tagName.toLowerCase() == node.tagName.toLowerCase();
		if ( node.nodeType === 1 && check ) {
			i++;
		}
	}
	return i;
}

function showRecommenderBox(selectorStr){
	LeoObserverProxy.synchLeoVisitorId(function(vid) {
		// url
		var cb = Math.floor(Math.random() * 100000);
		var src = "https://obs.example.com/ris/html/products?visid=" + vid;
		src += ("&tpurl=" + encodeURIComponent(location.href) );
		src += ("&cb=" + cb );
		// width and height
		var h = '700px';
		var w = '300px';
		// append DOM
		var f = document.createElement('iframe');
		f.setAttribute("src", src);
		f.setAttribute("style", "width:"+w+"; height:"+h+"; overflow-y:scroll; margin:auto; display:block; border:none;");
		f.setAttribute("frameBorder", "0");
		var s = document.getElementById('leo_recommender_box');
	    s.parentNode.insertBefore(f, s);
	})
	var s = jQuery(selectorStr).empty().append(jQuery('<div id="leo_recommender_box" ></div>'));
	s.css("background-color","#fff").css("position","sticky").css("top","90px")
}

function addFeedbackPlugin(domId, productId, idType, iframeWidth ){
	if(typeof window.leoStarRatingTemplateId !== "string"){
		document.getElementById(domId).html("<h3>LEO CDP Chrome Ext: leoStarRatingTemplateId is empty!</h3>");
		return;
	}
	
	//  Feedback/Survey Form Collector for Customer Feedback Score (CFS) form
	var tpUrl = jQuery('link[rel="canonical"]').attr('href');
	var productId = productId || "";
	var idType = idType || "";
	var iframeWidth = iframeWidth || "100%";
		
	if(tpUrl === "" || tpUrl === null){
		console.error('link[rel="canonical"] is empty or not found !')
		return;
	}
		
    var svf = document.title;
	var tplFeedbackType = "RATING";

    var templateId = window.leoStarRatingTemplateId;
	
	var url  = 'https://obs.example.com/webform?tplid=' + templateId;
	url = url + "&obsid=" +  window.leoObserverId;
	url = url + "&tplfbt=" + tplFeedbackType;
	url = url + "&tpname=" + encodeURIComponent(document.title);
	url = url + "&tpurl=" + encodeURIComponent(tpUrl);
	url = url + "&cb=" + new Date().getTime();
	
	// container
	var divWrapper = document.createElement("div"); 
	var cssDiv = "position: relative; width:"+iframeWidth+"; overflow: hidden; height: 110px;";
	divWrapper.setAttribute("style",cssDiv);
	
	// iframe 
	var iframe = document.createElement("iframe"); 
	var cssIframe = "position: relative; width: 100%; overflow: hidden; height: 100px;border: 0px;";
	iframe.setAttribute("style",cssIframe);
	iframe.setAttribute("src",url);
	divWrapper.appendChild(iframe);
	
	var referenceNode = document.getElementById(domId);
	if(referenceNode){
		referenceNode.parentNode.insertBefore(divWrapper, referenceNode.nextSibling);
	}
}


function amazonPurchaseSimulation(node){
	var source = jQuery(node).data('source').toUpperCase();
	var productId =  jQuery(node).data('productId').trim();
	var idType =  jQuery(node).data('idType').trim();
	var title = jQuery("#productTitle").text().trim();
	var fullUrl = jQuery('link[rel="canonical"]').attr("href");
	
	var salePrice = parseFloat(jQuery('#twister-plus-price-data-price').val());
	var originalPrice = salePrice;
	var imageUrl = jQuery('#landingImage').attr('src');
	
	jQuery(node).attr('disabled','disabled').css('background-color','#696969');
	
	
	var transactionId = source+ "_demo_"+ new Date().getTime();

	var shoppingCartItems = [];
	shoppingCartItems.push({
		source : 'amazon_test',
        name: title,
        itemId: productId,
        idType: idType,
        originalPrice: originalPrice,
        salePrice: salePrice,
        quantity: 1,
        currency: "USD",
        supplierId: "amazon",
        couponCode: "",
        fullUrl: fullUrl,
        imageUrl: imageUrl
    });
	leoTrackEventPurchasedOK(transactionId, shoppingCartItems);
	alert(' amazonPurchaseSimulation source ' + source + ' transactionId ' + transactionId);
	console.log("amazonPurchaseSimulation transactionId ", transactionId, shoppingCartItems)
}

function addBuyButtonOnAmazon(source, productId, idType, selector) {
	console.log("addBuyButtonOnAmazon",source, productId, idType);
	
	var leoBuyBtnStyle = 'button.leocdp_buy_btn { border: none; color: white; padding: 12px 40px; text-align: center; font-size: 18px; margin: 4px 2px; cursor: pointer;border-radius: 6px;}';
	leoBuyBtnStyle += 'button.leocdp_buy_btn:hover{background-color:orange} button.leocdp_buy_btn:focus{background-color:gray} ';
	
	var holderNode = selector ? selector : jQuery("body");
	holderNode.prepend('<style> '+ leoBuyBtnStyle + '</style>');
	holderNode.prepend('<div id="leo_test_holder" style="padding:20px;text-align:center;width: 100%;"></div>');
	
	var btn = jQuery('<button class="leocdp_buy_btn" type="button" style="background-color: #4CAF50" >Buy Button <br>(Demo CDP)</button> ');
	btn.data('source',source);
	btn.data('productId',productId);
	btn.data('idType',idType);
	btn.click(function(){
		amazonPurchaseSimulation(jQuery(this)[0])
	})
	jQuery('#leo_test_holder').append(btn)
}
