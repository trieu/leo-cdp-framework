package test.cdp.connector;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import leotech.cdp.model.activation.ActivationRule;
import leotech.system.domain.SystemEventManagement;
import rfx.core.util.Utils;

public class TestScheduleSynchAtSpecificTime extends TimerTask {
	
	static Timer timer = new Timer(true);

	public static void main(String... arguments) {
		TestScheduleSynchAtSpecificTime task = new TestScheduleSynchAtSpecificTime();
		// perform the task once a day at 4 a.m., starting tomorrow morning
		
		//timer.scheduleAtFixedRate(task, task.getFirstTimeToRun(), fONCE_PER_4_SECONDS);
		timer.scheduleAtFixedRate(task, 1, fONCE_PER_4_SECONDS);
		
		System.out.println("start TestScheduleSynchAtSpecificTime at " +  new Date());
		
		Utils.foreverLoop();
	}

	@Override
	public void run() {
		String data = "doing at " +  new Date();
		System.out.println(data);
		String objectName = ActivationRule.class.getSimpleName();
		String objectId = "3Ab916c9nxq9mQh2CzBKti";
		String actionUri = TestScheduleSynchAtSpecificTime.class.getName();
		SystemEventManagement.dataJobLog("127.0.0.1",objectName, objectId, actionUri, data);
	}

	public final static long fONCE_PER_4_SECONDS = 4000;
	public final static long fONCE_PER_MINUTE = 1000 * 60;
	public final static long fONCE_PER_HOUR = fONCE_PER_MINUTE * 60;
	public final static long fONCE_PER_DAY = fONCE_PER_HOUR * 24;


	public Date getFirstTimeToRun() {
		Calendar baseTime = new GregorianCalendar();
		
		int hourOfDay = 20;
		int minute = 20;
		int second = 20;
		
		//startMoment.add(Calendar.DATE, fONE_DAY);
		Calendar result = new GregorianCalendar(baseTime.get(Calendar.YEAR), baseTime.get(Calendar.MONTH),baseTime.get(Calendar.DATE), hourOfDay , minute, second);
		return result.getTime();
	}
}
