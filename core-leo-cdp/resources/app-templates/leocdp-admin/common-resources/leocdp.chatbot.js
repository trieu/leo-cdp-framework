// LEOCHAT BOT configs
const USING_LEO_BOT_WITH_AI = dnsDomainLeoBot !== "";
const LEOBOT_URL_ASK = baseLeoBotUrl + '/ask';
const LEOBOT_URL_GET_INFO = baseLeoBotUrl + '/get-visitor-info';

var leoBotActionList = [];
leoBotActionList.push({ text: 'Search profile', value: 'Search profile', icon: 'search' });
if (USING_LEO_BOT_WITH_AI) {
	leoBotActionList.push({ text: 'Ask question', value: 'Ask question', icon: 'question-circle' });
	leoBotActionList.push({ text: 'Create content', value: 'Create content', icon: 'file-word-o' });
	leoBotActionList.push({ text: 'Create slides', value: 'Create slides', icon: 'file-powerpoint-o' });
}

leoBotActionList.push({ text: 'Create short URL', value: 'Create short URL', icon: 'link' });
leoBotActionList.push({ text: 'Help me', value: 'Help me', icon: 'info-circle' });

window.leoBotUI = false;
function getBotUI() {
	if (window.leoBotUI === false) {
		window.leoBotUI = new BotUI('LEO_ChatBot_Container');
	}
	return window.leoBotUI;
}

var notifyLeoBotOk = function() {
	var bt = '<b> The AI ChatBot </b>';
	notifySuccessMessage(bt + ' is ready !')
	$('#menu_ai_chatbot').show();
}

function checkLeoBotIsServerReady(profileVisitorId, visitorName = null, initTouchpointId = null) {
	if (!USING_LEO_BOT_WITH_AI) return;

	// Build query params dynamically
	const params = new URLSearchParams({
		visitor_id: profileVisitorId,
		_: Date.now()
	});

	if (visitorName) params.append("name", visitorName);
	if (initTouchpointId) params.append("touchpoint_id", initTouchpointId);

	const url = `${LEOBOT_URL_GET_INFO}?${params.toString()}`;

	$.getJSON(url, function (data) {
		const errorCode = data.error_code;
	
		if (errorCode === 0) {
			notifyLeoBotOk();
		} else {
			updateVisitorProfileAndActivateLeoBot(profileVisitorId);
		}
	});
}

function updateVisitorProfileAndActivateLeoBot(visitorId) {
	if(currentUserProfile.status === 1) {
		var urlStr = baseLeoAdminUrl + '/user/activate-leobot';
		LeoAdminApiUtil.callPostAdminApi(urlStr, { 'visitorId': visitorId }, function(json) {
			if (json.data) {
				notifyLeoBotOk()
			} else {
				LeoAdminApiUtil.logErrorPayload(json);
			}
		});
	}
}

function initLeoChatBot(context) {
	$('#leoChatBotDialog').modal({ backdrop: 'static', keyboard: false });
	getBotUI().action.hide();
	getBotUI().message.removeAll();
	$('#leobot_answer_in_language').show()

	LeoObserverProxy.synchLeoVisitorId(function(vid) {
		if (window.currentUserProfile) {
			window.currentUserProfile.profileVisitorId = vid;
			leoBotSayHello();
		}
	});
}

var closeLeoChatBotDialog = function() {
	getBotUI().action.hide();
	getBotUI().message.removeAll().then(function() {
		$('#leoChatBotDialog').modal('hide')
	})
}

var leoBotDoAction = function(res) {
	var v = res.value;
	var content = "<b>" + v + "</b>";
	getBotUI().message.add({
		human: true, content: content, type: 'html'
	}).then(function() {
		if (v === 'Create short URL') {
			showChatBotLoader().then(function(index) {
				leoBotCreateShortUrl(function() {
					getBotUI().message.remove(index);
				});
			});
		}
		else if (v === 'Help me') {
			leoBotHelpUser();
		}
		else if (v === 'Search profile') {
			leoBotSearchProfiles();
		}
		else if (v === 'Create content') {
			leoBotCreateContent();
		}
		else if (v === 'Create slides') {
			leoBotCreateSlides();
		}
		else {
			leoBotPromptQuestion();
		}
	})
}

