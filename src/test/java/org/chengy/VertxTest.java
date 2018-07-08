package org.chengy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import jdk.management.resource.internal.inst.FileOutputStreamRMHooks;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.chengy.infrastructure.music163.EncryptTools;
import org.chengy.infrastructure.music163.Music163ApiCons;
import org.chengy.infrastructure.music163.UserFactory;
import org.chengy.net.hc.HttpHelper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.chengy.infrastructure.music163.Music163ApiCons.Music163UserHost;

@SuppressWarnings("Duplicates")
public class VertxTest {

	private static WebClient webClient;

	private ExecutorService executorService = Executors.newFixedThreadPool(5);

	static {
		Vertx vertx = Vertx.vertx();
		WebClientOptions webClientOptions = new WebClientOptions();
		webClientOptions.setMaxPoolSize(50).setConnectTimeout(1000).setKeepAlive(true)
				.setDefaultHost("music.163.com");
		webClient = WebClient.create(vertx, webClientOptions);
	}


	public static void main(String[] args) throws Exception {
		String html = HttpHelper.get("https://www.kuaidaili.com/free/inha/1/");

		Document document = Jsoup.parse(html);
		Elements elements=
		document.select("#list > table > tbody > tr");
		for (Element element:elements){
			String  hots=
			element.select("td:nth-child(1)").html();
			String port=element.select("td:nth-child(2)").html();

		}


	}

}
