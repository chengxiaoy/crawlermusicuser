package org.chengy;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.chengy.model.User;
import org.chengy.repository.UserRepository;
import org.chengy.service.Crawler163music;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

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
		Random random = new Random();
		int rand=random.nextInt(200);
		System.out.println(rand);
		int threadNums=13;
		Pageable pageable = new PageRequest(rand, threadNums);
		List<String> listStr= userRepositor.findAll(pageable).getContent().stream().map(ob->ob.getCommunityId()).collect(Collectors.toList());
		System.out.println(listStr);
		Iterator strItr = listStr.iterator();
		for (int i = 0; i < threadNums; i++) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					String communityId = (String) strItr.next();
					List<User> userList = userRepositor.findByCommunityId(communityId);
					userRepositor.delete(userList);
					crawler163music.getUserInfo(communityId);
				}
			});
			thread.start();
		}
	}

}
