package leotech.starter.router;

import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;

import java.io.File;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;

import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.RoutingContext;
import leotech.cdp.dao.FileMetadataDaoUtil;
import leotech.cdp.model.asset.FileMetadata;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.file.FileApiResponse;
import leotech.cdp.utils.VngCloudUtil;
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
 * @author thomas
 * @since 2021
 *
 */
public final class UploaderHttpRouter extends BaseHttpRouter {
	
	static final String PROFILE = "profile";
	static final String IMPORTER_PREFIX = "importer-";
	
	public static final String STATIC_BASE_URL = "//" + SystemMetaData.DOMAIN_STATIC_CDN;
	public static final String UPLOADED_FILES_LOCATION = "/public/uploaded-files/";
	static final boolean USE_LOCAL_STORAGE = ! VngCloudUtil.isReadyToUpload();

	public UploaderHttpRouter(RoutingContext context) {
		super(context);
		System.out.println("init UploadFileHttpRouter");
	}

	@Override
	public boolean handle() throws Exception {
		HttpServerRequest req = context.request();
		HttpServerResponse resp = context.response();
		// ---------------------------------------------------------------------------------------------------
		MultiMap outHeaders = resp.headers();
		outHeaders.set(CONNECTION, HttpTrackingUtil.HEADER_CONNECTION_CLOSE);
		outHeaders.set(POWERED_BY, SERVER_VERSION);
		outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_JSON);

		MultiMap reqHeaders = req.headers();
		String origin = StringUtil.safeString(reqHeaders.get(BaseHttpHandler.ORIGIN), "*");
		// String contentType =
		// StringUtil.safeString(reqHeaders.get(BaseApiHandler.CONTENT_TYPE),
		// BaseApiHandler.CONTENT_TYPE_JSON);
		String userSession = StringUtil.safeString(reqHeaders.get(BaseWebRouter.HEADER_SESSION));
		String uri = req.path();
		
		SystemUser loginUser = SecuredHttpDataHandler.initSystemUser(userSession, uri, req.params());

		// CORS Header
		BaseHttpRouter.setCorsHeaders(outHeaders, origin);

		String httpMethod = req.rawMethod();			

