package leotech.cdp.model.customer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoDatabase;
import com.arangodb.entity.Key;
import com.arangodb.model.FulltextIndexOptions;
import com.arangodb.model.PersistentIndexOptions;
import com.google.gson.annotations.Expose;

import leotech.cdp.model.AutoSetData;
import leotech.cdp.model.asset.BusinessAction;
import leotech.cdp.model.asset.BusinessContract;
import leotech.cdp.model.asset.BusinessDeal;
import leotech.system.util.database.PersistentObject;
import rfx.core.util.RandomUtil;
import rfx.core.util.StringUtil;

/**
 * 
 * Business Account, the data entity is used to store any information of a company or an organization
 * 
 * @author Trieu Nguyen (Thomas)
 * @since 2022
 *
 */
public class BusinessAccount extends PersistentObject {

	public static final String COLLECTION_NAME = getCdpCollectionName(BusinessAccount.class);
	static ArangoCollection dbCol;

	@Key
	@Expose
	protected String id;
	
	@Expose
	protected String name;
	
	@Expose
	protected String description;
	
	@Expose
	protected int status = STATUS_ACTIVE;
	
	/**
	 * who created this segment 
	 */
	@Expose
	String ownerUsername = "";
	
	/**
	 * A list of login users who can update data, default only admin and super admin can update data
	 */
	@Expose
	protected List<String> authorizedEditors = new ArrayList<String>();
	
	/**
	 * A list of login users who can view data, default only admin and super admin can view data
	 */
	@Expose
	protected List<String> authorizedViewers = new ArrayList<String>();
	
	@Expose
	protected Date createdAt = new Date();

	@Expose
	protected Date updatedAt = new Date();

	@Expose
	protected String parentId = ""; // the parent ID of this account (e.g Alphabet has YouTube, Google)
	
	@Expose
	protected String businessId = ""; // manage by government

	@Expose
	protected String businessTaxId = ""; // manage by government

	@Expose
	protected String businessType = "";// unknown

	@Expose
	protected Set<String> keywords = new HashSet<String>();

	@Expose
	protected String primaryLogo;

	@Expose
	protected String primaryWebsite;

	@Expose
	protected String primaryEmail = "";

	@Expose
	protected String primaryPhone = "";

	@Expose
	protected String locationAddress = "";

	@Expose
	protected String locationCity = "";

	@Expose
	protected String locationCode = "";

	@Expose
	protected String locationZipCode = "";

	@Expose
	protected String originCountry = "";
	
	@Expose
	protected String stockCode = "";

	@Expose
	protected Map<String, String> businessEmails = new HashMap<>(20);

	@Expose
	protected Map<String, String> businessPhones = new HashMap<>(20);

	@Expose
	protected Map<String, String> businessIds = new HashMap<>(20);

	// --- BEGIN Marketing B2B , inputed by Marketer

	@AutoSetData(setDataAsString = true)
	protected Set<String> businessIndustries = new HashSet<>(50);
	
	@Expose
	protected Map<String, Integer> profileScoreMap = new HashMap<>(100);
	
	// --- END Marketing B2B Data Model

	@Expose
	protected Set<BusinessAction> businessActions = new HashSet<>(1000);

	@Expose
	protected Set<BusinessDeal> businessDeals = new HashSet<>(1000);

	@Expose
	protected Set<BusinessContract> signedContracts = new HashSet<>();

	/**
	 * total profile in this account
	 */
	@Expose
	protected long totalProfile = 0;

