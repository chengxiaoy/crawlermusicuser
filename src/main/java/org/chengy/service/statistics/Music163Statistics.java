package org.chengy.service.statistics;

import ch.qos.logback.core.joran.util.beans.BeanUtil;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.Song;
import org.chengy.model.User;
import org.chengy.repository.SongRepository;
import org.chengy.repository.UserRepository;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
				Field field= FieldUtils.getField(Song.class,fieldname,true);

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

	/**
	 * 随机读取1000个人 根据歌曲获取相关度较高的人
	 * 获取相关度较高的用户
	 *
	 * @param userId
	 * @throws IOException
	 */
	public void relativedUser(String userId) throws IOException {

		User user = userRepository.findByCommunityIdAndCommunity(userId, Music163ApiCons.communityName);

		String filename = "datafile/sameUserfor" + user.getUsername() + ".txt";
		File file = new File(filename);
		file.createNewFile();


		List<String> songList = user.getLoveSongId();
		Random random = new Random();
		int pageIndex = random.nextInt(190);
		System.out.println("===========" + pageIndex);
		int pageSize = 1000;
		List<User> userList = userRepository.findAll(new PageRequest(pageIndex, pageSize)).getContent();
		Map<String, Integer> map = new HashMap<>();
		for (User other : userList) {
			map.put(other.getCommunityId(), getIntersectionNum(songList, other.getLoveSongId()));
		}

		List<Map.Entry<String, Integer>> entries =
				map.entrySet().stream().sorted((ob1, ob2) -> -(ob1.getValue() - ob2.getValue())).collect(Collectors.toList());


		List<String> relativeUser = new ArrayList<>();
		int i = 0;
		for (Map.Entry<String, Integer> entry : entries) {
			relativeUser.add(entry.getKey());
			i++;
			if (i == 10) {
				break;
			}
		}

		List<User> relativeUserList = userRepository.findByCommunityIdContainsAndCommunity(relativeUser, Music163ApiCons.communityName);
		try (FileWriter fileWriter = new FileWriter(file)) {
			for (User item : relativeUserList) {
				fileWriter.write(item.getUsername());
				fileWriter.write("\t");
				fileWriter.write(item.getCommunityId());
				fileWriter.write("\n");
			}
		}
	}


	private void generatePotentialGF(String filename) throws IOException {
		File file = new File(filename);
		BufferedReader reader = new BufferedReader(new FileReader(file));
		FileWriter fileWriter = new FileWriter(new File(filename + "pro"));


		String line = null;
		int i = 0;
		while (i++ < 10) {
			line = reader.readLine();
			String[] hehe = line.trim().split("\t");

		}


	}

	int getIntersectionNum(List<String> idList1, List<String> idList2) {
		int i = 0;
		HashSet<String> set = new HashSet<>(idList1);
		for (String id : idList2) {
			if (set.contains(id)) {
				i++;
			}
		}
		return i;
	}


}
