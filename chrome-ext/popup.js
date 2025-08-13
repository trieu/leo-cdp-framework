document.addEventListener('DOMContentLoaded', documentEvents, false);

function saveData(leoObserverId, leoStarRatingTemplateId) {

	chrome.storage.sync.set({ 'leoObserverId': leoObserverId, 'leoStarRatingTemplateId': leoStarRatingTemplateId }, function() {
		var msg = "The Data Observer ID [" + leoObserverId + "] and Rating Template ID [" + leoStarRatingTemplateId + "] is saved OK ";
		jQuery('#info').text(msg).show();
	});
}

function loadData(){
	chrome.storage.sync.get(['leoObserverId', 'leoStarRatingTemplateId'], function(items) {
      	console.log('LEO CDP Settings retrieved', items);
      	var leoObserverId = items.leoObserverId || "";
      	var leoStarRatingTemplateId = items.leoStarRatingTemplateId || "";
	      	
      	jQuery('#leoObserverId').val(leoObserverId);
      	jQuery('#leoStarRatingTemplateId').val(leoStarRatingTemplateId);
		
		if(leoObserverId == ""){
			jQuery('#info').text("Please set leoObserverId !").show();
		}
    });	
}

function documentEvents() {
	
	//document.getElementById('load_btn').addEventListener('click', loadData);
	loadData()
	
	jQuery('#ok_btn').click(function() {
		jQuery('#info').hide()
		
		var leoObserverId = jQuery('#leoObserverId').val();
		var leoStarRatingTemplateId = jQuery('#leoStarRatingTemplateId').val();
		
		saveData(leoObserverId, leoStarRatingTemplateId);
	});
	// you can add listeners for other objects ( like other buttons ) here 
}