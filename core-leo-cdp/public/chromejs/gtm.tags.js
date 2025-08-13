// (1) LEO OBSERVER: load JavaScript code for [Demo blog]
(function() {
	window.leoObserverLogDomain = "demotrack.leocdp.net";
	window.leoObserverCdnDomain = "demo.leocdp.net/public";
	window.leoObserverId = "3UsJfOzYUXaC5SBfnfnTyQ";
	// Data Touchpoint Metadata
	window.srcTouchpointName = encodeURIComponent(document.title);
	window.srcTouchpointUrl = encodeURIComponent(location.href);

	var leoproxyJsPath = '/js/leo-observer/leo.proxy.min.js';
	var src = location.protocol + '//' + window.leoObserverCdnDomain
			+ leoproxyJsPath;
	var jsNode = document.createElement('script');
	jsNode.async = true;
	jsNode.defer = true;
	jsNode.src = src;
	var s = document.getElementsByTagName('script')[0];
	s.parentNode.insertBefore(jsNode, s);
})();

// (2) LEO OBSERVER: set-up all event tracking functions
var LeoObserver = {};

// (2.1) function to track Action Event "AcceptTracking"
LeoObserver.recordEventAcceptTracking = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("accept-tracking", eventData);
}

// (2.2) function to track View Event "PageView"
LeoObserver.recordEventPageView = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordViewEvent("page-view", eventData);
}

// (2.3) function to track Action Event "Search"
LeoObserver.recordEventSearch = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("search", eventData);
}

// (2.4) function to track View Event "ContentView"
LeoObserver.recordEventContentView = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordViewEvent("content-view", eventData);
}

// (2.5) function to track View Event "ItemView"
LeoObserver.recordEventItemView = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordViewEvent("item-view", eventData);
}

// (2.6) function to track View Event "CourseView"
LeoObserver.recordEventCourseView = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordViewEvent("course-view", eventData);
}

// (2.7) function to track Action Event "PlayVideo"
LeoObserver.recordEventPlayVideo = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("play-video", eventData);
}

// (2.8) function to track Action Event "ClickDetails"
LeoObserver.recordEventClickDetails = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("click-details", eventData);
}

// (2.9) function to track Feedback Event "SurveyView"
LeoObserver.recordEventSurveyView = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordFeedbackEvent("survey-view", eventData);
}

// (2.10) function to track Action Event "ShortLinkClick"
LeoObserver.recordEventShortLinkClick = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("short-link-click", eventData);
}

// (2.11) function to track Action Event "QRCodeScan"
LeoObserver.recordEventQRCodeScan = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("qr-code-scan", eventData);
}

// (2.12) function to track Action Event "NotificationClick"
LeoObserver.recordEventNotificationClick = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("notification-click", eventData);
}

// (2.13) function to track Action Event "RegisterAccount"
LeoObserver.recordEventRegisterAccount = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("register-account", eventData);
}

// (2.14) function to track Action Event "SubmitContact"
LeoObserver.recordEventSubmitContact = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("submit-contact", eventData);
}

// (2.15) function to track Action Event "UserLogin"
LeoObserver.recordEventUserLogin = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("user-login", eventData);
}

// (2.16) function to track Action Event "EmailClick"
LeoObserver.recordEventEmailClick = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("email-click", eventData);
}

// (2.17) function to track Action Event "JoinWorkshop"
LeoObserver.recordEventJoinWorkshop = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("join-workshop", eventData);
}

// (2.18) function to track Action Event "JoinWebinar"
LeoObserver.recordEventJoinWebinar = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("join-webinar", eventData);
}

// (2.19) function to track Action Event "JoinCommunity"
LeoObserver.recordEventJoinCommunity = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("join-community", eventData);
}

// (2.20) function to track Action Event "CheckIn"
LeoObserver.recordEventCheckIn = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("check-in", eventData);
}

// (2.21) function to track Action Event "AskQuestion"
LeoObserver.recordEventAskQuestion = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordActionEvent("ask-question", eventData);
}

// (2.22) function to track Conversion Event "AddWishlist"
LeoObserver.recordEventAddWishlist = function(eventData, shoppingCartItems,
		transactionId, transactionValue, currencyCode) {
	// need 5 params
	eventData = typeof eventData === "object" ? eventData : {};
	shoppingCartItems = typeof shoppingCartItems === "object" ? shoppingCartItems
			: [];
	transactionId = typeof transactionId === "string" ? transactionId : "";
	transactionValue = typeof transactionValue === "number" ? transactionValue
			: 0;
	currencyCode = typeof currencyCode === "string" ? currencyCode : "USD";
	// submit data
	LeoObserverProxy.recordConversionEvent("add-wishlist", eventData,
			transactionId, shoppingCartItems, transactionValue, currencyCode);

}

