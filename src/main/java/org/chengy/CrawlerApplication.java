package org.chengy;

import org.chengy.service.crawler.CrawlerLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CrawlerApplication implements CommandLineRunner {


	@Autowired
	CrawlerLauncher launcher;



	public static void main(String[] args) {
		SpringApplication.run(CrawlerApplication.class, args);
	}

	public void run(String... var1) throws Exception {
	//	launcher.crawlM163User();
	}

}
