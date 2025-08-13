package leotech.cdp.dao;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;
import com.arangodb.ArangoDBException;
import com.arangodb.ArangoDatabase;

import leotech.cdp.model.asset.FileMetadata;
import leotech.system.config.AqlTemplate;
import leotech.system.util.database.ArangoDbUtil;

public class FileMetadataDaoUtil {

	static final String AQL_GET_BY_PATH = AqlTemplate.get("AQL_GET_FILE_METADATA_BY_PATH");
	static final String AQL_GET_BY_NETWORK_ID = AqlTemplate.get("AQL_GET_FILE_METADATA_BY_NETWORK_ID");
	static final String AQL_GET_BY_OWNER_ID = AqlTemplate.get("AQL_GET_FILE_METADATA_BY_OWNER_ID");
	static final String AQL_GET_BY_OBJECT = AqlTemplate.get("AQL_GET_FILE_METADATA_BY_OBJECT");

	public static String save(FileMetadata fileMetadata) {
		if (fileMetadata.dataValidation()) {
			ArangoCollection col = fileMetadata.getDbCollection();
			if (col != null) {
				String _key = col.insertDocument(fileMetadata).getKey();
				return _key;
			}
		}
		return null;
	}

	public static boolean deleteByPath(String path) {
		FileMetadata f = getByPath(path);
		if (f != null) {
			ArangoCollection col = f.getDbCollection();
			col.deleteDocument(f.getId());
			return true;
		}
		return false;
	}

	public static FileMetadata getByPath(String path) {
		ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
		Map<String, Object> bindVars = new HashMap<>(1);
		bindVars.put("path", path);

		ArangoCursor<FileMetadata> cursor = db.query(AQL_GET_BY_PATH, bindVars, null, FileMetadata.class);
		while (cursor.hasNext()) {
			return cursor.next();
		}
		return null;
	}

	public static List<FileMetadata> listAllByNetwork(long networkId) {
		List<FileMetadata> list = new ArrayList<>();
		try {
			ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
			Map<String, Object> bindVars = new HashMap<>(3);
			bindVars.put("networkId", networkId);

			ArangoCursor<FileMetadata> cursor = db.query(AQL_GET_BY_NETWORK_ID, bindVars, null, FileMetadata.class);
			while (cursor.hasNext()) {
				FileMetadata f = cursor.next();
				list.add(f);
			}
		} catch (ArangoDBException e) {
			e.printStackTrace();
		}
		return list;
	}

	public static List<FileMetadata> listAllByOwner(String ownerLogin) {
		List<FileMetadata> list = new ArrayList<>();
		try {
			ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
			Map<String, Object> bindVars = new HashMap<>(3);
			bindVars.put("ownerLogin", ownerLogin);

			ArangoCursor<FileMetadata> cursor = db.query(AQL_GET_BY_OWNER_ID, bindVars, null, FileMetadata.class);
			while (cursor.hasNext()) {
				FileMetadata f = cursor.next();
				list.add(f);
			}
		} catch (ArangoDBException e) {
			e.printStackTrace();
		}
		return list;
	}

	public static List<FileMetadata> listAllByObject(String refObjectClass, String refObjectKey) {
		List<FileMetadata> list = new ArrayList<>();
		try {
			ArangoDatabase db = ArangoDbUtil.getCdpDatabase();
			Map<String, Object> bindVars = new HashMap<>(3);
			bindVars.put("refObjectClass", refObjectClass);
			bindVars.put("refObjectKey", refObjectKey);

			ArangoCursor<FileMetadata> cursor = db.query(AQL_GET_BY_OBJECT, bindVars, null, FileMetadata.class);
			while (cursor.hasNext()) {
				FileMetadata f = cursor.next();
				list.add(f);
			}
		} catch (ArangoDBException e) {
			e.printStackTrace();
		}
		return list;
	}
}
