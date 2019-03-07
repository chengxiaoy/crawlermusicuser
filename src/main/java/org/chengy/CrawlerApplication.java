package org.chengy;

import org.chengy.configuration.CrawlerBizConfig;
import org.chengy.service.analyzer.SongRecordAnalyzer;
import org.chengy.service.crawler.CrawlerLauncher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;
import java.util.concurrent.*;

@SpringBootApplication
public class CrawlerApplication implements CommandLineRunner {

	private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerApplication.class);
	@Autowired
	CrawlerLauncher launcher;
	@Autowired
	SongRecordAnalyzer songRecordAnalyzer;
	@Autowired
	CrawlerBizConfig bizConfig;

	private ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();


	public static void main(String[] args) {
		SpringApplication.run(CrawlerApplication.class, args);
	}


	public void run(String... var1) throws Exception {
		crawlConfig();

		new Thread(new Runnable() {
			@Override
			public void run() {
				launcher.crawlM163User();

			}
		}).start();
//
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				launcher.crawlM163Songs();
//
//			}
//		}).start();F
//
		songRecordAnalyzer.saveSongRecordInfo();


	}

	public void crawlConfig() {
		scheduledExecutorService.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				//bizConfig.setCrawlSongSwitch(true);
				bizConfig.setCrawlUserSwitch(true);

				try {
					Thread.sleep(1000 * 60 * 60 * 2);
				} catch (InterruptedException e) {
					LOGGER.warn("thread has been interrupted");
				}
				bizConfig.setCrawlUserSwitch(false);
				bizConfig.setCrawlSongSwitch(false);
			}
		}, 0, 12, TimeUnit.HOURS);
	}

}
