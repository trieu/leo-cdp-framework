package test.parser;

import java.util.concurrent.ExecutionException;

import leotech.system.model.DeviceInfo;
import leotech.system.util.DeviceParserUtil;
import rfx.core.util.Utils;
import rfx.core.util.useragent.Client;
import rfx.core.util.useragent.Parser;

public class UserAgentParserTest {
    public static void main(String[] args) throws ExecutionException {
	String ua = "";
	// ua = "Opera/9.80 (Linux mips; Opera TV Store/4510; U; vi) Presto/2.10.250
	// Version/11.60 Model/Sony-KDL-46EX650 SonyCEBrowser/1.0 (KDL-46EX650;
	// CTV/PKG2.120GAA; VNM)";
	// ua = "Mozilla/5.0 (SMART-TV; Linux; Tizen 2.3) AppleWebkit/538.1 (KHTML, like
	// Gecko) SamsungBrowser/1.0 Safari/538.1";
	// ua = "Opera/9.80 (Linux armv7l; InettvBrowser/2.2
	// (00014A;SonyDTV115;0002;0100) KDL42W650A; CC/GRC) Presto/2.12.362
	// Version/12.11";
	// ua = "Opera/9.80 (Linux armv7l; Opera TV Store/5602; Model/Sony-KDL-42W674A
	// SonyCEBrowser/1.0 (KDL-42W674A; CTV2013/PKG4.540GAA; VNM)) Presto/2.12.362
	// Version/12.11";
	// ua = "Mozilla/5.0 (Web0S; Linux/SmartTV) AppleWebKit/538.2 (KHTML, like
	// Gecko) Large Screen WebAppManager Safari/538.2";
	// ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 6_0 like Mac OS X)
	// AppleWebKit/536.26 (KHTML, like Gecko) Version/6.0 Mobile/10A5376e
	// Safari/8536.25";
	
	// smart tv sony
//	ua = "Opera/9.80 (Linux armv7l; Opera TV Store/6221) Presto/2.12.407 Version/12.50 Model/Sony-KDL-42W700B SonyCEBrowser/1.0 (KDL-42W700B; CTV2014/PKG2.303GAA; XX)";

	//pc desktop
	ua = "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.79 Safari/537.36";
	
	//mobile iphone x
	ua = "Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) Version/11.0 Mobile/15A372 Safari/604.1";
	
	//mobile samsung
	//ua = "Mozilla/5.0 (Linux; Android 5.0; SM-G900P Build/LRX21T) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/67.0.3396.79 Mobile Safari/537.36";
	
	//ipad
	//ua = "Mozilla/5.0 (iPad; CPU OS 11_0 like Mac OS X) AppleWebKit/604.1.34 (KHTML, like Gecko) Version/11.0 Mobile/15A5341f Safari/604.1";
	
	//ua = "";
	
	Client uaClient = Parser.load().parse(ua);
	System.out.println(uaClient.device.family.split(" ")[0]);
	System.out.println(uaClient.device.deviceType());
	System.out.println(uaClient.os.family);
	System.out.println(uaClient.os.major);
	System.out.println(uaClient.os.minor);
	System.out.println(uaClient.userAgent.family);
	System.out.println(uaClient.userAgent.major);

	System.out.println("-------------------------------------");

	for (int i = 0; i < 1; i++) {

	    DeviceInfo deviceInfo = DeviceParserUtil.parseWithCache(ua);
	    System.out.println(deviceInfo.deviceName);
	    System.out.println(deviceInfo.deviceOs);
	    System.out.println(deviceInfo.platformType);
	    Utils.sleep(1000);
	}

    }
}
