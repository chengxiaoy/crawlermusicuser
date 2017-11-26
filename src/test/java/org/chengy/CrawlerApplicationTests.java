package org.chengy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.chengy.infrastructure.music163secret.EncryptTools;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.Song;
import org.chengy.model.User;
import org.chengy.repository.SongRepository;
import org.chengy.repository.UserRepository;
import org.chengy.service.crawler.Crawler163music;
import org.chengy.service.statistics.Music163Statistics;
import org.json.JSONArray;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RunWith(SpringRunner.class)
@SpringBootTest
public class CrawlerApplicationTests {
	@Autowired
	Crawler163music crawler163music;

	@Autowired
	Music163Statistics music163Statistics;

	@Autowired
	SongRepository songRepository;

	@Autowired
	UserRepository userRepository;


	@Test
	public void contextLoads() {
	}

	@Test
	public void getUserInfoTest() {
		crawler163music.getUserInfo("73005221");
	}

	@Test
	public void getUserRencentSongTest() throws Exception {
		crawler163music.getUserRecentSong("330313");

	}

	@Test
	public void testCompoundIndex() {

		User user = new User();
		user.setCommunityId("262093432");
		user.setCommunity(Music163ApiCons.communityName);
		userRepository.save(user);

	}

	@Test
	public void getFans() throws Exception {
		List<String> ids =
				crawler163music.getFansId("330313");
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

		music163Statistics.getMostPopSong("datafile/popsong.txt");
	}

	@Test
	public void getRelativedUser() throws IOException {

		music163Statistics.relativedUser("330313");
	}


	@Test
	public void getMostPopLyricistTest() throws IOException, IllegalAccessException {
		music163Statistics.getMostPopLyricist("datafile/mostPopLyricist.txt", "lyricist");
	}

	@Test
	public void getMostPopComposerTest() throws IOException, IllegalAccessException {
		music163Statistics.getMostPopLyricist("datafile/mostPopComposer.txt", "composer");
	}

	@Test
	public void getSongBylyricist() throws IOException {

		music163Statistics.getLyricByLyricist("方文山");
	}


	@Test
	public void getSongPlayTimes() throws Exception {
		music163Statistics.getTopSongforUser("330313");
	}

	@Test
	public void testJson() throws Exception {
		String songRecordParam = Music163ApiCons.getSongRecordALLParams("330313", 1, 100);
		Document document = EncryptTools.commentAPI(songRecordParam, Music163ApiCons.songRecordUrl);
		String jsonStr = document.text();
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(jsonStr);
		List<HashMap<String, Object>> hashMapList
				= objectMapper.readValue(jsonNode.findValue("allData").toString(),
				new TypeReference<List<HashMap<String, Object>>>() {
				});

		Map<String, Integer> recordInfo =
				hashMapList.stream().collect(Collectors.toMap(ob ->(((HashMap)ob.get("song")).get("id")).toString(), ob ->(Integer)ob.get("score")));

		System.out.println(recordInfo);


	}


}
