package org.chengy;

import org.chengy.configuration.CrawlerBizConfig;
import org.chengy.service.analyzer.SongRecordAnalyzer;
import org.chengy.service.crawler.CrawlerLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class CrawlerApplication implements CommandLineRunner {

	@Autowired
	CrawlerLauncher launcher;
	@Autowired
	SongRecordAnalyzer songRecordAnalyzer;
	@Autowired
	CrawlerBizConfig bizConfig;


	public static void main(String[] args) {
		SpringApplication.run(CrawlerApplication.class, args);
	}

	public void run(String... var1) throws Exception {
//		bizConfig.specifyCrawlerUser(Arrays.asList("330313", "252839335", "625356566", "250038717"));
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//					launcher.crawlM163User();
//
//			}
//		}).start();
////
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				launcher.crawlM163Songs();
//
//			}
//		}).start();

	//	songRecordAnalyzer.getSongRecordInfo();
	}

}
