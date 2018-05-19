package org.chengy;

import org.chengy.service.crawler.music163.Crawler163music;
import org.chengy.service.crawler.CrawlerLauncher;
import org.chengy.service.crawler.music163.Vertx163Muisc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CrawlerApplication implements CommandLineRunner {


	@Autowired
	CrawlerLauncher launcher;
	@Autowired
	Crawler163music crawler163music;
	@Autowired
	Vertx163Muisc vertx163Muisc;


	public static void main(String[] args) {
		SpringApplication.run(CrawlerApplication.class, args);

	}

	public void run(String... var1) throws Exception {
//		launcher.saveMusic163SongByUser();
//
		//launcher.getSongRecordInfo();
	//	vertx163Muisc.crawlUser();
	}

}
