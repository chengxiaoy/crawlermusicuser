package org.chengy;

import org.chengy.service.Crawler163music;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CrawlerApplication implements CommandLineRunner {

	@Autowired
	Crawler163music crawler163music;


	public static void main(String[] args) {
		SpringApplication.run(CrawlerApplication.class, args);

	}

	public void run(String... var1) throws Exception {
		crawler163music.getUserInfo("330313");
	}

}
