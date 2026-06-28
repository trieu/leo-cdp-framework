package leotech.starter.router;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.dao.FileMetadataDaoUtil;
import leotech.cdp.model.asset.FileMetadata;
import leotech.cdp.model.customer.Profile;
import leotech.system.common.BaseHttpHandler;
import leotech.system.common.BaseHttpHandler.JsonErrorPayload;
import leotech.system.common.BaseHttpRouter;
import leotech.system.common.BaseWebRouter;
import leotech.system.common.SecuredHttpDataHandler;
import leotech.system.model.FileUploaderData;
import leotech.system.model.JsonDataPayload;
import leotech.system.model.SystemUser;
import leotech.system.util.HttpTrackingUtil;
import leotech.system.version.SystemMetaData;
import rfx.core.util.HashUtil;
import rfx.core.util.StringUtil;

/**
 * Handles HTTP routing for file uploads within the CDP.
 * Refactored to improve readability, separate concerns, and enhance file I/O
 * safety.
 * * @author thomas
 * 
 * @since 2021
 */
public final class UploaderHttpRouter extends BaseHttpRouter {

	static final Logger logger = LoggerFactory.getLogger(UploaderHttpRouter.class);

	static final String PROFILE = "profile";
	static final String IMPORTER_PREFIX = "importer-";

	public static final String STATIC_BASE_URL = "//" + SystemMetaData.DOMAIN_STATIC_CDN;
	public static final String UPLOADED_FILES_LOCATION = "/public/uploaded-files/";

	public UploaderHttpRouter(RoutingContext context, String host, int port) {
		super(context, host, port);
		logger.info("init UploadFileHttpRouter");
	}

	@Override
	public void process() throws Exception {
		HttpServerRequest req = context.request();
		HttpServerResponse resp = context.response();
		MultiMap reqHeaders = req.headers();

		// 1. Set standard response headers early to avoid duplication
		setDefaultHeaders(resp.headers());

		// 2. Authenticate User
		String userSession = StringUtil.safeString(reqHeaders.get(BaseWebRouter.HEADER_SESSION));
		SystemUser loginUser = SecuredHttpDataHandler.initSystemUser(userSession, req.path(), req.params());
		String httpMethod = req.rawMethod();

		// 3. Route request based on HTTP method
		if (HTTP_METHOD_POST.equalsIgnoreCase(httpMethod)) {
			handlePostUpload(req, resp, reqHeaders, loginUser);
		} else if (HTTP_GET_OPTIONS.equalsIgnoreCase(httpMethod) || HTTP_METHOD_GET.equalsIgnoreCase(httpMethod)) {
			handleGetOptions(resp);
		} else {
			// Good practice: Handle unsupported HTTP methods
			resp.setStatusCode(405).end("Method Not Allowed");
			resp.close();
		}
	}

