package leotech.cdp.model.customer;

import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import com.google.gson.annotations.Expose;
import com.google.gson.internal.LinkedTreeMap;

import rfx.core.util.StringUtil;

/**
 * 
 * Data entity to store course for Education Service
 * 
 * @author Trieu Nguyen (tantrieuf31)
 * @since 2023
 *
 */
public class LearningCourse {

	@Expose
	protected String id = "";

	@Expose
	protected String school;

	@Expose
	protected String journeyMapId;

	@Expose
	protected Set<String> requiredCourseIds = new HashSet<String>();

	@Expose
	protected String name;

	@Expose
	protected Set<String> categories = new HashSet<String>();

	@Expose
	protected String url = "";

	@Expose
	protected String certification = "";

	@Expose
	protected Set<String> keywords = new HashSet<String>();

	@Expose
	protected Set<String> instructors = new HashSet<String>();

	@Expose
	protected String description;

	@Expose
	protected float tuitionFee = 0;

	@Expose
	protected Date registeredDate;

	@Expose
	protected Date finishedDate;

	@Expose
	protected float gradeScore = -100;

	public LearningCourse() {
		// json
	}
	
	// Copy constructor
    public LearningCourse(LearningCourse other) {
        this.id = other.id;
        this.school = other.school;
        this.journeyMapId = other.journeyMapId;
        this.requiredCourseIds = new HashSet<>(other.requiredCourseIds); // Deep copy
        this.name = other.name;
        this.categories = new HashSet<>(other.categories); // Deep copy
        this.url = other.url;
        this.certification = other.certification;
        this.keywords = new HashSet<>(other.keywords); // Deep copy
        this.instructors = new HashSet<>(other.instructors); // Deep copy
        this.description = other.description;
        this.tuitionFee = other.tuitionFee;
        this.registeredDate = other.registeredDate != null ? new Date(other.registeredDate.getTime()) : null; // Deep copy
        this.finishedDate = other.finishedDate != null ? new Date(other.finishedDate.getTime()) : null; // Deep copy
        this.gradeScore = other.gradeScore;
    }
    
    public LearningCourse(LinkedTreeMap<String, Object> other) {
        this.id = StringUtil.safeString(other.get("id"), "");
        this.school = StringUtil.safeString(other.get("school"), "");
        this.journeyMapId = StringUtil.safeString(other.get("journeyMapId"), "");
        this.name = StringUtil.safeString(other.get("name"), "");
        this.url = StringUtil.safeString(other.get("url"), "");
        this.certification = StringUtil.safeString(other.get("certification"), "");
        this.description = StringUtil.safeString(other.get("description"), "");
        this.tuitionFee = StringUtil.safeParseFloat(other.get("tuitionFee"), 0F);
        this.registeredDate = new Date();
    }

	public LearningCourse(String id, String name) {
		super();
		this.id = id;
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Set<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(Set<String> keywords) {
		this.keywords = keywords;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public float getTuitionFee() {
		return tuitionFee;
	}

	public void setTuitionFee(float tuitionFee) {
		this.tuitionFee = tuitionFee;
	}

	public Date getRegisteredDate() {
		return registeredDate;
	}

	public void setRegisteredDate(Date registeredDate) {
		this.registeredDate = registeredDate;
	}

	public Date getFinishedDate() {
		return finishedDate;
	}

	public void setFinishedDate(Date finishedDate) {
		this.finishedDate = finishedDate;
	}

	public String getSchool() {
		return school;
	}

	public void setSchool(String school) {
		this.school = school;
	}

	public String getJourneyMapId() {
		return journeyMapId;
	}

	public void setJourneyMapId(String journeyMapId) {
		this.journeyMapId = journeyMapId;
	}

	public Set<String> getRequiredCourseIds() {
		return requiredCourseIds;
	}

	public void setRequiredCourseIds(Set<String> requiredCourseIds) {
		this.requiredCourseIds = requiredCourseIds;
	}

	public String getCertification() {
		return certification;
	}

	public void setCertification(String certification) {
		this.certification = certification;
	}

	public Set<String> getInstructors() {
		return instructors;
	}

	public void setInstructors(Set<String> instructors) {
		this.instructors = instructors;
	}

	public Set<String> getCategories() {
		return categories;
	}

	public void setCategories(Set<String> categories) {
		this.categories = categories;
	}

	public float getGradeScore() {
		return gradeScore;
	}

	public void setGradeScore(float gradeScore) {
		this.gradeScore = gradeScore;
	}

	@Override
	public boolean equals(Object obj) {
		return this.hashCode() == obj.hashCode();
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, name, url);
	}

}
