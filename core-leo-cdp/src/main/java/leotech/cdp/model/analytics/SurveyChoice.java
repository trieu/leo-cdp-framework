package leotech.cdp.model.analytics;

import java.util.Objects;

import com.google.gson.Gson;

public class SurveyChoice implements Comparable<SurveyChoice> {

	String answer;
	int answerScore = 0;
	int responseCount;

	public SurveyChoice(String answer, int answerScore, int responseCount) {
		super();
		this.answer = answer;
		this.answerScore = answerScore;
		this.responseCount = responseCount;
	}

	public String getAnswer() {
		return answer;
	}

	public void setAnswer(String answer) {
		this.answer = answer;
	}

	public int getAnswerScore() {
		return answerScore;
	}

	public void setAnswerScore(int answerScore) {
		this.answerScore = answerScore;
	}

	public int getResponseCount() {
		return responseCount;
	}

	public void setResponseCount(int responseCount) {
		this.responseCount = responseCount;
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(this.answer);
	}
	
	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	@Override
	public int compareTo(SurveyChoice o) {
		if (this.answerScore > o.answerScore) {
			return 1;
		} else if (this.answerScore < o.answerScore) {
			return -1;
		}
		return 0;
	}
}