var leoBotSayHello = function() {
	var msg = 'Hello ' + currentUserProfile.displayName + ', how can I help you ?';
	getBotUI().message.bot(msg).then(function() {
		return getBotUI().action.button({
			delay: 1000,
			addMessage: false,
			action: leoBotActionList
		})
	}).then(function(res) {
		leoBotDoAction(res)
	});
}

var leoBotPromptQuestion = function(delay) {
	getBotUI().action.text({
		delay: typeof delay === 'number' ? delay : 800,
		action: {
			icon: 'question-circle',
			cssClass: 'leobot-question-input',
			value: '', // show the prevous answer if any
			placeholder: 'Give me a question'
		}
	}).then(function(res) {
		var question = res.value;
		leoBotAskServer('ask', question, question);
	});
}

var leoBotCreateContent = function() {
	
	var handleCreateContent = function(selectedOption) {
		getBotUI().message.bot(selectedOption.text)
		var v = selectedOption.value;
		if(v === 'content-as-asset') {
			var askContentPurpose = function(title) {
				getBotUI().action.text({
					delay: 500,
					action: {
						icon: 'question-circle',
						cssClass: 'leobot-question-input',
						value: '', // show the prevous answer if any
						placeholder: 'Give me the purpose of content'
					}
				}).then(function(res) {
					var prompt = 'Write a blog post about ' + title;
					var extraInfo = ', the purpose of blog post is ' + res.value
					leoBotAskServer('content', title, prompt, extraInfo);
				});
			};
			getBotUI().action.text({
				delay: 500,
				action: {
					icon: 'question-circle',
					cssClass: 'leobot-question-input',
					value: '', // show the prevous answer if any
					placeholder: 'Give me the title of content'
				}
			}).then(function(res) {
				askContentPurpose(res.value)
			});
		} else if(v === 'content-for-segment') {
			getBotUI().action.select({
			  action: {
			      placeholder : "Select Language", 
			      value: 'TR', // Selected value or selected object. Example: {value: "TR", text : "Türkçe" }
			      searchselect : true, // Default: true, false for standart dropdown
			      label : 'text', // dropdown label variable
			      options : [
		                      {value: "EN", text : "English" },
		                      {value: "ES", text : "Español" },
		                      {value: "TR", text : "Türkçe" },
		                      {value: "DE", text : "Deutsch" },
		                      {value: "FR", text : "Français" },
		                      {value: "IT", text : "Italiano" },
			                ],
			      button: {
			        icon: 'check',
			        label: 'OK'
			      }
			    }
			}).then(function (res) { // will be called when a button is clicked.
			  console.log(res.value); // will print "one" from 'value'
			});
		}
		
	}
	
	var actionList = [];
	actionList.push({ text: 'Create content as digital asset', value: 'content-as-asset', icon: 'file-word-o' });
	actionList.push({ text: 'Create content for all profiles in a segment', value: 'content-for-segment', icon: 'file-word-o' });
	var msg = 'Please select an option';
	getBotUI().message.bot(msg).then(function() {
		return getBotUI().action.button({
			delay: 100,
			addMessage: false,
			action: actionList
		})
	}).then(function(res) {
		handleCreateContent(res)
	});
	
}

var leoBotCreateSlides = function() {
	var askNumberOfSlides = function(question) {
		getBotUI().action.text({
			delay: 500,
			action: {
				icon: 'question-circle',
				cssClass: 'leobot-question-input',
				value: '', // show the prevous answer if any
				placeholder: 'Give me the number of slides in presentation'
			}
		}).then(function(res) {
			var numberOfSlides = parseInt(res.value);
			if (isNaN(numberOfSlides)) {
				leoBotShowInfo(res.value + ' is not a number !', function() {
					askNumberOfSlides(question);
				});
			}
			else {
				var prompt = 'Create a presentation slide, just text and no images, with the title is "' + question + '" ';
				var extraInfo = ' in ' + numberOfSlides + ' slides.';
				extraInfo += ' Every slide must begin with the prefix "## " ';
				leoBotAskServer('presentation', question, prompt, extraInfo, 'markdown');
			}
		});
	};
	getBotUI().action.text({
		delay: 500,
		action: {
			icon: 'question-circle',
			cssClass: 'leobot-question-input',
			value: '', // show the prevous answer if any
			placeholder: 'Give me a title of presentation slide'
		}
	}).then(function(res) {
		var question = res.value;
		askNumberOfSlides(question)
	});
}

