package leotech.cdp.domain;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.arangodb.ArangoDBException;
import com.github.jknack.handlebars.internal.text.WordUtils;
import com.github.slugify.Slugify;
import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import leotech.cdp.dao.AssetGroupDaoUtil;
import leotech.cdp.dao.AssetProductItemDaoUtil;
import leotech.cdp.model.asset.AssetGroup;
import leotech.cdp.model.asset.ProductItem;
import leotech.system.model.ImportingResult;
import rfx.core.util.StringUtil;

/**
 * product item catalog management
 * 
 * @author tantrieuf31
 * @since 2021
 *
 */
public class ProductItemManagement {
	
	private static final String PRODUCT = "product";
	private static final String HTTP_SCHEMA_ORG_IN_STOCK = "http://schema.org/InStock";
	private static final int MAX_PRODUCT_TO_GET_KEYWORDS = 20;

	/**
	 * @return
	 */
	public static Map<String, String> getKeywordsForSubscriptionForm(){
		Map<String, String> keywords = new HashMap<String, String>();
		List<ProductItem> products = AssetProductItemDaoUtil.list(0, MAX_PRODUCT_TO_GET_KEYWORDS);
		for (ProductItem productItem : products) {
			Set<String> pKeywords = productItem.getKeywords();
			for (String pKeyword : pKeywords) {
				String k = new Slugify().slugify(pKeyword);
				String v = WordUtils.capitalize(pKeyword);
				keywords.put(k, v);
			}
		}
		return keywords;
	}

	/**
	 * @param groupId
	 * @param importFileUrl
	 * @param previewTop20
	 * @return
	 */
	public static List<ProductItem> parseImportedCsvFile(String groupId, String importFileUrl, boolean previewTop20) {
		AssetGroup group = AssetGroupDaoUtil.getById(groupId);
		if(group == null) {
			// not found any group to contain items
			new ArrayList<ProductItem>(0);
		}
		CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(true);
		if(previewTop20) {
			settings.setNumberOfRecordsToRead(20);
		}
		CsvParser csvParser = new CsvParser(settings);
		List<ProductItem> items = null;
		try {
			String pathname = "." + importFileUrl;
			
			List<String[]> allRows = csvParser.parseAll(new FileReader(new File(pathname)));
			items = allRows.parallelStream().map(data -> {
				ProductItem item = ProductItem.initFromCsvData(groupId, data);
				if(item != null) {
					item.setAvailability(HTTP_SCHEMA_ORG_IN_STOCK);
					item.setContentClass(PRODUCT);
					item.setCategoryIds(group.getCategoryIds());
				}
				return item;
			}).filter(item -> {return item != null;} ).collect(Collectors.toList());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if(items == null) {
				items = new ArrayList<ProductItem>(0);
			}
		}
		return items;
	}
	
	/**
	 * @param groupId
	 * @param importFileUrl
	 * @return
	 */
	public static ImportingResult importCsvProductItems(String groupId, String importFileUrl) {
		AtomicInteger ok = new AtomicInteger();
		AtomicInteger fail = new AtomicInteger();
		if(StringUtil.isNotEmpty(importFileUrl)) {
			List<ProductItem> items = parseImportedCsvFile(groupId, importFileUrl, false);
			items.parallelStream().forEach(item->{
				String id = null;
				try {
					id = AssetProductItemDaoUtil.save(item);
				} catch (ArangoDBException e) {
					fail.incrementAndGet();
				}
				if(id != null) {
					ok.incrementAndGet();
				}
			});
		}
		return new ImportingResult(ok.get(), fail.get());
	}
}
