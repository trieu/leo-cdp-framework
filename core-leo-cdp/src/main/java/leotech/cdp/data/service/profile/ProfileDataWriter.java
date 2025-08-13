package leotech.cdp.data.service.profile;

import java.util.List;

import leotech.cdp.dao.ProfileDaoUtil;
import leotech.cdp.data.DataWriter;
import leotech.cdp.model.customer.ProfileSingleView;

/**
 *  to save ProfileSingleView
 * 
 * @author tantrieuf31
 * @since 2022
 *
 */
public class ProfileDataWriter extends DataWriter<ProfileSingleView> {

	static final boolean UPSERT_MODE = true;

	@Override
	public void process(List<ProfileSingleView> list, CallbackProcessor<ProfileSingleView> callback) {
		list.forEach(p -> {
			this.writeData(p);
			if(callback != null) {
				callback.process(p);	
			}
		});
	}
	
	@Override
	protected void writeData(ProfileSingleView p) {
		if(autoWriteData) {
			System.out.println("==> ProfileDataWriter Fullname: " + p.getLastName() + " " + p.getMiddleName() + " " + p.getFirstName());
			String insertedId = ProfileDaoUtil.insertProfile(p, null);
			System.out.println("ProfileDataWriter OK with insertedId = " + insertedId);
		}
	}
}
