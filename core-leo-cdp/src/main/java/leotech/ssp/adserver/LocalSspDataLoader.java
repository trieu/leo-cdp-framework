package leotech.ssp.adserver;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.logging.log4j.core.LoggerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atg.openssp.common.configuration.GlobalContext;
import com.atg.openssp.core.cache.broker.dto.PricelayerDto;
import com.atg.openssp.core.cache.broker.dto.SiteDto;
import com.atg.openssp.core.cache.broker.dto.SupplierDto;
import com.atg.openssp.core.cache.type.ConnectorCache;
import com.atg.openssp.core.cache.type.PricelayerCache;
import com.atg.openssp.core.cache.type.SiteDataCache;
import com.atg.openssp.core.exchange.channel.rtb.OpenRtbConnector;
import com.atg.openssp.core.system.LocalContext;
import com.google.gson.Gson;

public class LocalSspDataLoader {
	private static final Logger log = LoggerFactory.getLogger(LocalSspDataLoader.class);
	final static String path = "./configs/openssp/site_db.json";
	final static String logConfigFile = "./configs/openssp/log4j2.xml";

	public static void init() {
		LoggerContext.getContext().setConfigLocation(new File(logConfigFile).toURI());

		GlobalContext.refreshContext();
		LocalContext.setDspChannelEnabled(true);
		LocalContext.setSspChannelEnabled(true);
		LocalContext.setAdservingChannelEnabled(true);
		LocalContext.setVerboseEnabled(false);
		LocalContext.setSspVersion("1.0");

		loadSupplierCache();
		loadSiteCaching();
		loadPriceLayerCache();
	}

	protected static boolean loadSiteCaching() {
		final Gson gson = new Gson();
		try {

			final String content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
			final SiteDto dto = gson.fromJson(content, SiteDto.class);
			System.out.println(content);
			if (dto != null) {
				log.info("sizeof site data=" + dto.getSites().size());
				dto.getSites().forEach(site -> {
					SiteDataCache.instance.put(site.getId(), site);
					System.out.println("=> " + site.getId() + " : " + new Gson().toJson(site));
				});
				SiteDataCache.instance.switchCache();
				System.out.println(SiteDataCache.instance.getAll());
				return true;
			}
			log.error("no Site data");
			return false;
		} catch (final IOException e) {
			log.error(e.getMessage());
		}

		return true;
	}

	public static boolean loadSupplierCache() {
		final Gson gson = new Gson();
		try {
			final String path = "./configs/openssp/supplier_db.json";
			final String content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
			final SupplierDto dto = gson.fromJson(content, SupplierDto.class);
			if (dto != null) {
				log.info("sizeof supplier data=" + dto.getSupplier().size());
				dto.getSupplier().forEach(supplier -> {
					final OpenRtbConnector openRtbConnector = new OpenRtbConnector(supplier);
					ConnectorCache.instance.add(openRtbConnector);
				});

				// TODO
				// ConnectorCache.instance.add(key);

				ConnectorCache.instance.switchCache();
				return true;
			}
			log.error("no Supplier data");
			return false;
		} catch (final IOException e) {
			log.error(e.getMessage());
		}
		return true;
	}

	protected static boolean loadPriceLayerCache() {
		final Gson gson = new Gson();
		try {
			final String path = "./configs/openssp/price_layer.json";
			final String content = new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
			final PricelayerDto dto = gson.fromJson(content, PricelayerDto.class);
			if (dto != null) {
				log.info("sizeof pricelayer data=" + dto.getPricelayer().size());
				dto.getPricelayer().forEach(pricelayer -> {
					PricelayerCache.instance.put(pricelayer.getSiteid(), pricelayer);
				});
				PricelayerCache.instance.switchCache();
				return true;
			}
			log.error("no price data");
			return false;
		} catch (final IOException e) {
			log.error(e.getMessage());
		}
		return true;
	}
}
