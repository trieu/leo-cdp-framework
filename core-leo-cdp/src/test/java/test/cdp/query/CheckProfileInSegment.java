package test.cdp.query;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import com.itfsw.query.builder.exception.ParserNotFoundException;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.dao.SegmentDaoUtil;
import leotech.cdp.model.RefKey;
import leotech.cdp.model.customer.Profile;
import leotech.cdp.model.customer.ProfileIdentity;
import leotech.cdp.model.customer.Segment;
import rfx.core.util.Utils;

public class CheckProfileInSegment {

	public static void main(String[] args) throws ParserNotFoundException, IOException {

		checkOneProfile("6SBgS50qdwKFdpdXIZY4ZR");

		// checkAllProfiles();

		Utils.exitSystemAfterTimeout(20000);
	}

	static void checkAllProfiles() {
		List<Segment> segments = SegmentDaoUtil.getAllActiveSegments();
		System.out.println(" [segments.size] " + segments.size());
		int startIndex = 0;
		int numberResult = 500;

		List<Profile> allProfiles = ProfileDaoUtil.listByPagination(startIndex, numberResult);
		while (allProfiles.size() > 0) {
			System.out.println(" \n startIndex: " + startIndex + " numberResult: " + numberResult);
			allProfiles.parallelStream().forEach(profile -> {
				Set<RefKey> inSegments = profile.getInSegments();
				ProfileDaoUtil.updateProfileSegmentRefs(profile.getId(), segments, inSegments);
			});

			startIndex += numberResult;
			allProfiles = ProfileDaoUtil.listByPagination(startIndex, numberResult);

		}
	}

	static void checkOneProfile(String profileId) {
		ProfileIdentity profileIdx = ProfileDaoUtil.getProfileIdentityById(profileId);
		if(profileIdx != null) {
			Set<RefKey> inSegments = profileIdx.getInSegments();
			List<Segment> segments = SegmentDaoUtil.getAllActiveSegments();
			ProfileDaoUtil.updateProfileSegmentRefs(profileId, segments, inSegments);
		}
		
	}

}
