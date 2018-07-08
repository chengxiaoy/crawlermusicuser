package org.chengy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Lists;
import org.chengy.model.Music163Song;
import org.chengy.model.Music163User;
import org.chengy.repository.remote.Music163SongRepository;
import org.chengy.repository.remote.Music163UserRepository;
import org.chengy.service.analyzer.SongRecordAnalyzer;
import org.chengy.service.discovery.Music163Discovery;
import org.chengy.service.statistics.Music163Statistics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class pythonTest {

	private static final Logger LOGGER = LoggerFactory.getLogger(pythonTest.class);

	@Value("${profile}")
	String env;
	@Autowired
	Music163Discovery music163Discovery;
	@Autowired
	Music163Statistics music163Statistics;
	@Autowired
	SongRecordAnalyzer songRecordAnalyzer;

	@Autowired
	Music163SongRepository songRepository;
	@Autowired
	Music163UserRepository userRepository;

	@Autowired
	MongoTemplate mongoTemplate;

	@Test
	public void mongoTest() {
		Query query = new Query();
		query.skip(1000).limit(1000);
		System.out.println(System.currentTimeMillis());
		List<Music163User> music163UserList = mongoTemplate.find(query, Music163User.class);
		System.out.println(System.currentTimeMillis());
		System.out.println(music163UserList.size());

	}


	@Test
	public void loggerTest() {
		LOGGER.trace("trace logger");
		LOGGER.debug("debug logger");
		LOGGER.info("info logger");
		LOGGER.warn("warn logger");
		LOGGER.error("error logger");
	}


	@Test
	public void averageScoreTest() {
		Map<String, Double> map = songRecordAnalyzer.getSongAverageScore(Arrays.asList("440353010"));
		System.out.println(map);
	}

	@Test
	public void newRecommend() throws Exception {
		Map<String, Double> map = music163Statistics.getUserRelativeSong("252839335", 10);
		List<Music163Song> songList = Lists.newArrayList(songRepository.findAllById(map.keySet()));
		System.out.println("======tf-idf score======");
		Map<String, Double> songInfo = songList.stream().collect(Collectors.toMap(ob -> ob.getTitle(), ob -> map.get(ob.getId())));
		System.out.println(songInfo);


	}


	@Test
	public void getUserRelativeSongTest() throws Exception {
		Map<String, Double> map = music163Statistics.getUserRelativeSong("330313", 10);
		List<Music163Song> songList = Lists.newArrayList(songRepository.findAllById(map.keySet()));

		Map<String, Double> scoreMap = songRecordAnalyzer.getSongAverageScore(map.keySet());
		System.out.println("======tf-idf score======");
		Map<String, Double> songInfo = songList.stream().collect(Collectors.toMap(ob -> ob.getTitle(), ob -> map.get(ob.getId())));
		System.out.println(songInfo);
		System.out.println("======average love score======");
		songInfo = songList.stream().collect(Collectors.toMap(ob -> ob.getTitle(), ob -> scoreMap.get(ob.getId())));
		System.out.println(songInfo);
	}

	@Test
	public void getSimilarSongsTest() throws JsonProcessingException {
		String songId = "483671599";

		System.out.println(music163Discovery.getSimilarSongs(songId, 20, false));
//        System.out.println(music163Discovery.getSimilarSongs(songId, 20, true));
	}


	@Test
	public void getUserBasedRecommendTest() {
		String uid = "476915651";
		System.out.println(env);
		List<Music163Song> songList = music163Discovery.userBasedRecommend(uid, 100, 20);
		System.out.println("====== user recommend======" + songList);
		songList = music163Discovery.itemBasedRecommend(uid, 100, 20);
		System.out.println("====== item recommend======" + songList);
		songList = music163Discovery.getRecommendSongs(uid, 100, 20);
		System.out.println("====== model recommend======" + songList);

	}


	@Test
	public void testScore() {


	}

	@Test
	public void testRecommend() {
		List<Music163Song> songList = music163Discovery.getRecommendSongs("330313", 100, 10);
		System.out.println(songList);
	}


	public static void main(String[] args) {
		Process process;
		try {
			String argv1 = "330313";
			String argv2 = "1428889750,304528598,261810251,442709295,123163229,353085612";
			String filePath = "/Users/chengxiaoy/PycharmProjects/abracadabra/music163/user/user_recommend.py";
			String[] argvs = new String[]{"python", filePath, argv1, argv2};
			process = Runtime.getRuntime().exec(argvs);
			//   process.waitFor();
			BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String s;
			while ((s = stdOut.readLine()) != null) {
				System.out.println(s);
			}
			int result = process.waitFor();
			System.out.println(result);
			process.destroy();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
	}
}