var leoBotHelpUser = function() {
	// TODO improve this
	var template = Handlebars.compile($('#tpl_leocdp_documents').html());
	var html = template({});
	if (USING_LEO_BOT_WITH_AI) {
		leoBotShowAnswer('html', html, leoBotPromptQuestion)
	}
	else {
		leoBotShowAnswer('html', html)
	}
}

var leoBotSearchProfiles = function() {
	getBotUI().action.text({
		delay: 500,
		action: {
			icon: 'question-circle',
			cssClass: 'leobot-question-input',
			placeholder: 'Give me a name, email address or phone number of customer'
		}
	}).then(function(res) {
		var encodedString = Base64.encode(res.value);
		location.hash = "calljs-leoCdpRouter('Customer_Profile_Search','" + encodedString + "')";
		closeLeoChatBotDialog();
	});
}

var leoBotCreateShortUrl = function(callback) {
	var paramObj = { 'assetType': "15" };
	var urlStr = window.baseLeoAdminUrl + "/cdp/asset-group/list-by-asset-type";
	LeoAdminApiUtil.getSecuredData(urlStr, paramObj, function(json) {
		callback();
		if (typeof json.data[0] === 'object') {
			var assetGroup = json.data[0];
			var params = leoCdpRouterParams(["", assetGroup.id, assetGroup.categoryIds[0]]);
			location.hash = "calljs-leoCdpRouter('Asset_Short_Link_Editor','" + params + "')";
			closeLeoChatBotDialog();
		} else {
			LeoAdminApiUtil.logErrorPayload(json);
		}
	});
}

var leoBotCreateNewItem = function(context, title, content) {
	var callbackNewItem = function() {
		$('#asset_item_title').val(title).focus();
		var trix = window.trixElement;
		if (typeof trix === 'object' && context === 'content') {
			trix.editor.loadHTML(content);
		}
		else if (context === 'presentation' && typeof formatMarkdownToPresentation === 'function') {
			formatMarkdownToPresentation(content);
		}
	}
	var urlStr = baseLeoAdminUrl + '/cdp/asset-group/get-default';
	var params = { "context": context };
	LeoAdminApiUtil.getSecuredData(urlStr, params, function(json) {
		if (json.httpCode === 0 && json.errorMessage === '') {
			var group = json.data;
			newAssetItemEditor(group.assetType, group.id, group.categoryIds[0]);
			setTimeout(callbackNewItem, 2000)
		} else {
			LeoAdminApiUtil.logErrorPayload(json);
		}
	});
}


var leoBotShowAnswer = function(in_format, answer, callback) {
	var html = '', cssClass = 'leobot-answer';
	if(in_format === 'markdown'){
		html = '<textarea style="width: 100%; height: 220px;" readonly=""> ' + answer + "</textarea>";
		cssClass = 'leobot-answer-in-markdown';
	}
	else {
		html = answer;
	}
	console.log('leoBotShowAnswer', in_format)
	
	getBotUI().message.add({
		human: false,
		cssClass: cssClass,
		content: html,
		type: 'html'
	}).then(function() {
		// format all href nodes in answer
		$("div.botui-message").find("a").each(function() {
			$(this).attr("target", "_blank");
			var href = $(this).attr("href");
			if (href.indexOf('google.com') < 0) {
				href = 'https://www.google.com/search?q=' + encodeURIComponent($(this).text());
			}
			$(this).attr("href", href);
		});
		callback()
	});
}


var leoBotShowInfo = function(msg, callback) {
	getBotUI().message.add({
		human: false,
		cssClass: 'chatbot-info',
		content: msg
	}).then(function() {
		callback()
	});
}


