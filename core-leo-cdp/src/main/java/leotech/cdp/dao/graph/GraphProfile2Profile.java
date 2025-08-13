package leotech.cdp.dao.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoDatabase;
import com.arangodb.ArangoEdgeCollection;
import com.arangodb.entity.EdgeDefinition;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.model.asset.AssetContent;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.graph.Profile2Profile;
import leotech.system.util.database.ArangoDbCommand;
import leotech.system.util.database.ArangoDbCommand.CallbackQuery;

/**
 * @author tantrieuf31
 * @since 2023
 *
 */
public final class GraphProfile2Profile extends AbstractCdpDatabaseUtil {

	private static final String GRAPH_NAME = Profile2Profile.GRAPH_NAME;
	
	public final static void initGraph(ArangoDatabase db) {
		Collection<EdgeDefinition> edgeDefinitions = new ArrayList<>();
        EdgeDefinition edgeDefinition = new EdgeDefinition().collection(Profile2Profile.COLLECTION_NAME).from(Profile.COLLECTION_NAME).to(AssetContent.COLLECTION_NAME);
        edgeDefinitions.add(edgeDefinition);
        
        // check before create
        if (!db.graph(GRAPH_NAME).exists()) {
        	db.createGraph(GRAPH_NAME, edgeDefinitions, null);
        }
        
        Profile2Profile.initIndex();
	}
	

	public final static void updateEdgeData(ProfileIdentity fromProfileIdentity, ProfileIdentity toProfileIdentity, int singleViewScore) {
		// init the edge
		Profile2Profile insertedEdge = new Profile2Profile(fromProfileIdentity, toProfileIdentity, singleViewScore);
		
		// get edge collect of graph
		ArangoEdgeCollection arangoEdgeCollection = Profile2Profile.getArangoEdgeCollection();
		
		// set data
		arangoEdgeCollection.insertEdge(insertedEdge);
	}
	
	
	
	public final static List<Profile2Profile> querySingleView(ProfileIdentity fromProfileIdentity, int startIndex, int numberResult, int singleViewScoreLimit) {
		ArangoDatabase db = getCdpDatabase();
		
		String aql = Profile2Profile.getGraphQueryForSingleView(singleViewScoreLimit);
		
		Map<String, Object> bindVars = new HashMap<>(4);
		bindVars.put("fromProfileId", fromProfileIdentity.getDocumentUUID());
		bindVars.put("startIndex", startIndex);
		bindVars.put("numberResult", numberResult);
		bindVars.put("singleViewScoreLimit", singleViewScoreLimit);
		
		CallbackQuery<Profile2Profile> callback = new CallbackQuery<Profile2Profile>() {
			@Override
			public Profile2Profile apply(Profile2Profile obj) {
				Profile toProfile = ProfileDaoUtil.getProfileById(obj.getToProfileId(), false);
				obj.setToProfile(toProfile);
				return obj;
			}
		};
		List<Profile2Profile> rs = new ArangoDbCommand<Profile2Profile>(db, aql, bindVars, Profile2Profile.class, callback).getResultsAsList();
		return rs;
	}
	
}