	/**
	 * Sets standard headers required for all responses in this router.
	 */
	private void setDefaultHeaders(MultiMap outHeaders) {
		outHeaders.set(CONNECTION, HttpTrackingUtil.HEADER_CONNECTION_CLOSE);
		outHeaders.set(POWERED_BY, SERVER_VERSION);
		outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_JSON);
	}

	/**
	 * Handles the POST request to upload a file.
	 */
	private void handlePostUpload(HttpServerRequest req, HttpServerResponse resp, MultiMap reqHeaders,
			SystemUser loginUser) {
		if (loginUser == null) {
			resp.setStatusCode(504).end(JsonErrorPayload.NO_AUTHORIZATION.toString());
			resp.close();
			return;
		}

		JsonDataPayload dataPayload = uploadHandler(loginUser, context, req, reqHeaders);

		if (dataPayload != null) {
			resp.setStatusCode(201).end(dataPayload.toString());
		} else {
			resp.setStatusCode(500).end("Error on uploading file or unauthorized data operator.");
		}
		resp.close();
	}

	/**
	 * Handles GET and OPTIONS requests for status/health checks.
	 */
	private void handleGetOptions(HttpServerResponse resp) {
		resp.end("CDP Uploader_" + nodeInfo);
	}

	/**
	 * Extracts metadata from headers and triggers the local storage upload process.
	 */
	public static JsonDataPayload uploadHandler(SystemUser loginUser, RoutingContext context, HttpServerRequest request,
			MultiMap reqHeaders) {
		String refObjClass = StringUtil.safeString(reqHeaders.get("refObjectClass"));
		String refObjKey = StringUtil.safeString(reqHeaders.get("refObjectKey"));

		logger.info("refObjKey {}", refObjKey);

		return uploadUsingLocalStorage(loginUser, context, request, refObjClass, refObjKey);
	}

	/**
	 * Processes file uploads, moves files from temp to final destination, and saves
	 * metadata.
	 * Refactored to reduce nested 'if' statements and use java.nio.file.Files for
	 * safer file operations.
	 */
	private static JsonDataPayload uploadUsingLocalStorage(SystemUser loginUser, RoutingContext context,
			HttpServerRequest request, String refObjClass, String refObjKey) {

		// Early exit if user is not authorized to operate on data
		if (!SecuredHttpDataHandler.isDataOperator(loginUser)) {
			logger.warn("User {} is not an authorized data operator.", loginUser.getUserLogin());
			return null;
		}

		JsonDataPayload dataPayload = null;
		FileUploaderData data = new FileUploaderData();
		Set<FileUpload> fileUploads = context.fileUploads();
		String owner = loginUser.getUserLogin();

		for (FileUpload uploadedFile : fileUploads) {
			String name = uploadedFile.fileName();
			long size = uploadedFile.size();
			String extension = FilenameUtils.getExtension(name).toLowerCase();

			// Generate unique file name
			String keyHint = size + uploadedFile.uploadedFileName() + System.currentTimeMillis();
			String newFileName = HashUtil.sha1(keyHint);
			String fileUri = UPLOADED_FILES_LOCATION + newFileName + "." + extension;

			data.setFileUrl(fileUri);

			try {
				// Use NIO Files.move instead of File.renameTo for safer file manipulation
				// across different file systems
				Path sourceTempFile = Paths.get("./" + uploadedFile.uploadedFileName());
				Path targetFinalFile = Paths.get("." + fileUri);

				// Ensure parent directories exist before moving
				Files.createDirectories(targetFinalFile.getParent());
				Files.move(sourceTempFile, targetFinalFile, StandardCopyOption.REPLACE_EXISTING);

			} catch (IOException e) {
				logger.error("Failed to move uploaded file {} to final destination {}", uploadedFile.uploadedFileName(),
						fileUri, e);
				continue; // Skip metadata save if file move fails
			}

			// Save Metadata based on reference object class
			if (PROFILE.equals(refObjClass)) {
				if (loginUser.canInsertData(Profile.class)) {
					saveFileMetadata(owner, fileUri, name, refObjClass, refObjKey);
					dataPayload = new JsonDataPayload(request.uri(), data, true);
				} else {
					logger.warn("User {} does not have permission to insert Profile data.", owner);
				}
			} else {
				saveFileMetadata(owner, fileUri, name, refObjClass, refObjKey);
				// TODO thumbnail cronjob here
				dataPayload = new JsonDataPayload(request.uri(), data, true);
			}

			logger.info("uploadHandler.filename: {} | fileSize: {} | contentType: {}", name, size,
					uploadedFile.contentType());
		}

		return dataPayload;
	}

	/**
	 * Helper method to encapsulate file metadata saving logic.
	 */
	private static void saveFileMetadata(String owner, String fileUri, String name, String refObjClass,
			String refObjKey) {
		FileMetadata fileMetadata = new FileMetadata(owner, fileUri, name, refObjClass, refObjKey);
		FileMetadataDaoUtil.save(fileMetadata);
	}
}