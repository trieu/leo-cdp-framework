package leotech.cdp.model.customer;

import java.util.Set;

import com.google.gson.Gson;

import leotech.cdp.model.journey.Touchpoint;
import rfx.core.util.StringUtil;

public final class ProfileUpdateData {

	String profileId;
	String loginId; 
	String loginProvider;
	
	String firstName;
	String lastName;
	String email;
	String phone;
	String genderStr;
	int age;
	String dateOfBirth;
	String observerId;
	Touchpoint srcTouchpoint;
	String lastSeenIp;
	String usedDeviceId;
	Set<String> contentKeywords;
	Set<String> productKeywords;
	String livingLocation;
	
	String workingHistory;
	String jobTitles;
	String personalProblems;
	
	Set<LearningCourse> learningCourses;
	
	public ProfileUpdateData() {
		
	}
	
	public ProfileUpdateData(String profileId, String loginId, String loginProvider, String firstName,
			String lastName, String email, String phone, String genderStr, int age, String dateOfBirth, String observerId,
			Touchpoint srcTouchpoint, String lastSeenIp, String usedDeviceId, Set<String> contentKeywords, Set<String> productKeywords, 
			String livingLocation, String workingHistory, String jobTitles, String personalProblems, Set<LearningCourse> learningCourses) {
		super();
		this.profileId = profileId;
		this.loginId = loginId;
		this.loginProvider = loginProvider;
		this.firstName = firstName;
		this.lastName = lastName;
		this.email = email;
		this.phone = phone;
		this.genderStr = genderStr;
		this.age = age;
		this.dateOfBirth = dateOfBirth;
		this.observerId = observerId;
		this.srcTouchpoint = srcTouchpoint;
		this.lastSeenIp = lastSeenIp;
		this.usedDeviceId = usedDeviceId;
		this.contentKeywords = contentKeywords;
		this.productKeywords = productKeywords;
		this.livingLocation = livingLocation;
		this.workingHistory = workingHistory;
		this.jobTitles = jobTitles;
		this.personalProblems = personalProblems;
		this.learningCourses = learningCourses;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public String getLoginId() {
		return loginId;
	}

	public void setLoginId(String loginId) {
		this.loginId = loginId;
	}

	public String getLoginProvider() {
		return loginProvider;
	}

	public void setLoginProvider(String loginProvider) {
		this.loginProvider = loginProvider;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getGenderStr() {
		return genderStr;
	}

	public void setGenderStr(String genderStr) {
		this.genderStr = genderStr;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public String getObserverId() {
		return observerId;
	}

	public void setObserverId(String observerId) {
		this.observerId = observerId;
	}

	public Touchpoint getSrcTouchpoint() {
		return srcTouchpoint;
	}

	public void setSrcTouchpoint(Touchpoint srcTouchpoint) {
		this.srcTouchpoint = srcTouchpoint;
	}

	public String getLastSeenIp() {
		return lastSeenIp;
	}

	public void setLastSeenIp(String lastSeenIp) {
		this.lastSeenIp = lastSeenIp;
	}

	public String getUsedDeviceId() {
		return usedDeviceId;
	}

	public void setUsedDeviceId(String usedDeviceId) {
		this.usedDeviceId = usedDeviceId;
	}

	public Set<String> getContentKeywords() {
		return contentKeywords;
	}

	public void setContentKeywords(Set<String> contentKeywords) {
		this.contentKeywords = contentKeywords;
	}
	
	public String getWorkingHistory() {
		return workingHistory;
	}

	public void setWorkingHistory(String workingHistory) {
		this.workingHistory = workingHistory;
	}

	public Set<String> getProductKeywords() {
		return productKeywords;
	}

	public void setProductKeywords(Set<String> productKeywords) {
		this.productKeywords = productKeywords;
	}

	public String getLivingLocation() {
		return livingLocation;
	}

	public void setLivingLocation(String livingLocation) {
		this.livingLocation = livingLocation;
	}
	
	

	public String getDateOfBirth() {
		return dateOfBirth;
	}

	public void setDateOfBirth(String dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	public Set<LearningCourse> getLearningCourses() {
		return learningCourses;
	}

	public void setLearningCourses(Set<LearningCourse> learningCourses) {
		this.learningCourses = learningCourses;
	}

	public String getJobTitles() {
		return jobTitles;
	}

	public void setJobTitles(String jobTitles) {
		this.jobTitles = jobTitles;
	}

	public String getPersonalProblems() {
		return personalProblems;
	}

	public void setPersonalProblems(String personalProblems) {
		this.personalProblems = personalProblems;
	}

	@Override
	public String toString() {
		return new Gson().toJson(this);
	}

	public boolean isUpdateContact() {
		return (StringUtil.isNotEmpty(this.email) || StringUtil.isNotEmpty(this.phone)) 
				&& (StringUtil.isNotEmpty(this.firstName) || StringUtil.isNotEmpty(this.lastName)) ;
	}
	
}
