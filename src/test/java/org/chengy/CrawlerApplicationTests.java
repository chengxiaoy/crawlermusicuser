package org.chengy;

import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.Song;
import org.chengy.repository.SongRepository;
import org.chengy.service.crawler.Crawler163music;
import org.chengy.service.statistics.Music163Statistics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CrawlerApplicationTests {
	@Autowired
	Crawler163music crawler163music;

	@Autowired
	Music163Statistics music163Statistics;

	@Autowired
	SongRepository songRepository;


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

		Song exitSong = songRepository.findSongByCommunityIdAndCommunity("exits", Music163ApiCons.communityName);

		if (exitSong == null) {
			System.out.println("=================not exit============ ");
		}
	}

	@Test
	public void getPopSong() throws IOException {

		music163Statistics.getMostPopSong("popsong.txt");
	}

	@Test
	public void getRelativedUser() throws IOException {

		music163Statistics.relativedUser("330313");
	}

}
