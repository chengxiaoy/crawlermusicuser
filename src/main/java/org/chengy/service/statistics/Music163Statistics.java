package org.chengy.service.statistics;

import ch.qos.logback.core.joran.util.beans.BeanUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.sun.org.apache.bcel.internal.generic.NEW;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.chengy.infrastructure.music163secret.EncryptTools;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.infrastructure.music163secret.SongRecordFactory;
import org.chengy.model.BaseEntity;
import org.chengy.model.Song;
import org.chengy.model.SongRecord;
import org.chengy.model.User;
import org.chengy.repository.SongRecordRepository;
import org.chengy.repository.SongRepository;
import org.chengy.repository.UserRepository;
import org.chengy.service.crawler.Crawler163music;
import org.jsoup.nodes.Document;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by nali on 2017/9/29.
 */

@SuppressWarnings("Duplicates")
@Component
public class Music163Statistics {
	@Autowired
	UserRepository userRepository;
	@Autowired
	SongRepository songRepository;
	@Autowired
	SongRecordRepository songRecordRepository;
	@Autowired
	ObjectMapper objectMapper;



	/**
	 * 将作词家的歌词写入文件
	 *
	 * @param lyricist
	 * @throws IOException
	 */
	public void getLyricByLyricist(String lyricist) throws IOException {
		List<Song> songList =
				songRepository.findSongsByLyricist(lyricist);
		String dirPath = "datafile" + File.separator + "song" + File.separator + lyricist;
		File dir = new File(dirPath);
		if (!dir.exists()) {
			dir.mkdirs();
		}
		for (Song song : songList) {
			String songtitle = song.getTitle();
			songtitle = songtitle.replace("/", "／");
			File file = new File(dirPath + File.separator + songtitle);
			if (!file.exists()) {
				System.out.println(file.getAbsolutePath());
				file.createNewFile();
				try (FileWriter fileWriter = new FileWriter(file)) {
					fileWriter.write(song.getLyric());
				}
			}
		}
	}

	/**
	 * 将最受欢迎的作词人／作曲人写入到文件中
	 *
	 * @param filename
	 * @throws IOException
	 */
	public void getMostPopLyricist(String filename, String fieldname) throws IOException, IllegalAccessException {
		File file = new File(filename);
		if (!file.exists()) {
			file.createNewFile();
		}
		HashMap<String, Integer> map = new HashMap<>();

		int pageIndex = 0;
		int pageSize = 1000;
		List<Song> songList = songRepository.findAll(new PageRequest(pageIndex, pageSize)).getContent();
		while (songList.size() > 0) {
			for (Song song : songList) {
//				Field field =
//						FieldUtils.getField(Song.class, fieldname);
//				field.get(song);
				Field field = FieldUtils.getField(Song.class, fieldname, true);

				String key = field.get(song).toString();
				if (key != null && !key.equals("")) {
					if (map.keySet().contains(key)) {
						map.put(key, map.get(key) + 1);
					} else {
						map.put(key, 1);
					}
				}
			}
			songList = songRepository.findAll(new PageRequest(++pageIndex, pageSize)).getContent();
		}
		List<Map.Entry<String, Integer>> sorted =
				map.entrySet().stream().sorted((ob1, ob2) -> -(ob1.getValue() - ob2.getValue())).collect(Collectors.toList());

		Iterator<Map.Entry<String, Integer>> iterator = sorted.iterator();
		try (FileWriter fileWriter = new FileWriter(file)) {
			for (int i = 0; i < 100; i++) {
				Map.Entry<String, Integer> entry = iterator.next();
				fileWriter.write(entry.getKey());
				fileWriter.write("\t");
				fileWriter.write(entry.getValue().toString());
				fileWriter.write("\n");
			}
		}
	}

	/**
	 * 查询被喜欢次数最多的歌曲的歌曲
	 */
	public void getMostPopSong(String filename) throws IOException {

		File file = new File(filename);
		if (!file.exists()) {
			file.createNewFile();
		}


		ConcurrentHashMap<String, Integer> concurrentHashMap = new ConcurrentHashMap<>();

		int pageIndex = 0;
		int pageSize = 1000;
		List<User> userList = userRepository.findAll(new PageRequest(pageIndex, pageSize)).getContent();
		while (userList.size() > 0) {
			for (User user : userList) {
				List<String> songIds = user.getLoveSongId();
				for (String id : songIds) {
					if (concurrentHashMap.containsKey(id)) {
						concurrentHashMap.put(id, concurrentHashMap.get(id) + 1);
					} else {
						concurrentHashMap.put(id, 1);
					}
				}
			}
			userList = userRepository.findAll(new PageRequest(++pageIndex, pageSize)).getContent();
		}

		List<Map.Entry<String, Integer>> sorted =
				concurrentHashMap.entrySet().stream().sorted((ob1, ob2) -> -(ob1.getValue() - ob2.getValue())).collect(Collectors.toList());
		FileWriter fileWriter = new FileWriter(file);

		for (int i = 0; i < 100; i++) {
			Song song =
					songRepository.findSongByCommunityIdAndCommunity(sorted.get(i).getKey(), Music163ApiCons.communityName);

			if (song != null) {
				fileWriter.write(song.getTitle());
				fileWriter.write("\t");
				fileWriter.write(song.getArts().get(0));
				fileWriter.write("\t");
				fileWriter.write(sorted.get(i).getValue().toString());
				fileWriter.write("\n");
			} else {
				fileWriter.write(sorted.get(i).getKey());
				fileWriter.write("\t");
				fileWriter.write(sorted.get(i).getValue().toString());
				fileWriter.write("\n");
			}

		}
		fileWriter.close();


	}




