package leotech.cdp.model.analytics;

import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;

import com.google.gson.Gson;

/**
 * the final result of survey, for statistics
 * 
 * @author tantrieuf31
 *
 */
public final class SurveyResult implements Comparable<SurveyResult> {

	int index;
	String questionKey;
	String questionGroup;
	String question;

	SortedSet<SurveyChoice> answerResults = new TreeSet<SurveyChoice>();


	public SurveyResult(SurveyQuestionAnswer a, List<SurveyChoice> surveyChoices) {
		super();
		this.index = a.getIndex();
		this.questionKey = a.getQuestionKey();
		this.questionGroup = a.getQuestionGroup();
		this.question = a.getQuestion();
		
		for (SurveyChoice choice : surveyChoices) {
			answerResults.add(choice);
		}
		
		SurveyChoice answer = new SurveyChoice(a.getAnswer(), a.getAnswerScore(), a.getResponseCount());
		answerResults.remove(answer);
		answerResults.add(answer);
	}

	public String getQuestionKey() {
		return questionKey;
	}

	public void setQuestionKey(String questionKey) {
		this.questionKey = questionKey;
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

	public SortedSet<SurveyChoice> getAnswerResults() {
		return answerResults;
	}

	public void setAnswerResults(SortedSet<SurveyChoice> answerResults) {
		this.answerResults = answerResults;
	}

	public int getIndex() {
		return index;
	}

	public void setAnswerResults(SurveyQuestionAnswer a) {
		SurveyChoice answer = new SurveyChoice(a.getAnswer(), a.getAnswerScore(), a.getResponseCount());
		answerResults.remove(answer);
		answerResults.add(answer);
	}

	@Override
	public int hashCode() {
		if (this.questionKey != null) {
			return Objects.hash(this.questionKey);
		}
		return 0;
	}

	@Override
	public int compareTo(SurveyResult o) {
		if (this.index > o.index) {
			return 1;
		} else if (this.index < o.index) {
			return -1;
		}
		return 0;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

}
