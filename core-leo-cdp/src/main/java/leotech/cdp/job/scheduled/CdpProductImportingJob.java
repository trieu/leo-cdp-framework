package leotech.cdp.job.scheduled;

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import leotech.cdp.domain.ProductItemManagement;
import leotech.system.model.ImportingResult;
import rfx.core.job.ScheduledJob;
import rfx.core.util.StringUtil;

/**
 * @author Trieu Nguyen
 * @since 2024
 *
 */
public class CdpProductImportingJob extends ScheduledJob  {

	public static class ProductImportingEvent {
		public final String groupId;
		public final String importFileUrl;
		public ProductImportingEvent(String groupId, String importFileUrl) {
			super();
			this.groupId = groupId;
			this.importFileUrl = importFileUrl;
		}
	}

	static ExecutorService executor = Executors.newFixedThreadPool(4);

	Queue<ProductImportingEvent> queue = new LinkedList<>();

	public CdpProductImportingJob(ProductImportingEvent e) {
		queue.add(e);
	}



	@Override
	public void doTheJob() {
		ProductImportingEvent e = queue.poll();
		if(e != null) {
			if(StringUtil.isNotEmpty(e.groupId) && StringUtil.isNotEmpty(e.importFileUrl)) {
				ImportingResult rs = ProductItemManagement.importCsvProductItems(e.groupId, e.importFileUrl);
				System.out.println("importCsvProductItems "+rs);
			}
		}
	}


}
