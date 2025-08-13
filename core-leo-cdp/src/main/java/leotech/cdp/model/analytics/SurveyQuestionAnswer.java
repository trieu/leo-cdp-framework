package leotech.cdp.model.analytics;

import com.google.gson.Gson;

import leotech.system.util.IdGenerator;
import rfx.core.util.StringUtil;

/**
 * the container of Question and the answer from user
 * 
 * @author tantrieuf31
 *
 */
public final class SurveyQuestionAnswer implements Comparable<SurveyQuestionAnswer> {

	int index;
	String id = "";
	String questionKey;
	
	String questionGroup;
	String question;
	
	String answer;
	int answerScore = 0;
	int responseCount = 1;

	public SurveyQuestionAnswer(int index, String questionGroup, String question, String selected) {
		super();
		this.index = index;
		this.questionGroup = questionGroup.trim();
		this.question = question.trim();
		this.answer = selected.trim();
		
		// e.g: [5] Strongly Agree
		int startingIndex = answer.indexOf("[");
		int closingIndex = answer.indexOf("]");
		if(startingIndex >=0 && closingIndex > 0) {
			this.answerScore = StringUtil.safeParseInt(answer.substring(startingIndex + 1, closingIndex));
		}
		else {
			this.answerScore = selected.hashCode();
		}

		String keyHint = this.questionGroup + this.question + answerScore;
		this.id = IdGenerator.createHashedId(keyHint);
		this.questionKey = IdGenerator.createHashedId(this.questionGroup + this.question);
	}

	public String getId() {
		return id;
	}
	
	public int getIndex() {
		return index;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getQuestionGroup() {
		return questionGroup;
	}

	public void setQuestionGroup(String questionGroup) {
		this.questionGroup = questionGroup;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}
	
	public int getAnswerScore() {
		return answerScore;
	}

	public void setAnswerScore(int answerScore) {
		this.answerScore = answerScore;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}
	
	public String getQuestionKey() {
		return questionKey;
	}
	
	public int getResponseCount() {
		return responseCount;
	}
	
	public void updateResponseCount() {
		this.responseCount += 1;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}
	
	@Override
	public int hashCode() {
		return this.id.hashCode();
	}

	@Override
	public int compareTo(SurveyQuestionAnswer o) {
		if(this.index > o.index) {
			return 1;
		}
		else if(this.index < o.index) {
			return -1;
		}
		return 0;
	}

}
