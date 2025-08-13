package leotech.cdp.model.customer;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.annotations.Expose;

import rfx.core.util.StringUtil;

/**
 * the data result, for LEO AI use to Predicts your personality out of the 16 Myers-Briggs Type Personalities 
 *  by your social media (Facebook, Twitter, Tiktok, YouTube, LinkedIn) data and compares your personality types with the people that you follow. <br>
 * https://github.com/USPA-Technology/Social-BERTerfly
 * 
 * @author tantrieuf31
 * @since 2022
 */
public class PersonalityType {

	@Expose
	String type = ""; // E.g: ISFJ
	
	@Expose
	Set<String> traits = new HashSet<>(); // E.g: ["Warm", "Detailed", "Caring", "Practical"]
	
	@Expose
	Set<String> careers = new HashSet<>(); // E.g: ["Counselor”, “Human Resource”, “Manager”]
	
	@Expose
	Set<String> eminentPersonalities = new HashSet<>(); // E.g: ["Jimmy Carter”, “Mother Teresa”, “Pope Francis"]
	
	
	public PersonalityType() {
		// default
	}
	
	public PersonalityType(String type) {
		super();
		this.type = type;
	}

	public PersonalityType(String type, Set<String> traits, Set<String> careers, Set<String> eminentPersonalities) {
		super();
		this.type = type;
		this.traits = traits;
		this.careers = careers;
		this.eminentPersonalities = eminentPersonalities;
	}


	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Set<String> getTraits() {
		return traits;
	}

	public void setTraits(Set<String> traits) {
		this.traits = traits;
	}
	
	public void setTraits(String trait) {
		this.traits.add(trait);
	}

	public Set<String> getCareers() {
		return careers;
	}

	public void setCareers(Set<String> careers) {
		this.careers = careers;
	}
	
	public void setCareers(String career) {
		this.careers.add(career);
	}

	public Set<String> getEminentPersonalities() {
		return eminentPersonalities;
	}

	public void setEminentPersonalities(Set<String> eminentPersonalities) {
		this.eminentPersonalities = eminentPersonalities;
	}
	
	public void setEminentPersonalities(String eminentPersonality) {
		this.eminentPersonalities.add(eminentPersonality);
	}
	
	@Override
	public int hashCode() {
		return StringUtil.safeString(this.type).hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		return this.hashCode() == obj.hashCode();
	}
}
