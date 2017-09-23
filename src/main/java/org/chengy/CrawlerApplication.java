package org.chengy;

import org.chengy.model.User;
import org.chengy.repository.UserRepository;
import org.chengy.service.Crawler163music;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;

import java.util.List;

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
				List<User> userList=userRepositor.findByCommunityId("64469779");
				userRepositor.delete(userList);
				crawler163music.getUserInfo("64469779");
			}
		});

		Thread thread2 = new Thread(new Runnable() {
			@Override
			public void run() {
				List<User> userList=userRepositor.findByCommunityId("346947461");
				userRepositor.delete(userList);
				crawler163music.getUserInfo("346947461");
			}
		});
		Thread thread3 = new Thread(new Runnable() {
			@Override
			public void run() {
				List<User> userList=userRepositor.findByCommunityId("48582637");
				userRepositor.delete(userList);
				crawler163music.getUserInfo("48582637");
			}
		});
		Thread thread4 = new Thread(new Runnable() {
			@Override
			public void run() {
				List<User> userList=userRepositor.findByCommunityId("286670807");
				userRepositor.delete(userList);
				crawler163music.getUserInfo("286670807");
			}
		});
		Thread thread5 = new Thread(new Runnable() {
			@Override
			public void run() {
				List<User> userList=userRepositor.findByCommunityId("276439053");
				userRepositor.delete(userList);
				crawler163music.getUserInfo("276439053");
			}
		});
		Thread thread6 = new Thread(new Runnable() {
			@Override
			public void run() {
				List<User> userList=userRepositor.findByCommunityId("273699222");
				userRepositor.delete(userList);
				crawler163music.getUserInfo("273699222");
			}
		});
		Thread thread7 = new Thread(new Runnable() {
			@Override
			public void run() {
				List<User> userList=userRepositor.findByCommunityId("330928393");
				userRepositor.delete(userList);
				crawler163music.getUserInfo("330928393");
			}
		});
		thread1.start();
		thread2.start();
		thread3.start();
		thread4.start();
		thread5.start();
		thread6.start();
		thread7.start();

	}

}
