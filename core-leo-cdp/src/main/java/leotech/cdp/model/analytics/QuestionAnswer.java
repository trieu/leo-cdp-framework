package leotech.cdp.model.analytics;

import java.util.List;

import com.google.gson.annotations.Expose;

public final class QuestionAnswer {
	
	@Expose
	String key;

	@Expose
	String question;

	@Expose
	List<String> answers;

	public QuestionAnswer() {
		// gson 
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getQuestion() {
		return question;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public List<String> getAnswers() {
		return answers;
	}

	public void setAnswers(List<String> answers) {
		this.answers = answers;
	}
}