// (2.23) function to track Conversion Event "ProductTrial"
LeoObserver.recordEventProductTrial = function(eventData, shoppingCartItems,
		transactionId, transactionValue, currencyCode) {
	// need 5 params
	eventData = typeof eventData === "object" ? eventData : {};
	shoppingCartItems = typeof shoppingCartItems === "object" ? shoppingCartItems
			: [];
	transactionId = typeof transactionId === "string" ? transactionId : "";
	transactionValue = typeof transactionValue === "number" ? transactionValue
			: 0;
	currencyCode = typeof currencyCode === "string" ? currencyCode : "USD";
	// submit data
	LeoObserverProxy.recordConversionEvent("product-trial", eventData,
			transactionId, shoppingCartItems, transactionValue, currencyCode);

}

// (2.24) function to track Conversion Event "AddToCart"
LeoObserver.recordEventAddToCart = function(eventData, shoppingCartItems,
		transactionId, transactionValue, currencyCode) {
	// need 5 params
	eventData = typeof eventData === "object" ? eventData : {};
	shoppingCartItems = typeof shoppingCartItems === "object" ? shoppingCartItems
			: [];
	transactionId = typeof transactionId === "string" ? transactionId : "";
	transactionValue = typeof transactionValue === "number" ? transactionValue
			: 0;
	currencyCode = typeof currencyCode === "string" ? currencyCode : "USD";
	// submit data
	LeoObserverProxy.recordConversionEvent("add-to-cart", eventData,
			transactionId, shoppingCartItems, transactionValue, currencyCode);

}

// (2.25) function to track Conversion Event "OrderCheckout"
LeoObserver.recordEventOrderCheckout = function(eventData, shoppingCartItems,
		transactionId, transactionValue, currencyCode) {
	// need 5 params
	eventData = typeof eventData === "object" ? eventData : {};
	shoppingCartItems = typeof shoppingCartItems === "object" ? shoppingCartItems
			: [];
	transactionId = typeof transactionId === "string" ? transactionId : "";
	transactionValue = typeof transactionValue === "number" ? transactionValue
			: 0;
	currencyCode = typeof currencyCode === "string" ? currencyCode : "USD";
	// submit data
	LeoObserverProxy.recordConversionEvent("order-checkout", eventData,
			transactionId, shoppingCartItems, transactionValue, currencyCode);

}

// (2.26) function to track Conversion Event "Purchase"
LeoObserver.recordEventPurchase = function(eventData, shoppingCartItems,
		transactionId, transactionValue, currencyCode) {
	// need 5 params
	eventData = typeof eventData === "object" ? eventData : {};
	shoppingCartItems = typeof shoppingCartItems === "object" ? shoppingCartItems
			: [];
	transactionId = typeof transactionId === "string" ? transactionId : "";
	transactionValue = typeof transactionValue === "number" ? transactionValue
			: 0;
	currencyCode = typeof currencyCode === "string" ? currencyCode : "USD";
	// submit data
	LeoObserverProxy.recordConversionEvent("purchase", eventData,
			transactionId, shoppingCartItems, transactionValue, currencyCode);

}

// (2.27) function to track Conversion Event "Enroll"
LeoObserver.recordEventEnroll = function(eventData, shoppingCartItems,
		transactionId, transactionValue, currencyCode) {
	// need 5 params
	eventData = typeof eventData === "object" ? eventData : {};
	shoppingCartItems = typeof shoppingCartItems === "object" ? shoppingCartItems
			: [];
	transactionId = typeof transactionId === "string" ? transactionId : "";
	transactionValue = typeof transactionValue === "number" ? transactionValue
			: 0;
	currencyCode = typeof currencyCode === "string" ? currencyCode : "USD";
	// submit data
	LeoObserverProxy.recordConversionEvent("enroll", eventData, transactionId,
			shoppingCartItems, transactionValue, currencyCode);

}

// (2.28) function to track Conversion Event "Subscribe"
LeoObserver.recordEventSubscribe = function(eventData, shoppingCartItems,
		transactionId, transactionValue, currencyCode) {
	// need 5 params
	eventData = typeof eventData === "object" ? eventData : {};
	shoppingCartItems = typeof shoppingCartItems === "object" ? shoppingCartItems
			: [];
	transactionId = typeof transactionId === "string" ? transactionId : "";
	transactionValue = typeof transactionValue === "number" ? transactionValue
			: 0;
	currencyCode = typeof currencyCode === "string" ? currencyCode : "USD";
	// submit data
	LeoObserverProxy.recordConversionEvent("subscribe", eventData,
			transactionId, shoppingCartItems, transactionValue, currencyCode);

}