	@Override
	public ArangoCollection getDbCollection() {
		if (dbCol == null) {
			ArangoDatabase arangoDatabase = getArangoDatabase();
			dbCol = arangoDatabase.collection(COLLECTION_NAME);
			
			dbCol.ensurePersistentIndex(Arrays.asList("primaryWebsite"),new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("primaryEmail"),new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("primaryPhone"),new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("locationCode"),new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("stockCode"),new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("businessId"),new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("businessTaxId"),new PersistentIndexOptions().unique(false));
			
			dbCol.ensureFulltextIndex(Arrays.asList("name"), new FulltextIndexOptions().minLength(2));
			dbCol.ensureFulltextIndex(Arrays.asList("locationAddress"), new FulltextIndexOptions().minLength(2));
			dbCol.ensureFulltextIndex(Arrays.asList("locationCity"), new FulltextIndexOptions().minLength(2));
			
			// to check permission
			dbCol.ensurePersistentIndex(Arrays.asList("ownerUsername"), new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("authorizedEditors[*]"),new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("authorizedViewers[*]"),new PersistentIndexOptions().unique(false));
			
			dbCol.ensurePersistentIndex(Arrays.asList("keywords[*]"),new PersistentIndexOptions().unique(false));
			dbCol.ensurePersistentIndex(Arrays.asList("businessIndustries[*]"),new PersistentIndexOptions().unique(false));
		}
		return dbCol;
	}

	@Override
	public boolean dataValidation() {
		return StringUtil.isNotEmpty(this.id) && StringUtil.isNotEmpty(this.name);
	}

	@Override
	public String buildHashedId() throws IllegalArgumentException {
		if (StringUtil.isEmpty(this.id)) {
			String keyHint =  RandomUtil.getRandom(1000000) + this.name + "-" + this.primaryEmail + "-" + this.primaryWebsite + "-" + System.currentTimeMillis();
			this.id = createId(this.id, keyHint);
		}
		return this.id;
	}

	@Override
	public String getDocumentUUID() {
		return getDocumentUUID(COLLECTION_NAME, this.id);
	}



	public String getBusinessType() {
		return businessType;
	}

	public void setBusinessType(String businessType) {
		this.businessType = businessType;
	}

	public String getBusinessTaxId() {
		return businessTaxId;
	}

	public void setBusinessTaxId(String businessTaxId) {
		this.businessTaxId = businessTaxId;
	}

	public String getBusinessId() {
		return businessId;
	}

