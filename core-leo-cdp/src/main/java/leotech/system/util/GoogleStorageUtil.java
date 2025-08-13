package leotech.system.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import com.google.api.gax.paging.Page;
import com.google.auth.Credentials;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.Identity;
import com.google.cloud.Policy;
import com.google.cloud.Role;
import com.google.cloud.storage.Acl;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.Storage.BucketListOption;
import com.google.cloud.storage.StorageOptions;
import com.google.cloud.storage.StorageRoles;

/**
 * Google Cloud Storage Util class
 * 
 * @author tantrieuf31
 *
 */
public final class GoogleStorageUtil {

	// https://cloud.google.com/storage/docs/access-control/making-data-public#storage-make-object-public-java
	// https://github.com/googleapis/java-storage
	// https://cloud.google.com/storage/docs/samples/storage-upload-file

	private static final String UTF_8 = "utf-8";
	private static final String PROJECT_ID = "leocdp";
	private static final String GOOGLE_API_CREDENTIALS = "./configs/google-api-credentials.json";

	public static String uploadPublicFile(String contentType, String bucketName, String objectName,
			String baseLicenseFolder, String prefixPublicUrl) {
		try {
			Credentials credentials = GoogleCredentials.fromStream(new FileInputStream(GOOGLE_API_CREDENTIALS));
			Storage storage = StorageOptions.newBuilder().setCredentials(credentials).setProjectId(PROJECT_ID).build()
					.getService();
			Page<Bucket> page = storage.list(BucketListOption.prefix(bucketName));

			List<Acl> defaultAcls = new ArrayList<Acl>();
			page.getValues().forEach(bucket -> {
				if (bucketName.equals(bucket.getName())) {
					defaultAcls.addAll(bucket.getAcl());
				}
			});

			if (defaultAcls.size() > 0) {
				URI filePath = new URI(baseLicenseFolder + objectName);
				BlobId blobId = BlobId.of(bucketName, objectName);
				BlobInfo blobInfo = BlobInfo.newBuilder(blobId).setContentEncoding(UTF_8).setContentType(contentType)
						.setAcl(defaultAcls).build();
				Blob uploadedBlob = storage.create(blobInfo, Files.readAllBytes(Paths.get(filePath)));

				System.out.println("File " + filePath + " uploaded to bucket " + bucketName + " as " + objectName);

				storage.createAcl(uploadedBlob.getBlobId(), Acl.of(Acl.User.ofAllUsers(), Acl.Role.READER));

				String publicUrl = prefixPublicUrl + objectName;

				return publicUrl;
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "";
	}

	protected static void loopBlobListInBucket(Bucket bucket) {
		bucket.list().getValues().forEach(blob -> {
			String fileName = blob.getName();
			System.out.println(fileName);
		});
	}

	public static Blob createBlobFromByteArray(Bucket bucket, String blobName, String data) throws IOException {
		Blob blob = bucket.create(blobName, data.getBytes(UTF_8), "text/plain");
		return blob;
	}

	protected static void makeBucketPublic(Storage storage, String bucketName) {
		Policy originalPolicy = storage.getIamPolicy(bucketName);
		Role legacyBucketReader = StorageRoles.legacyBucketReader();
		Identity allUsers = Identity.allUsers();
		storage.setIamPolicy(bucketName, originalPolicy.toBuilder().addIdentity(legacyBucketReader, allUsers).build());
	}
}