// (2.29) function to track Feedback Event "ChatforSupport"
LeoObserver.recordEventChatforSupport = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordFeedbackEvent("chat-for-support", eventData);
}

// (2.30) function to track Conversion Event "MadePayment"
LeoObserver.recordEventMadePayment = function(eventData, shoppingCartItems,
		transactionId, transactionValue, currencyCode) {
	// need 5 params
	eventData = typeof eventData === "object" ? eventData : {};
	shoppingCartItems = typeof shoppingCartItems === "object" ? shoppingCartItems
			: [];
	transactionId = typeof transactionId === "string" ? transactionId : "";
	transactionValue = typeof transactionValue === "number" ? transactionValue
			: 0;
	currencyCode = typeof currencyCode === "string" ? currencyCode : "USD";
	// submit data
	LeoObserverProxy.recordConversionEvent("made-payment", eventData,
			transactionId, shoppingCartItems, transactionValue, currencyCode);

}

// (2.31) function to track Feedback Event "SubmitRatingForm"
LeoObserver.recordEventSubmitRatingForm = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordFeedbackEvent("submit-rating-form", eventData);
}

// (2.32) function to track Feedback Event "SubmitCommentForm"
LeoObserver.recordEventSubmitCommentForm = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordFeedbackEvent("submit-comment-form", eventData);
}

// (2.33) function to track Conversion Event "Engage"
LeoObserver.recordEventEngage = function(eventData, shoppingCartItems,
		transactionId, transactionValue, currencyCode) {
	// need 5 params
	eventData = typeof eventData === "object" ? eventData : {};
	shoppingCartItems = typeof shoppingCartItems === "object" ? shoppingCartItems
			: [];
	transactionId = typeof transactionId === "string" ? transactionId : "";
	transactionValue = typeof transactionValue === "number" ? transactionValue
			: 0;
	currencyCode = typeof currencyCode === "string" ? currencyCode : "USD";
	// submit data
	LeoObserverProxy.recordConversionEvent("engage", eventData, transactionId,
			shoppingCartItems, transactionValue, currencyCode);

}

// (2.34) function to track Feedback Event "SubmitFeedbackForm"
LeoObserver.recordEventSubmitFeedbackForm = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordFeedbackEvent("submit-feedback-form", eventData);
}

// (2.35) function to track Feedback Event "SubmitCESForm"
LeoObserver.recordEventSubmitCESForm = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordFeedbackEvent("submit-ces-form", eventData);
}

// (2.36) function to track Feedback Event "SubmitCSATForm"
LeoObserver.recordEventSubmitCSATForm = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordFeedbackEvent("submit-csat-form", eventData);
}

// (2.37) function to track Feedback Event "SubmitNPSForm"
LeoObserver.recordEventSubmitNPSForm = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordFeedbackEvent("submit-nps-form", eventData);
}

// (2.38) function to track Feedback Event "NegativeFeedback"
LeoObserver.recordEventNegativeFeedback = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordFeedbackEvent("negative-feedback", eventData);
}

// (2.40) function to track Feedback Event "NegativeSocialReview"
LeoObserver.recordEventNegativeSocialReview = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordFeedbackEvent("negative-social-review", eventData);
}

// (2.43) function to track Feedback Event "ViralNegativeReview"
LeoObserver.recordEventViralNegativeReview = function(eventData) {
	eventData = eventData ? eventData : {};
	LeoObserverProxy.recordFeedbackEvent("viral-negative-review", eventData);
}

// (3) LEO OBSERVER is ready
function leoObserverProxyReady(session) {
	// auto tracking when LEO CDP JS is ready
	
	var metadata = typeof dataLayer === "object" ? dataLayer[0] : false;
	var name = "";
	var isItemView = false;
	try {
		isItemView = (document.querySelector('li[class="breadcrumb-item active"]').innerText || "").trim() === "Product Detail";
		var nodes =  document.querySelectorAll('a[href="/user/372/edit"]');
		if(nodes.length > 1){
			name = nodes[1].innerText.trim();
		}
	}catch(e){
		
	}
	
	if(isItemView) {
		LeoObserver.recordEventItemView();
	} else {
		LeoObserver.recordEventPageView();
	}
	
	var shouldUpdateProfile = true;
	if(shouldUpdateProfile) {
		var userInfo = {};
        userInfo["loginId"] = "123";
        userInfo["loginProvider"] = "talentnet";
        userInfo["email"] = "contact@uspa.tech";
        userInfo["firstName"] = "Trieu";
        userInfo["lastName"] = "Nguyen";
	}

	
}
