package org.chengy;

import org.chengy.service.crawler.Crawler163music;
import org.chengy.service.statistics.Music163Statistics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CrawlerApplicationTests {
	@Autowired
	Crawler163music crawler163music;

	@Autowired
	Music163Statistics music163Statistics;


	@Test
	public void contextLoads() {
	}

	@Test
	public void getFans() throws Exception {
		List<String> ids =
				crawler163music.getFansId("95355159");
		System.out.println(ids.size());
		System.out.println(ids);

	}

	@Test
	public void test() {

		music163Statistics.getMostPopSong();

	}


}