	public Map<String, Double> getRelativeSongByAlldata(String userid, int k) throws Exception {
		//总时间的喜欢的歌曲
		String songRecordParam = Music163ApiCons.getSongRecordALLParams(userid, 1, 100);
		Document document = EncryptTools.commentAPI(songRecordParam, Music163ApiCons.songRecordUrl);
		String jsonStr = document.text();
		JsonNode jsonNode = objectMapper.readTree(jsonStr);
		List<HashMap<String, Object>> hashMapList
				= objectMapper.readValue(jsonNode.findValue("allData").toString(),
				new TypeReference<List<HashMap<String, Object>>>() {
				});

		Map<String, Integer> recordInfo =
				hashMapList.stream().collect(Collectors.toMap(ob -> (((HashMap) ob.get("song")).get("id")).toString(), ob -> (Integer) ob.get("score")));

		return getUserRelativeSong(recordInfo, k);
	}

	public Map<String, Double> getRelativeSongByWeekdata(String userid, int k) throws Exception {
//最近一周听的歌曲
		String weekSongRecordParam = Music163ApiCons.getSongRecordofWeek(userid, 1, 100);
		Document document = EncryptTools.commentAPI(weekSongRecordParam, Music163ApiCons.songRecordUrl);

		String jsonStr = document.text();
		JsonNode jsonNode = objectMapper.readTree(jsonStr);
		List<HashMap<String, Object>> hashMapList
				= objectMapper.readValue(jsonNode.findValue("weekData").toString(),
				new TypeReference<List<HashMap<String, Object>>>() {
				});

		Map<String, Integer> weekRecordInfo =
				hashMapList.stream().collect(Collectors.toMap(ob -> (((HashMap) ob.get("song")).get("id")).toString(), ob -> (Integer) ob.get("score")));
		return getUserRelativeSong(weekRecordInfo, k);
	}

	/**
	 * 根据TF-IDF原理获取用户k条最具代表性的歌曲
	 */
	public Map<String, Double> getUserRelativeSong(Map<String, Integer> recordInfo, int k) throws Exception {


		long alldata =
				userRepository.countAllBySongRecordIsTrue();

		List<String> songids =
				new ArrayList<>(recordInfo.keySet());


		List<SongRecord> songRecordList = songRecordRepository.findSongRecordsByCommunityIdInAndCommunity(songids, Music163ApiCons.communityName);
		Map<String, SongRecord> songRecordMap = songRecordList.stream().collect(Collectors.toMap(BaseEntity::getCommunityId, ob -> ob));

		Map<String, Double> IDFmap = songRecordList.stream().collect(Collectors.toMap(ob1 -> ob1.getCommunityId(), ob2 -> recordInfo.get(ob2.getCommunityId())
				/ calculateIDF(alldata, (long) songRecordMap.get(ob2.getCommunityId()).getLoveNum())));


		Map<String, Double> relativeSong =
				IDFmap.entrySet().stream().sorted((ob1, ob2) -> {
					if (ob1.getValue() < ob2.getValue()) {
						return 1;
					}
					return -1;
				}).limit(k).collect(Collectors.toMap(ob1 -> ob1.getKey(), ob2 -> ob2.getValue()));

		return relativeSong;
	}

	public double calculateIDF(long alldata, long currentdata) {

		double f = (double) alldata / (double) currentdata;
		return Math.log(f);

	}


	/**
	 * 记录歌曲信息
	 *
	 * @param userid
	 * @throws Exception
	 */
	public void getSongRecord(String userid) throws Exception {

		User user = userRepository.findByCommunityIdAndCommunity(userid, Music163ApiCons.communityName);
		if (user.getSongRecord() != null) {
			return;
		}

		String songRecordParam = Music163ApiCons.getSongRecordALLParams(userid, 1, 100);
		Document document = EncryptTools.commentAPI(songRecordParam, Music163ApiCons.songRecordUrl);
		String jsonStr = document.text();
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode jsonNode = objectMapper.readTree(jsonStr);
		List<HashMap<String, Object>> hashMapList
				= objectMapper.readValue(jsonNode.findValue("allData").toString(),
				new TypeReference<List<HashMap<String, Object>>>() {
				});

		Map<String, Integer> recordInfo =
				hashMapList.stream().collect(Collectors.toMap(ob -> (((HashMap) ob.get("song")).get("id")).toString(), ob -> (Integer) ob.get("score")));

		for (Map.Entry<String, Integer> entry : recordInfo.entrySet()) {
			SongRecord songRecord =
					songRecordRepository.findSongRecordByCommunityIdAndCommunity(entry.getKey(), Music163ApiCons.communityName);

			if (songRecord == null) {
				SongRecord newSongRecord = SongRecordFactory.buildSongRecord(entry.getKey(), Music163ApiCons.communityName, 1, (long) entry.getValue());
				songRecordRepository.save(newSongRecord);
			} else {
				try {
					songRecord.setScore(songRecord.getScore() + entry.getValue());
					songRecord.setLoveNum(songRecord.getLoveNum() + 1);
					songRecordRepository.save(songRecord);
				} catch (OptimisticLockingFailureException e) {
					System.out.println("retry update songRecord");

					songRecord = songRecordRepository.findSongRecordByCommunityIdAndCommunity(songRecord.getCommunityId(), Music163ApiCons.communityName);
					songRecord.setScore(songRecord.getScore() + entry.getValue());
					songRecord.setLoveNum(songRecord.getLoveNum() + 1);
					songRecordRepository.save(songRecord);
				}

			}
		}

		user.setSongRecord(true);
		userRepository.save(user);
	}


}
