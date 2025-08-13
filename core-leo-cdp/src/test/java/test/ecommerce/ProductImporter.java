package test.ecommerce;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import com.univocity.parsers.csv.CsvParser;
import com.univocity.parsers.csv.CsvParserSettings;

import leotech.cdp.dao.AssetProductItemDaoUtil;
import leotech.cdp.model.asset.ProductItem;
import leotech.system.util.UrlUtil;
import rfx.core.util.StringUtil;

public class ProductImporter {

	public static void main(String[] args) {
		importTestProductData();
		
	}

	public static void importTestProductData() {
		CsvParserSettings settings = new CsvParserSettings();
		settings.setHeaderExtractionEnabled(true);
		CsvParser csvParser = new CsvParser(settings);
		
		String groupId = "4hy2C1Lim149fuag4P68zT";
		String pathname = "./data/sample-book-items.csv";
		// parses all rows in one go.
		List<String[]> allRows;
		try {
			allRows = csvParser.parseAll(new FileReader(new File(pathname)));
			allRows.stream().forEach(data -> {
				if(data.length > 10) {
					String contentClass = data[0];
					String keywords = StringUtil.safeString(data[1]);
					String storeId = StringUtil.safeString(data[2]);
					String productIdType = data[3];
					String productId = data[4];
					String title = data[5];
					String description = data[6];
					String image = data[7];
					double originalPrice = StringUtil.safeParseDouble(data[8]) ;
					double salePrice = StringUtil.safeParseDouble(data[9]) ;
					String priceCurrency = data[10];
					String fullUrl =  data[11];
					String siteName = "Demo Bookshop";
					String siteDomain = UrlUtil.getHostName(fullUrl);
					ProductItem item = new ProductItem(groupId, keywords, storeId, productId, productIdType, title, description, image, originalPrice, salePrice, priceCurrency, fullUrl, siteName, siteDomain);
					
					// direct usage
					if("product_book".equals(contentClass)) {
						item.setAvailability("http://schema.org/InStock");
						item.setItemCondition("http://schema.org/NewCondition");
					}
					else if("product_finance".equals(contentClass)) {
						item.setAvailability("http://schema.org/InStock");
					}
					
					item.setContentClass(contentClass);
					System.out.println(item);
					
					if(item.getSalePrice() > 0) {
						AssetProductItemDaoUtil.save(item);
					}
				}
			});
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
