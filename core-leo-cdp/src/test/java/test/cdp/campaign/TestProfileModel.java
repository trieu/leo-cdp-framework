package test.cdp.campaign;

import java.util.Date;
import java.util.List;
import java.util.Set;

import com.arangodb.ArangoCollection;

import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.asset.AssetTemplate;
import leotech.cdp.model.asset.ProductItem;
import leotech.cdp.model.customer.Profile;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class TestProfileModel extends Profile {
	String birthday = "", name = "";

	public TestProfileModel() {

	}

	public TestProfileModel(Set<String> behavioralEvents, Date dateOfBirth, String name) {
		super();
		this.behavioralEvents = behavioralEvents;
		this.dateOfBirth = dateOfBirth;
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public Set<String> getBehavioralEvents() {
		return behavioralEvents;
	}

	@Override
	public void setBehavioralEvents(Set<String> behavioralEvents) {
		this.behavioralEvents = behavioralEvents;
	}

	public void sendEmail(String templateId) {
		System.out.println("Sending email with template: " + templateId + toString());
	}

	public void sendZalo(String templateId) {
		System.out.println("Sending Zalo message with template: " + templateId + toString());
	}

	public void sendSMS(String templateId) {
		System.out.println("Sending SMS with template: " + templateId + toString());
	}

	public void waitFor(int duration, String unit, String date) {
		System.out.println("Waiting for " + duration + " " + unit + " until " + date + toString());
	}

	public void runPersonalizationJob(String assetGroupId) {
		System.out.println("Running personalization job assetGroupId " + assetGroupId + toString());
	}

	@Override
	public String toString() {
		return " [profile " + name + "] ";
	}

	@Override
	public ArangoCollection getDbCollection() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean dataValidation() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean recommendProductItems(AssetTemplate assetTemplate, List<ProductItem> productItems) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean recommendContentItems(AssetTemplate assetTemplate, List<AssetContent> contentItems) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDocumentUUID() {
		// TODO Auto-generated method stub
		return null;
	}

}
