package org.chengy.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by nali on 2017/11/4.
 */
@Component
public class CrawlerBizConfig {

	private static String crawlerUserSeed;

	private static String crawlerUserThreadNum;

	public static String getCrawlerUserSeed() {
		return crawlerUserSeed;
	}

	@Value("${crawler.user.seed}")
	public void setCrawlerUserSeed(String crawlerUserSeed) {
		CrawlerBizConfig.crawlerUserSeed = crawlerUserSeed;
	}

	public static String getCrawlerUserThreadNum() {
		return crawlerUserThreadNum;
	}

	@Value("${crawler.user.threadnum}")
	public void setCrawlerUserThreadNum(String crawlerUserThreadNum) {
		CrawlerBizConfig.crawlerUserThreadNum = crawlerUserThreadNum;
	}
}
