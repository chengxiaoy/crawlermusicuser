package org.chengy;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chengy.infrastructure.music163secret.EncryptTools;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.Song;
import org.chengy.model.SongRecord;
import org.chengy.model.User;
import org.chengy.repository.SongRecordRepository;
import org.chengy.repository.SongRepository;
import org.chengy.repository.UserRepository;
import org.chengy.service.crawler.music163.Crawler163music;
import org.chengy.service.discovery.Music163Discovery;
import org.chengy.service.statistics.Music163Statistics;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.*;
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

	@Autowired
	SongRecordRepository songRecordRepository;
	@Autowired
	Music163Discovery music163Discovery;


	@Test
	public void contextLoads() {
	}

	@Test
	public void getUserInfoTest() {
		crawler163music.getUserInfo(Arrays.asList("250038717"));
	}

	@Test
	public void getUserRencentSongTest() throws Exception {
		crawler163music.getUserRecentSong("250038717");

	}

	@Test
	public void getPlayList() throws Exception {
		String playListParams = Music163ApiCons.getPlayListParams("250038717", 1, 10);
		Document document = EncryptTools.commentAPI(playListParams, Music163ApiCons.playListUrl);

		String json=document.text();
		ObjectMapper objectMapper=new ObjectMapper();
		JsonNode jsonNode=objectMapper.readTree(json);
		List<JsonNode> jsonNodeList=jsonNode.findValue("playlist").findValues("id");
		String playListid=String.valueOf(jsonNodeList.get(0).asInt());
		System.out.println(playListid);

		String playDetail=Music163ApiCons.getPlayListDetailParam(playListid, 1, 10);
		document=EncryptTools.commentAPI(playDetail,Music163ApiCons.playListDetailUrl);

		System.out.println(document);

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
	public void getRelativedUser() throws Exception {

		music163Discovery.relativedUser("330313");
	}

	@Test
	public void getDiscoverySong() throws Exception {
		List<Song> songList = music163Discovery.getDiscoverySong("330313");

		System.out.println(songList.stream().map(ob -> ob.getTitle() + showArts(ob.getArts())).collect(Collectors.toList()));
	}


	public String showArts(List<String> arts) {
		String str = "";
		for (String s : arts) {
			str = str + " " + s + " ";
		}
		return str;
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
		music163Statistics.getSongRecord("330313");
	}

	@Test
	public void getRelativeSong() {
		try {
			Map<String, Double> score = music163Statistics.getRelativeSongByAlldata("330313", 20);
			List<Song> songList = songRepository.findSongsByCommunityIdInAndCommunity(new ArrayList<>(score.keySet()), Music163ApiCons.communityName);
			System.out.println(songList.stream().map(ob -> ob.getTitle()).collect(Collectors.toList()));

			List<SongRecord> songRecordList = songRecordRepository.findSongRecordsByCommunityIdInAndCommunity(songList.stream().map(ob -> ob.getCommunityId()).collect(Collectors.toList()), Music163ApiCons.communityName);


			score = music163Statistics.getRelativeSongByWeekdata("330313", 10);
			songList = songRepository.findSongsByCommunityIdInAndCommunity(new ArrayList<>(score.keySet()), Music163ApiCons.communityName);
			System.out.println(songList.stream().map(ob -> ob.getTitle()).collect(Collectors.toList()));

			songRecordList = songRecordRepository.findSongRecordsByCommunityIdInAndCommunity(songList.stream().map(ob -> ob.getCommunityId()).collect(Collectors.toList()), Music163ApiCons.communityName);


		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Test
	public void getWeekSong() throws Exception {
		String uid = "330313";
		String songRecordParam = Music163ApiCons.getSongRecordofWeek(uid, 1, 100);
		Document document = EncryptTools.commentAPI(songRecordParam, Music163ApiCons.songRecordUrl);

		String jsonStr = document.text();
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(jsonStr);
		List<HashMap<String, Object>> hashMapList
				= objectMapper.readValue(jsonNode.findValue("weekData").toString(),
				new TypeReference<List<HashMap<String, Object>>>() {
				});

		Map<String, Integer> recordInfo =
				hashMapList.stream().collect(Collectors.toMap(ob -> (((HashMap) ob.get("song")).get("id")).toString(), ob -> (Integer) ob.get("score")));

		System.out.println(recordInfo);

	}


}