var leoBotAskServer = function(context, question, textPrompts, extraInfo, in_format) {
	// add extraInfo into textPrompts
	if (typeof extraInfo === 'string') {
		textPrompts += extraInfo;
	}
	var answer_in_format = (typeof in_format === 'string') ? in_format : "html";
	var temperature_score = parseFloat($('#leobot_creativity_score').val());

	if (question.length > 1 && question !== "exit") {
		var processAnswer = function(answer) {
			if ('ask' === context) {
				leoBotShowAnswer(answer_in_format, answer, function() {
					var delay = answer.length > 120 ? 6000 : 2000;
					leoBotPromptQuestion(delay);
				});
			}
			else {
				var callback = function() {
					var reply = 'Would you like to write a blog post?';
					if ('presentation' === context) {
						reply = 'Would you like to create a presentation slide ?';
					}
					getBotUI().message.bot(reply).then(function() {
						return getBotUI().action.button({
							delay: 1000,
							action: [{
								text: 'Yes',
								value: 'yes'
							}, {
								text: 'No',
								value: 'no'
							}]
						})
					}).then(function(res) {
						if (res.value === 'yes') {
							getBotUI().message.bot('Okay').then(function() {
								closeLeoChatBotDialog();
								leoBotCreateNewItem(context, question, answer);
							});
						} else {
							closeLeoChatBotDialog();
						}
					});
				}
				leoBotShowAnswer(answer_in_format, answer, callback);
			}
		}

		var callServer = function(index) {
			var callback = function(data) {
				getBotUI().message.remove(index);
				if (typeof data.answer === 'string') {
					processAnswer(data.answer)
				}
				else if (data.error) {
					notifyErrorMessage(data.answer)
					closeLeoChatBotDialog();
				}
				else {
					notifyErrorMessage('LEO BOT is getting a system error !')
				}
			};
			var payload = { 'prompt': textPrompts, 'question': question, 'context' : 'agent' };
			payload["visitor_id"] = currentUserProfile.profileVisitorId;
			payload['answer_in_format'] = answer_in_format;
			payload['temperature_score'] = temperature_score;
			payload['answer_in_language'] = $('#leobot_answer_in_language').val();
			LeoAdminApiUtil.callPostApi(LEOBOT_URL_ASK, payload, callback, closeLeoChatBotDialog);
		}
		showChatBotLoader().then(callServer);
	}
	else {
		closeLeoChatBotDialog();
	}
}

var showChatBotLoader = function() {
	return getBotUI().message.add({ loading: true, content: '' });
}

var recommendContentIdeas = function(textPrompts) {
	var source = document.getElementById("suggestedVideoTpl").innerHTML;
	var template = Handlebars.compile(source);
	var context = {
		title: "My New Post",
		body: "This is my first post!"
	};
	var html = template(context);
	getBotUI().message.bot({ delay: 500, content: html });

	setTimeout(function() {
		$("#suggestedVideoCarousel").on('slid.bs.carousel', function() {
			var videoInfo = getActiveVideoInfo();
			console.log(videoInfo);
			$('#playlist_holder').empty();
			leoPlayVideoReport(videoInfo.video);
		});
	}, 500)

	//leoPlayVideoReport('https://www.youtube.com/watch?v=3HGU9TBdWs8');
	//leoPlayVideoReport('https://www.facebook.com/mauren.murillo.3/videos/10217089066046077/');
}

function getRandomInt(min, max) {
	min = Math.ceil(min);
	max = Math.floor(max);
	return Math.floor(Math.random() * (max - min + 1)) + min;
}

function leoPlayVideoReport(videoSource) {
	var placeHolderId = getRandomInt(1, 100000) + '_' + (new Date().getTime());
	var div = document.createElement('div');
	div.setAttribute('id', placeHolderId);
	div.setAttribute('class', 'videoholder');
	document.getElementById('playlist_holder').appendChild(div);

	var adUrls = [];
	var defaultSkipAdTime = 3; // show skip button after 3 seconds
	var autoplay = true;

	var adConfigs = [];
	MediaPlayerOne.create(autoplay, placeHolderId, videoSource, '', adUrls, defaultSkipAdTime, adConfigs,
		function(player) {
			setTimeout(function() { player.muted(false); }, 1000)
		}
	);
}

function getActiveVideoInfo() {
	var node = $('#suggestedVideoCarousel .active');
	var title = node.data('title');
	var video = node.data('video');
	return { 'title': title, 'video': video };
}