		if (HTTP_METHOD_POST.equalsIgnoreCase(httpMethod)) {
			boolean ok = false;
			if (loginUser != null) {
				JsonDataPayload dataPayload = uploadHandler(loginUser, context, req, reqHeaders);
				if(dataPayload != null) {
					resp.setStatusCode(201).end(dataPayload.toString());
				} else {
					resp.setStatusCode(500).end("Error on uploading file");
				}
				ok = true;
			} else {
				resp.setStatusCode(504).end(JsonErrorPayload.NO_AUTHORIZATION.toString());
			}
			resp.close();
			return ok;
		} else {
			outHeaders.set(CONNECTION, HttpTrackingUtil.HEADER_CONNECTION_CLOSE);
			outHeaders.set(BaseWebRouter.POWERED_BY, BaseWebRouter.SERVER_VERSION);
			outHeaders.set(CONTENT_TYPE, BaseHttpHandler.CONTENT_TYPE_JSON);
			
			// CORS Header
			BaseHttpRouter.setCorsHeaders(outHeaders, origin);
			if (HTTP_GET_OPTIONS.equalsIgnoreCase(httpMethod) || HTTP_METHOD_GET.equalsIgnoreCase(httpMethod)) {
				resp.end("CDP Uploader_"+DEFAULT_RESPONSE_TEXT);
			}
		}
		return false;
	}

	
	public static JsonDataPayload uploadHandler(SystemUser loginUser, RoutingContext context, HttpServerRequest request, MultiMap reqHeaders) {
		String refObjClass = StringUtil.safeString(reqHeaders.get("refObjectClass"));
		String refObjKey = StringUtil.safeString(reqHeaders.get("refObjectKey"));
		System.out.println("refObjKey " + refObjKey);
		
		if(USE_LOCAL_STORAGE || refObjKey.startsWith(IMPORTER_PREFIX)) {
			return uploadUsingLocalStorage(loginUser, context, request, reqHeaders, refObjClass, refObjKey);
		}
		else {
			return uploadUsingCloudCdn(loginUser, context, request, reqHeaders, refObjClass, refObjKey );
		}
		
	}

	private static JsonDataPayload uploadUsingCloudCdn(SystemUser loginUser, 
			RoutingContext context,HttpServerRequest request, MultiMap reqHeaders, String refObjClass, String refObjKey) {
		JsonDataPayload dataPayload = null;
		
		if (SecuredHttpDataHandler.isDataOperator(loginUser)) {
			FileUploaderData data = new FileUploaderData();
			Set<FileUpload> fileUploads = context.fileUploads();
			for (FileUpload uploadedFile : fileUploads) {
				String name = uploadedFile.fileName();
				long size = uploadedFile.size();
				String extension = FilenameUtils.getExtension(name).toLowerCase();
				String keyHint = size + uploadedFile.uploadedFileName() + System.currentTimeMillis();
				String newFileName = HashUtil.sha1(keyHint);
				String fileLocalPath = UPLOADED_FILES_LOCATION + newFileName + "." + extension;

				// rename the uploaded image
				File file = new File("./" + uploadedFile.uploadedFileName());
				File finalUploadedFile = new File("." + fileLocalPath);
				file.renameTo(finalUploadedFile);

				// get the Vstorage URL for the image
				VngCloudUtil vstorageUtil = new VngCloudUtil();
				FileApiResponse response = vstorageUtil.uploadFileToVstorage(finalUploadedFile.getPath());
				String fileUri = response.getFileUrl();

				// delete the image saved in local ./public/uploaded-files
				finalUploadedFile.delete();

				if(StringUtil.isNotEmpty(fileUri)) {
					data.setFileUrl(fileUri);

					String owner = loginUser.getUserLogin();
					

					if(PROFILE.equals(refObjClass)) {
						if(loginUser.canInsertData(Profile.class)) {
							FileMetadata fileMetadata = new FileMetadata(owner, fileUri, name, refObjClass, refObjKey);
							FileMetadataDaoUtil.save(fileMetadata);
							dataPayload = new JsonDataPayload(request.uri(), data, true);
						}
					} else {
						// store file meta-data in database
						FileMetadata fileMetadata = new FileMetadata(owner, fileUri, name, refObjClass, refObjKey);
						FileMetadataDaoUtil.save(fileMetadata);

						// TODO thumbnail cronjob here
						// String path = finalUploadedFile.getAbsolutePath();
						// ImageUtil.resize(path, path, percent);
						dataPayload = new JsonDataPayload(request.uri(), data, true);
					}

					System.out.println("uploadHandler.filename: " + name);
					System.out.println(" fileSize: " + size);
					System.out.println(" contentType: " + uploadedFile.contentType());
				}
				else {
					System.out.println(response.getMessage());
					System.out.println(response.getStatusCode());
					dataPayload = new JsonDataPayload(response.getMessage(), data, true);
				}
			}
		}
		return dataPayload;
	}
	
	private static JsonDataPayload uploadUsingLocalStorage(SystemUser loginUser, RoutingContext context,
			HttpServerRequest request, MultiMap reqHeaders, String refObjClass, String refObjKey) {
		JsonDataPayload dataPayload = null;
		if (SecuredHttpDataHandler.isDataOperator(loginUser)) {
			FileUploaderData data = new FileUploaderData();
			Set<FileUpload> fileUploads = context.fileUploads();
			for (FileUpload uploadedFile : fileUploads) {
				String name = uploadedFile.fileName();
				long size = uploadedFile.size();
				String extension = FilenameUtils.getExtension(name).toLowerCase();
				String keyHint = size + uploadedFile.uploadedFileName() + System.currentTimeMillis();
				String newFileName = HashUtil.sha1(keyHint);
				String fileUri = UPLOADED_FILES_LOCATION + newFileName + "." + extension;
				
				data.setFileUrl(fileUri);
				
				// rename the uploaded file
				File file = new File("./" + uploadedFile.uploadedFileName());
				File finalUploadedFile = new File("." + fileUri);
				file.renameTo(finalUploadedFile);

				String owner = loginUser.getUserLogin();
				
				if(PROFILE.equals(refObjClass)) {
					if(loginUser.canInsertData(Profile.class)) {
						FileMetadata fileMetadata = new FileMetadata(owner, fileUri, name, refObjClass, refObjKey);
						FileMetadataDaoUtil.save(fileMetadata);
						dataPayload = new JsonDataPayload(request.uri(), data, true);
					}
				} else {
					// store file meta-data in database
					FileMetadata fileMetadata = new FileMetadata(owner, fileUri, name, refObjClass, refObjKey);
					FileMetadataDaoUtil.save(fileMetadata);

					// TODO thumbnail cronjob here
					// String path = finalUploadedFile.getAbsolutePath();
					// ImageUtil.resize(path, path, percent);
					dataPayload = new JsonDataPayload(request.uri(), data, true);
				}
				
				System.out.println("uploadHandler.filename: " + name);
				System.out.println(" fileSize: " + size);
				System.out.println(" contentType: " + uploadedFile.contentType());
			}
		}
		return dataPayload;
	}
}
