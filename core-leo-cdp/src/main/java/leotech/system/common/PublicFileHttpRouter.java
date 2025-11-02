package leotech.system.common;

import java.io.File;

import org.apache.http.HttpStatus;
import org.apache.http.entity.ContentType;

import io.netty.handler.codec.http.HttpHeaderNames;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerResponse;
import leotech.system.model.SystemUser;
import leotech.system.template.TemplateUtil;
import leotech.system.util.HttpWebParamUtil;

/**
 * the handler for exported files
 * 
 * @author tantrieuf31
 *
 */
public final class PublicFileHttpRouter {
	
	
	public static final String MIME_TYPE_EXPORTED_FILE = ContentType.APPLICATION_OCTET_STREAM.getMimeType();
	public static final String UNAUTHORIZED_ERROR = "UNAUTHORIZED_ERROR";
	public static final int CACHING_EXPIRED_TIME = 3600 * 24 * 30;// 30 days
	public static final String MAX_AGE_CACHING_PUBLIC = "max-age=" + CACHING_EXPIRED_TIME + ", public";
	public static final String PUBLIC_FILE_ROUTER = "/public";
	public static final String PUBLIC_EXPORTED_FILES_SEGMENT = "/public/exported-files/segment-";

	/**
	 * handle exported file
	 * 
	 * @param resp
	 * @param outHeaders
	 * @param path
	 * @param params
	 */
	public static boolean handle(HttpServerResponse resp, MultiMap outHeaders, String path, MultiMap params) {
		try {
			if(path.startsWith(PUBLIC_EXPORTED_FILES_SEGMENT)) {
				String dataAccessKey = HttpWebParamUtil.getString(params, SecuredHttpDataHandler.DATA_ACCESS_KEY, "");
				SystemUser loginUser = SecuredHttpDataHandler.getUserByDataAccessKey(dataAccessKey);
				
				
				if (loginUser != null) {
					String pathname = "." + path.replaceAll("%20", " ");
					File file = new File(pathname);
					if (file.isFile()) {
						resp.sendFile(pathname);
						return true;
					}
				} else {
					resp.setStatusCode(HttpStatus.SC_UNAUTHORIZED);
					resp.end(UNAUTHORIZED_ERROR);
				}
			} 
			else {
				String pathname = "." + path.replaceAll("%20", " ");
				
				File file = new File(pathname);
				if (file.isFile()) {
					outHeaders.set(HttpHeaderNames.CACHE_CONTROL, MAX_AGE_CACHING_PUBLIC);
					if (params.get("download") != null) {
						outHeaders.set(HttpHeaderNames.CONTENT_TYPE, MIME_TYPE_EXPORTED_FILE);
						String name = file.getName();
						outHeaders.set(HttpHeaderNames.CONTENT_DISPOSITION, "attachment; filename=\"" + name + "\"");
					}
					resp.sendFile(pathname);
					return true;
				} else {
					resp.setStatusCode(HttpStatus.SC_NOT_FOUND);
					resp.end(TemplateUtil._404);
				}
			}
		} catch (Exception e) {
			resp.setStatusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
			resp.end(TemplateUtil._500);
		}
		return false;
	}
}
