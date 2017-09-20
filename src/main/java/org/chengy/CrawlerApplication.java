package org.chengy;

import org.chengy.model.User;
import org.chengy.repository.UserRepository;
import org.chengy.service.Crawler163music;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.Example;

@SpringBootApplication
public class CrawlerApplication implements CommandLineRunner {

	@Autowired
	Crawler163music crawler163music;
	@Autowired
	UserRepository userRepositor;

	public static void main(String[] args) {
		SpringApplication.run(CrawlerApplication.class, args);

	}

	public void run(String... var1) throws Exception {
		Thread thread1 = new Thread(new Runnable() {
			@Override
			public void run() {
				crawler163music.getUserInfo("56090514");
			}
		});

		Thread thread2 = new Thread(new Runnable() {
			@Override
			public void run() {
				crawler163music.getUserInfo("122132928");
			}
		});
		thread1.start();
		thread2.start();

	}

}
