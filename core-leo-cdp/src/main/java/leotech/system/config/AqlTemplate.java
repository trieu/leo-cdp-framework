package leotech.system.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import rfx.core.util.StringUtil;
import rfx.core.util.Utils;

public class AqlTemplate {
	static String baseFolderPath = "";
	static final String STRING_QUERY_TEMPLATE_FILE = "./resources/database/database-query-template.aql";

	static Map<String, String> mapQueryTpl = new HashMap<>();

	static {
		try {
			String str = readFileAsString(STRING_QUERY_TEMPLATE_FILE);
			String[] sqlStrTokens = str.split(";");
			for (String sqlStrToken : sqlStrTokens) {
				// System.out.println("=> sqlStrToken" + sqlStrToken);
				if (!sqlStrToken.contains("##")) {
					String[] toks = sqlStrToken.split("=>");
					if (toks.length == 2) {
						String qKey = toks[0].trim();
						String qVal = toks[1].trim().replace("\t", " ").replace("\n", " ");
						// System.out.println("=> qKey" + qKey);
						// System.out.println(qVal);

						if (StringUtil.isNotEmpty(qKey)) {
							if (StringUtil.isEmpty(qVal)) {
								System.err.println(qKey + " is INVALID key in " + STRING_QUERY_TEMPLATE_FILE);
								Utils.exitSystemAfterTimeout(1000);
							}
							if (mapQueryTpl.containsKey(qKey)) {
								System.err.println(qKey + "  is DUPLICATED key in " + STRING_QUERY_TEMPLATE_FILE);
								Utils.exitSystemAfterTimeout(1000);
							}
							mapQueryTpl.put(qKey, qVal);
						}

					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static String getRuntimeFolderPath() {
		if (baseFolderPath.isEmpty()) {
			try {
				File dir1 = new File(".");
				baseFolderPath = dir1.getCanonicalPath();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return baseFolderPath;
	}

	static String readFileAsString(String uri) throws java.io.IOException {
		StringBuffer fileData = new StringBuffer(1000);
		String fullpath = getRuntimeFolderPath() + uri.replace("/", File.separator);
		if (!uri.startsWith("/")) {
			fullpath = getRuntimeFolderPath() + File.separator + uri.replace("/", File.separator);
		}

		// System.out.println(fullpath);
		BufferedReader reader = new BufferedReader(new FileReader(fullpath));
		char[] buf = new char[2048];
		int numRead = 0;
		while ((numRead = reader.read(buf)) != -1) {
			fileData.append(buf, 0, numRead);
		}
		reader.close();
		return fileData.toString();
	}

	public static String get(String key) {
		String sql = mapQueryTpl.get(key);
		if (sql == null) {
			throw new IllegalArgumentException("Not found value for " + key + " in file " + STRING_QUERY_TEMPLATE_FILE);
		}
		return sql;
	}

}
