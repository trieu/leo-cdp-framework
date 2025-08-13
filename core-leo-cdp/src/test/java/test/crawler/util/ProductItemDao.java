package test.crawler.util;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.arangodb.ArangoCollection;
import com.arangodb.ArangoCursor;

import leotech.cdp.dao.AbstractCdpDatabaseUtil;
import leotech.cdp.model.asset.ProductItem;
import leotech.system.util.database.ArangoDbUtil;

public class ProductItemDao extends AbstractCdpDatabaseUtil {
	
	public static boolean importProductItems(List<String> filePaths) {
		ObjectInputStream inOOS = null;
		FileInputStream inFile = null;
		try {			
			for (String filePath : filePaths) {
				inFile = new FileInputStream(filePath);
				inOOS = new ObjectInputStream(inFile);
				@SuppressWarnings("unchecked")
				Collection<ProductItem> aColl = (Collection<ProductItem>) inOOS.readObject();
				for (ProductItem item : aColl) {
					System.out.println(item);
					save(item);
				}
				inOOS.close();
				inFile.close();				
			}					
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				inOOS.close();
				inFile.close();				
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
		}		
		return true;
	}
	
	public static String isExistedDocument(String id, String productId, String siteDomain) {
		Map<String, Object> bindKeys = new HashMap<>(2);
		String aql = "FOR d IN " + ProductItem.COLLECTION_NAME + " FILTER ";
		if (productId == null || productId.isEmpty()) {
			aql += "d._key == @id";
			bindKeys.put("id", id);
		} else {
			aql += "d.productId == @productId";
			bindKeys.put("productId", productId);
		}
		aql += " AND d.siteDomain == @siteDomain LIMIT 1 RETURN d._key";
		bindKeys.put("siteDomain", siteDomain);		
		
		ArangoCursor<String> cursor = ArangoDbUtil.getCdpDatabase().query(aql, bindKeys, String.class);
		
		if (cursor.hasNext())
			return cursor.next();
		return null;
	}

	public static String save(ProductItem item) {
		String id = null;
		if (item.dataValidation()) {
			ArangoCollection col = item.getDbCollection();
			if (col != null) {
				String productId = item.getId();
				id = isExistedDocument(productId,item.getProductId(),item.getSiteDomain());				
				if (id == null) {								
					col.insertDocument(item);
					System.out.println("add " + productId);
				} else {					
					item.setId(id);
					item.setUpdatedAt(new Date());
					col.updateDocument(id, item);
					System.out.println("update " + productId);
				}
			}
		}
		return null;
	}
	
	public static void main(String[] args) {
		List<String> filePaths = new ArrayList<>();
		filePaths.add("watsonsVn_BlockingDequeOfProductItem_72Pages_fromPage12");
		if (importProductItems(filePaths))
			System.out.println("DONE !");;
		
	}
	
}
