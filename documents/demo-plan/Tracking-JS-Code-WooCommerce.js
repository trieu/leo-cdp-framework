
(function() {
  //Leo Web Observer for channel: E-commerce Bookshop
  window.leoObserverLogDomain = "demotrack.leocdp.net";
  window.leoObserverCdnDomain = "demostatic.leocdp.net";
  window.leoObserverId = "4npiIZQyApj6gCo0FGHMkb";
  window.srcTouchpointName = encodeURIComponent(document.title);
  window.srcTouchpointUrl = encodeURIComponent(location.protocol + '//' + location.host + location.pathname);

  var leoproxyJsPath = '/js/leo-observer/leo.proxy.js';
  var src = location.protocol + '//' + window.leoObserverCdnDomain + leoproxyJsPath;
  var jsNode = document.createElement('script');
  jsNode.async = true;
  jsNode.src = src;
  var s = document.getElementsByTagName('script')[0];
  s.parentNode.insertBefore(jsNode, s);
})();


function parseQuery(queryString) {
    var query = {};
    var pairs = (queryString[0] === '?' ? queryString.substr(1) : queryString).split('&');
    for (var i = 0; i < pairs.length; i++) {
        var pair = pairs[i].split('=');
        query[decodeURIComponent(pair[0])] = decodeURIComponent(pair[1] || '');
    }
    return query;
}


function leoObserverProxyReady() {
  //Leo is ready, record event data
  
  // Add To Cart Button Click
  jQuery('button[name="add-to-cart"]').click(function(e){ 
	//e.preventDefault();
	  
    var sku = jQuery('span.sku').map(function() { return jQuery(this).text().trim(); }).get().join(';');
    console.log("### Add To Cart, SKU:" + sku);
    leoRecordEventAddToCart(sku);
  });
  
  // Place Order Button Click
  jQuery('button[name="woocommerce_checkout_place_order"]').click(function(e){ 
    //e.preventDefault();
    
	var orderSku = jQuery('span.sku').map(function() { return jQuery(this).text().trim(); }).get().join(';');
    localStorage.setItem('orderSku', orderSku);
    
    var firstname = jQuery('#billing_first_name').val();
    var lastname = jQuery('#billing_last_name').val();
    var email = jQuery('#billing_email').val();
    var phone = jQuery('#billing_phone').val();
    var livinglocation = jQuery('#billing_address_1').val();
    var loginProvider = "woocommerce_billing";
    
    var profileData = {'loginProvider': loginProvider, 'firstname': firstname, 'lastname': lastname, 'email': email, 'phone' : phone, 'livinglocation' : livinglocation}
    LeoObserverProxy.updateProfileBySession(profileData);
    console.log("### woocommerce_checkout_place_order, SKU:" + sku);
  });
  
  // tracking when page loaded 
  var isOrderReceived = location.href.indexOf('/checkout/order-received/') > 0;
  var isProductView = location.href.indexOf('/product/') > 0;
  var isCheckoutView = location.href.indexOf('/checkout/') > 0;
  var skuNode = jQuery('span.sku');
  
  if(isOrderReceived) { 
    // order received
	var orderSku = localStorage.getItem('orderSku') || ''; 
	localStorage.removeItem('orderSku');
    
	var orderId = jQuery('.woocommerce-order-overview__order > strong').text()
    console.log("### order-received, orderId:" + orderId);
    leoRecordEventOnlinePurchase(orderSku, orderId);
  } 
  else if(isCheckoutView && skuNode.length > 0 ) { 
    // Checkout to Buy
    var sku = jQuery('span.sku').map(function() { return jQuery(this).text().trim(); }).get().join(';');
    console.log("### checkout, SKU:" + sku);
    leoRecordEventOrderCheckout(sku);
  } 
  else {
    // record event content-view or product-view
     if( skuNode.length > 0 && isProductView) {
        var sku = jQuery('span.sku').map(function() { return jQuery(this).text().trim(); }).get().join(';');
        leoRecordEventProductView(sku);
     } else {
        LeoObserverProxy.recordViewEvent("page-view");
     }
  }
  
  // One Signal Notification 
  setTimeout(function(){
  	// update OneSignal Data user ID to Leo CDP
    var isSynchData = typeof localStorage.getItem("synch-onesignal-to-leocdp") === 'string';
    if(window.OneSignal && ! isSynchData ){
      OneSignal.isPushNotificationsEnabled(function(isEnabled) {
        if (isEnabled) {
            // user has subscribed
            OneSignal.getUserId( function(userId) {
                console.log('player_id of the subscribed user is : ' + userId);
                // Make a POST call to your server with the user ID   
              	var onesignalData = {notificationProvider: 'onesignal', notificationUserId : userId };
                LeoObserverProxy.updateProfileBySession(onesignalData);
                localStorage.setItem("synch-onesignal-to-leocdp","true")
            });
        }
      });
    }
  },1200)
}

function leoRecordEventContentView() {
  LeoObserverProxy.recordViewEvent("content-view");
}

function leoRecordEventProductView(skuProductList) {
  LeoObserverProxy.recordViewEvent("item-view", {'skuProductList': skuProductList });
}

function leoRecordEventPlayVideo() {
  LeoObserverProxy.recordViewEvent("play-video");
}

function leoRecordEventLikeProduct(skuProductList) {
  if(skuProductList) {
    LeoObserverProxy.recordActionEvent("like",{"skuProductList": skuProductList});
  }
}

function leoRecordEventAddToCart(skuProductList) {
  if(skuProductList) {
    LeoObserverProxy.recordActionEvent("add-to-cart",{"skuProductList": skuProductList});
  }
}

function leoRecordEventOrderCheckout(skuProductList) {
  if(skuProductList) {
    LeoObserverProxy.recordActionEvent("order-checkout",{"skuProductList": skuProductList});
  }
}

function leoRecordEventPurchase(skuProductList, orderId) {
  if(orderId) {
    LeoObserverProxy.recordConversionEvent("purchase",{"skuProductList": skuProductList}, orderId);
  }
}
