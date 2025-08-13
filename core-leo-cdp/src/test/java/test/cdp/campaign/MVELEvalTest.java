package test.cdp.campaign;

import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.mvel2.MVEL;

import com.google.common.collect.Sets;

import leotech.cdp.model.marketing.FlowContext;
import rfx.core.util.DateTimeUtil;

public class MVELEvalTest {

	public static void main(String[] args) throws ParseException {
		TestProfileModel profile = new TestProfileModel(Sets.newHashSet("purchase"),
				DateTimeUtil.parseDateStr("03/07/2024"), "John");
		FlowContext context = new FlowContext(profile);
		context.setResult("123");

		Map<String, FlowContext> map = new HashMap<>();
		map.put("context", context);
		Object eval = MVEL.eval("true", map);
		System.out.println(eval);
	}
}
