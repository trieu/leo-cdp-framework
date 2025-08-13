
// survey form template => HTML form => Java Data Model => ArangoDB => Survey Analytics, Customer Data Profile, Customer Research
function parseSurveyTemplate(rawSurveyTemplate, language) {
	
	var st = rawSurveyTemplate;
	var rows = st.split('\n');
	var cRatingQuestionGroup = false;
	var tplData = {};
	
	for(var i=0; i < rows.length; i++) {  
		var r = rows[i].trim(); 
		// 1 header tags of survey
		tplData = processHeader(r, tplData);
		
		// 2 e.g: [Profile First Name], [Profile Last Name], [Profile Email], 
		tplData = processProfileTags(r, tplData);
		
		// 3 e.g: Who is the final decision maker for your enrollment at VNUK ?
		tplData = processDecisionMakerTags(r, tplData);
		
		// 4 e.g: [Survey Evaluated Object] Name of course
		tplData = processEvaluatedObjectTags(r, tplData);
		
		// 5 e.g: [Survey Evaluated Person] The lecturer
		tplData = processEvaluatedPersonTags(r, tplData);
		
		// 6 e.g: [Survey Media Source Label] Which media channels do you hear about us ?
		tplData = processDataSourceTags(r, tplData);
		
		// 7 e.g: 
		tplData = processSurveyMetaDataTags(r, tplData);
		
		// 8 e.g: The lecturer was well-prepared with rating scale from 1 to 5
		tplData = processRatingQuestionTags(r, tplData);
		
		// 9 e.g: [Survey Multiple Choice Question] primary-reasons => Please tell us why you chose this program ? 
		tplData = processMultipleChoiceQuestionsTags(r, tplData);
		
		// 10 e.g: [Survey Single Choice Question] final-purpose => Please tell us the final purpose after your graduation ? 
		tplData = processSingleChoiceQuestionsTags(r, tplData);
		
		// 11 e.g: [Survey Extra Text Question] current-courses-for-learning-outcomes => Do you think the courses in the current Curriculum
		tplData = processExtraTextQuestionTags(r, tplData);
		
		// 12 footer tags of survey
		tplData = processFooter(r, tplData);
	}

	function processHeader(r, sData) {
		// Header Image
		if(r.indexOf('[Survey Header Image URL]') >= 0) {
		 	var d = r.substr('[Survey Header Image URL]'.length).trim()
		 	sData['Header_Image_URL'] = d;
		}
		// Header
		else if(r.indexOf('[Survey Header]') >= 0) {
		 	var d = r.substr('[Survey Header]'.length).trim();
		 	sData['Header'] = d;
		}
		// Survey Image URL
		else if(r.indexOf('[Survey Image URL]') >= 0) {
		 	var d = r.substr('[Survey Image URL]'.length).trim()
		 	sData['Survey_Image_URL'] = d;
		}
		// Survey URL
		else if(r.indexOf('[Survey URL]') >= 0) {
		 	var toks = r.substr('[Survey URL]'.length).trim().split('=>');
			if(toks.length === 2){
				sData['Survey_URL'] = toks[0].trim();
				sData['Survey_URL_Text'] = toks[1].trim();
			}
		}
		// Description
		else if(r.indexOf('[Survey Description]') >= 0) {
		 	var d = r.substr('[Survey Description]'.length).trim()
		 	sData['Description'] = d;
		}
		// Time Period
		else if(r.indexOf('[Survey Time Period]') >= 0) {
			var toks = r.substr('[Survey Time Period]'.length).trim().split('=>');
			if(toks.length === 2){
				sData['Time_Period'] = toks[0].trim();
				sData['Time_Period_Label'] = toks[1].trim();
			}
		}
		
		return sData;
	}
	
	function processFooter(r, sData) {
		// Footer
		if(r.indexOf('[Survey Footer]') >= 0) {
		 	var d = r.substr('[Survey Footer]'.length).trim()
		 	sData['Footer'] = d;
		}
		
		// Footer Image
		else if(r.indexOf('[Survey Footer Image URL]') >= 0) {
		 	var d = r.substr('[Survey Footer Image URL]'.length).trim()
		 	sData['Footer_Image_URL'] = d;
		}
		
		return sData;
	}
    
	function processProfileTags(r, sData) {
		
		// Profile Info Guide
		if(r.indexOf('[Profile Info Guide]') >= 0) {
			var d = r.substr('[Profile Info Guide]'.length).trim()
		 	sData['Profile_Info_Guide'] = d;
		}
		
		// Profile First Name
		if(r.indexOf('[Profile First Name]') >= 0) {
			var d = r.substr('[Profile First Name]'.length).trim()
		 	sData['Profile_First_Name_Label'] = d;
		}
		
		// Profile Last Name
		else if(r.indexOf('[Profile Last Name]') >= 0) {
			var d = r.substr('[Profile Last Name]'.length).trim()
		 	sData['Profile_Last_Name_Label'] = d;
		}
		
		// Profile Birth Date
		else if(r.indexOf('[Profile Birth Date]') >= 0) {
			var d = r.substr('[Profile Birth Date]'.length).trim()
		 	sData['Profile_Birth_Date_Label'] = d;
		}
		
		// Profile Gender
		if(r.indexOf('[Profile Gender]') >= 0) {
			var d = r.substr('[Profile Gender]'.length).trim();
			var choices = [];
			choices.push('Female')
			choices.push('Male')
			choices.push('Others')
		 	sData['Profile_Gender'] = {"label":d, "choices":choices};
		}
		
		// Profile Age
		else if(r.indexOf('[Profile Age]') >= 0) {
			var d = r.substr('[Profile Age]'.length).trim()
		 	sData['Profile_Age_Label'] = d;
		}
		
		// Profile Age Group
		else if(r.indexOf('[Profile Age Group]') >= 0) {
			var d = r.substr('[Profile Age Group]'.length).trim();
			var choices = [];
			choices.push("[18 - 24]")
			choices.push("[25 - 34]")
			choices.push("[35 - 44]")
			choices.push("[45 - 54]")
			choices.push("[55 - 64]")
			choices.push("[65 - 74]")
			choices.push("[75 - 99]")
		 	sData['Profile_Age_Group'] = {"label":d, "choices":choices};
		}
		
		// Profile Living Location
		else if(r.indexOf('[Profile Living Location]') >= 0) {
			var d = r.substr('[Profile Living Location]'.length).trim()
		 	sData['Profile_Living_Location_Label'] = d;
		}
		
		// Profile Location Code
		else if(r.indexOf('[Profile Location Code]') >= 0) {
			var d = r.substr('[Profile Location Code]'.length).trim()
		 	sData['Profile_Location_Code_Label'] = d;
		}
		
		// Profile Email
		else if(r.indexOf('[Profile Email]') >= 0) {
			var d = r.substr('[Profile Email]'.length).trim()
		 	sData['Profile_Email_Label'] = d;
		}
		
		// Profile Phone
		else if(r.indexOf('[Profile Phone]') >= 0) {
			var d = r.substr('[Profile Phone]'.length).trim()
		 	sData['Profile_Phone_Label'] = d;
		}
		
		// Ext ID
		else if(r.indexOf('[Profile Ext ID]') >= 0) {
			var d = r.substr('[Profile Ext ID]'.length).trim()
		 	sData['Ext_ID'] = d;
		}
		
		// Profile Extra Attribute (extAttributes)
		else if(r.indexOf('[Profile Extra Attribute]') >= 0) {
			sData['Profile_Extra_Attributes'] = typeof sData['Profile_Extra_Attributes'] === "object" ? sData['Profile_Extra_Attributes'] : [];
			var toks = r.substr('[Profile Extra Attribute]'.length).trim().split("=>");
			if(toks.length === 3) {
				var inputType = toks[0].trim();// text textarea password 
				var field = toks[1].trim(); 
				var label = toks[2].trim(); // 
				sData['Profile_Extra_Attributes'].push({"key":field, "label":label,"inputType": inputType});
			} 
			else if(toks.length === 2) {
				var field = toks[0].trim(); 
				var label = toks[1].trim(); // 
				sData['Profile_Extra_Attributes'].push({"key":field, "label":label,"inputType": "text"});
			}
		}
		return sData;
	}
	
	function processSurveyMetaDataTags(r, sData) {
		// Group
		if(r.indexOf('[Survey Group]') >= 0) {
			var toks = r.substr('[Survey Group]'.length).trim().split('=>');
			if(toks.length === 2){
				sData['Group'] = toks[0].trim();
				sData['Group_Label'] = toks[1].trim();
			}
		} 
		// Group Option
		else if(r.indexOf('[Survey Group Option]') >= 0) {
			sData['Group_Options'] = typeof sData['Group_Options'] === "object" ? sData['Group_Options'] : [];
			sData['Group_Options'].push(r.substr('[Survey Group Option]'.length).trim());
		}
		// Touchpoint Item to research (ecommerce website, people or place)
		else if(r.indexOf('[Survey Touchpoint Item]') >= 0) {
			var toks = r.substr('[Survey Touchpoint Item]'.length).trim().split('=>');
			if(toks.length === 2){
				sData['Touchpoint_Item_ID'] = toks[0].trim();
			 	sData['Touchpoint_Item'] = toks[1].trim();
			}
		}
		// Product Item
		else if(r.indexOf('[Survey Product Item]') >= 0) {
			var toks = r.substr('[Survey Product Item]'.length).trim().split('=>');
			if(toks.length === 2){
				sData['Product_Item_ID'] = toks[0].trim();
			 	sData['Product_Item'] = toks[1].trim();
			}
		}
		// Content Item
		else if(r.indexOf('[Survey Content Item]') >= 0) {
			var toks = r.substr('[Survey Content Item]'.length).trim().split('=>');
			if(toks.length === 2){
				sData['Content_Item_ID'] = toks[0].trim();
			 	sData['Content_Item'] = toks[1].trim();
			}
		}
		return sData;
	}

	function processDataSourceTags(r, sData) {
		// Survey Original Source Label
		if(r.indexOf('[Survey Original Source Label]') >= 0) {
			var str = r.substr('[Survey Original Source Label]'.length).trim();
			sData['Survey_Original_Source_Label'] = str;
		}
		// Survey Media Source Options
		else if(r.indexOf('[Survey Original Source Option]') >= 0) {
			sData['Survey_Original_Source_Options'] = typeof sData['Survey_Original_Source_Options'] === "object" ? sData['Survey_Original_Source_Options'] : [];
			sData['Survey_Original_Source_Options'].push(r.substr('[Survey Original Source Option]'.length).trim());
		}
		// Survey Media Source Label
		else if(r.indexOf('[Survey Media Source Label]') >= 0) {
			var str = r.substr('[Survey Media Source Label]'.length).trim();
			sData['Survey_Media_Source_Label'] = str;
		}
		// Survey Media Source Options
		else if(r.indexOf('[Survey Media Source Option]') >= 0) {
			sData['Survey_Media_Source_Options'] = typeof sData['Survey_Media_Source_Options'] === "object" ? sData['Survey_Media_Source_Options'] : [];
			sData['Survey_Media_Source_Options'].push(r.substr('[Survey Media Source Option]'.length).trim());
		}
		return sData;
	}	
	
	function processDecisionMakerTags(r, sData){
		// Decision Maker
		if(r.indexOf('[Decision Maker Label]') >= 0) {
			var str = r.substr('[Decision Maker Label]'.length).trim();
			sData['Decision_Maker_Label'] = str;
		} 
		
		// Decision Maker Options
		else if(r.indexOf('[Decision Maker Option]') >= 0) {
			sData['Decision_Maker_Options'] = typeof sData['Decision_Maker_Options'] === "object" ? sData['Decision_Maker_Options'] : [];
			sData['Decision_Maker_Options'].push(r.substr('[Decision Maker Option]'.length).trim());
		}
		return sData;
	}
	
	function processEvaluatedObjectTags(r, sData){
		// Evaluated Object
		if(r.indexOf('[Survey Evaluated Object]') >= 0) {
			sData['Evaluated_Object'] = r.substr('[Survey Evaluated Object]'.length).trim();
		}
		// Evaluated Object Object Item
		else if(r.indexOf('[Survey Evaluated Object Option]') >= 0) {
			sData['Evaluated_Object_Suggested_Items'] = typeof sData['Evaluated_Object_Suggested_Items'] === "object" ? sData['Evaluated_Object_Suggested_Items'] : [];
			var item = r.substr('[Survey Evaluated Object Option]'.length).trim();
			sData['Evaluated_Object_Suggested_Items'].push(item);
		}
		return sData;
	}
	
	function processEvaluatedPersonTags(r, sData){
		// Evaluated Person
		if(r.indexOf('[Survey Evaluated Person]') >= 0) {
			sData['Evaluated_Person'] = r.substr('[Survey Evaluated Person]'.length).trim();
		}
		// Evaluated Person Item
		else if(r.indexOf('[Survey Evaluated Person Option]') >= 0) {
			sData['Evaluated_Person_Suggested_Items'] = typeof sData['Evaluated_Person_Suggested_Items'] === "object" ? sData['Evaluated_Person_Suggested_Items'] : [];
			var item = r.substr('[Survey Evaluated Person Option]'.length).trim();
			sData['Evaluated_Person_Suggested_Items'].push(item);
		}
		return sData;
	}
	
	
	function processRatingQuestionTags(r, sData){
		// Survey Rating Question Guide
		 if(r.indexOf('[Survey Rating Question Guide]') >= 0) {
			var d = r.substr('[Survey Rating Question Guide]'.length).trim()
		 	sData['Rating_Question_Guide'] = d;
		}
		// Rating Question Group
		else if(r.indexOf('[Survey Rating Question Group]') >= 0) {
			sData['Rating_Question_List'] = typeof sData['Rating_Question_List'] === "object" ? sData['Rating_Question_List'] : {};
			cRatingQuestionGroup = r.substr('[Survey Rating Question Group]'.length).trim();
		 	sData['Rating_Question_List'][cRatingQuestionGroup] = [];
		}
		// Rating Question
		else if(r.indexOf('[Survey Rating Question]') >= 0) {
			if(typeof cRatingQuestionGroup === "string" && typeof sData['Rating_Question_List'][cRatingQuestionGroup] === "object"){
				var question = r.substr('[Survey Rating Question]'.length).trim();
				sData['Rating_Question_List'][cRatingQuestionGroup].push(question);	
			}
		}
		// Rating Choices for all questions
		else if(r.indexOf('[Survey Rating Choices]') >= 0) {
			sData['Rating_Choices'] = typeof sData['Rating_Choices'] === "object" ? sData['Rating_Choices'] : [];
			var toks = r.substr('[Survey Rating Choices]'.length).trim().split(';');
		 	if(toks.length >1){
		 		toks.forEach(function(e){
		 			if(typeof e === 'string'){
		 				sData['Rating_Choices'].push(e.trim());
		 			}
		 		})
		 	}
		}
		return sData; 
	}
	
	function processMultipleChoiceQuestionsTags(r, sData) {
		sData['Multiple_Choice_Question_List'] = typeof sData['Multiple_Choice_Question_List'] === "object" ? sData['Multiple_Choice_Question_List'] : {};
		// Multiple Choice Question
		if(r.indexOf('[Survey Multiple Choice Question]') >= 0) {
			var toks = r.substr('[Survey Multiple Choice Question]'.length).trim().split("=>");
			if(toks.length === 2){
				var key = stringToSlug(toks[0]);
				var label = (toks[1].trim());
				var obj = {"label":label, "choices" : []}
				sData['Multiple_Choice_Question_List'][key] = obj;
			}
		}
		// Multiple Choices for all questions
		else if(r.indexOf('[Survey Multiple Choice]') >= 0) {
			var toks = r.substr('[Survey Multiple Choice]'.length).trim().split("=>");
			if(toks.length === 2){
				var key = stringToSlug(toks[0]);
				var choice = (toks[1].trim());
				if( typeof sData['Multiple_Choice_Question_List'][key] === "object" ) {
					sData['Multiple_Choice_Question_List'][key]['choices'].push(choice);
				}
			}
		}
		return sData;
	}
	
	function processSingleChoiceQuestionsTags(r, sData) {
		sData['Single_Choice_Question_List'] = typeof sData['Single_Choice_Question_List'] === "object" ? sData['Single_Choice_Question_List'] : {};
		// Single Choice Question
		if(r.indexOf('[Survey Single Choice Question]') >= 0) {
			var toks = r.substr('[Survey Single Choice Question]'.length).trim().split("=>");
			if(toks.length === 2){
				var key = stringToSlug(toks[0]);
				var label = (toks[1].trim());
				var obj = {"label":label, "choices" : []}
				sData['Single_Choice_Question_List'][key] = obj;
			}
		}
		// Single Choice for question
		else if(r.indexOf('[Survey Single Choice]') >= 0) {
			var toks = r.substr('[Survey Single Choice]'.length).trim().split("=>");
			if(toks.length === 2){
				var key = stringToSlug(toks[0]);
				var choice = (toks[1].trim());
				if( typeof sData['Single_Choice_Question_List'][key] === "object" ) {
					sData['Single_Choice_Question_List'][key]['choices'].push(choice);
				}
			}
		}
		return sData;
	}
	
	function processExtraTextQuestionTags(r, sData) {
		// Survey Extra Question Guide
		if(r.indexOf('[Survey Extra Question Guide]') >= 0) {
			var d = r.substr('[Survey Extra Question Guide]'.length).trim()
		 	sData['Survey_Extra_Question_Guide'] = d;
		}
		// Survey Extra Text Question
		else if(r.indexOf('[Survey Extra Text Question]') >= 0) {
			sData['Survey_Extra_Text_Questions'] = typeof sData['Survey_Extra_Text_Questions'] === "object" ? sData['Survey_Extra_Text_Questions'] : [];
			
			var toks = r.substr('[Survey Extra Text Question]'.length).trim().split("=>");
			if(toks.length === 2){
				var key = stringToSlug(toks[0]);
				var label = toks[1].trim();
				sData['Survey_Extra_Text_Questions'].push({"key":key, "label":label});
			}
		}
		// Survey Comment
		else if(r.indexOf('[Survey Comment]') >= 0) {
			var d = r.substr('[Survey Comment]'.length).trim()
		 	sData['Comment_Label'] = d;
		}
		return sData;
	}
	return tplData;
}