	public void setBusinessId(String businessId) {
		this.businessId = businessId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getPrimaryLogo() {
		return primaryLogo;
	}

	public void setPrimaryLogo(String primaryLogo) {
		this.primaryLogo = primaryLogo;
	}

	public String getPrimaryWebsite() {
		return primaryWebsite;
	}

	public void setPrimaryWebsite(String primaryWebsite) {
		this.primaryWebsite = primaryWebsite;
	}

	public String getPrimaryEmail() {
		return primaryEmail;
	}

	public void setPrimaryEmail(String primaryEmail) {
		this.primaryEmail = primaryEmail;
	}

	public String getPrimaryPhone() {
		return primaryPhone;
	}

	public void setPrimaryPhone(String primaryPhone) {
		this.primaryPhone = primaryPhone;
	}

	public String getLocationAddress() {
		return locationAddress;
	}

	public void setLocationAddress(String locationAddress) {
		this.locationAddress = locationAddress;
	}

	public String getLocationCity() {
		return locationCity;
	}

	public void setLocationCity(String locationCity) {
		this.locationCity = locationCity;
	}

	public String getLocationCode() {
		return locationCode;
	}

	public void setLocationCode(String locationCode) {
		this.locationCode = locationCode;
	}

	public String getLocationZipCode() {
		return locationZipCode;
	}

	public void setLocationZipCode(String locationZipCode) {
		this.locationZipCode = locationZipCode;
	}

	public String getOriginCountry() {
		return originCountry;
	}

	public void setOriginCountry(String originCountry) {
		this.originCountry = originCountry;
	}

	public String getParentId() {
		return parentId;
	}

	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	public String getDescription() {
		return description;
	}

	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Map<String, String> getBusinessEmails() {
		return businessEmails;
	}

	public void setBusinessEmails(Map<String, String> businessEmails) {
		if(businessEmails != null) {
			this.businessEmails.putAll(businessEmails);
		}
	}

	public Map<String, String> getBusinessPhones() {
		return businessPhones;
	}

	public void setBusinessPhones(Map<String, String> businessPhones) {
		if(businessPhones != null) {
			this.businessPhones.putAll(businessPhones);
		}
	}

	public Set<BusinessAction> getBusinessActions() {
		return businessActions;
	}

	public void setBusinessActions(Set<BusinessAction> businessActions) {
		if(businessActions != null) {
			this.businessActions.addAll(businessActions);
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public Set<String> getKeywords() {
		return keywords;
	}

	public void setKeywords(Set<String> keywords) {
		if(keywords != null) {
			this.keywords.addAll(keywords);
		}
	}

	public Map<String, String> getBusinessIds() {
		return businessIds;
	}

	public void setBusinessIds(Map<String, String> businessIds) {
		if(businessIds != null) {
			this.businessIds.putAll(businessIds);
		}
	}

	public Set<String> getBusinessIndustries() {
		return businessIndustries;
	}

	public void setBusinessIndustries(Set<String> businessIndustries) {
		if(businessIndustries != null) {
			this.businessIndustries.addAll(businessIndustries);
		}
	}

	public Set<BusinessDeal> getBusinessDeals() {
		return businessDeals;
	}

	public void setBusinessDeals(Set<BusinessDeal> businessDeals) {
		if(businessDeals != null) {
			this.businessDeals.addAll(businessDeals);
		}
	}

	public long getTotalProfile() {
		return totalProfile;
	}

	public void setTotalProfile(long totalProfile) {
		this.totalProfile = totalProfile;
	}

	public String getStockCode() {
		return stockCode;
	}

	public void setStockCode(String stockCode) {
		this.stockCode = stockCode;
	}

	public Set<BusinessContract> getSignedContracts() {
		return signedContracts;
	}

	public void setSignedContracts(List<BusinessContract> signedContracts) {
		if(signedContracts != null) {
			this.signedContracts.addAll(signedContracts);
		}
	}

	@Override
	public Date getCreatedAt() {
		return createdAt;
	}

	@Override
	public void setCreatedAt(Date createdAt) {
		this.createdAt = createdAt;
	}

	@Override
	public Date getUpdatedAt() {
		return updatedAt;
	}

	@Override
	public void setUpdatedAt(Date updatedAt) {
		this.updatedAt = updatedAt;
	}
	
	public List<String> getAuthorizedEditors() {
		if(authorizedEditors == null) {
			authorizedEditors = new ArrayList<>();
		}
		return authorizedEditors;
	}

	public void setAuthorizedEditors(List<String> authorizedEditors) {
		if(authorizedEditors != null) {
			this.authorizedEditors = authorizedEditors;
		}
	}
	
	public void setAuthorizedEditor(String authorizedEditorId) {
		if(StringUtil.isNotEmpty(authorizedEditorId)) {
			this.authorizedEditors.add(authorizedEditorId);
		}
	}
	
	public List<String> getAuthorizedViewers() {
		return authorizedViewers;
	}

	public void setAuthorizedViewers(List<String> authorizedViewers) {
		if(authorizedViewers != null) {
			this.authorizedViewers = authorizedViewers;
		}
	}
	
	public void setAuthorizedViewer(String authorizedViewerId) {
		if(StringUtil.isNotEmpty(authorizedViewerId)) {
			this.authorizedViewers.add(authorizedViewerId);
		}
	}
	
	public String getOwnerUsername() {
		if(this.ownerUsername == null) {
			return "";
		}
		return ownerUsername;
	}

	public void setOwnerUsername(String ownerUsername) {
		if(StringUtil.isEmpty(this.ownerUsername)) {
			this.ownerUsername = ownerUsername;
		}
	}

	public Map<String, Integer> getProfileScoreMap() {
		return profileScoreMap;
	}

	public void setProfileScoreMap(Map<String, Integer> profileScoreMap) {
		if(profileScoreMap != null) {
			this.profileScoreMap.putAll(profileScoreMap);
		}
	}
	
	public void setProfileScore(String profileId, int score) {
		this.profileScoreMap.put(profileId, score);
	}

	@Override
	public long getMinutesSinceLastUpdate() {
		return getDifferenceInMinutes(this.updatedAt);
	}
}
