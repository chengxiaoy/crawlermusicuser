package org.chengy.service.statistics;

import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.Song;
import org.chengy.model.User;
import org.chengy.repository.SongRepository;
import org.chengy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import sun.jvm.hotspot.runtime.VM;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
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
	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	/**
	 * 查询被喜欢次数最多的歌曲的歌曲
	 */
	public void getMostPopSong(String filename) throws IOException {

		File file = new File(filename);
		if (!file.exists()) {
			file.createNewFile();
		}


		ConcurrentHashMap<String, Integer> concurrentHashMap = new ConcurrentHashMap<>();

		int pageIndex = 1;
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


	public void relativedUser(String userId) throws IOException {

		String filename = "datafile/sameUserfor" + ".txt";
		File file = new File(filename);
		file.createNewFile();


		User user = userRepository.findByCommunityIdAndCommunity(userId, Music163ApiCons.communityName);
		List<String> songList = user.getLoveSongId();


		Random random = new Random();
		int pageIndex = random.nextInt(200);
		System.out.println("==========="+pageIndex);
		int pageSize = 1000;
		List<User> userList = userRepository.findAll(new PageRequest(pageIndex, pageSize)).getContent();
		Map<String, Integer> map = new HashMap<>();
		for (User other : userList) {
			map.put(other.getCommunityId(), getIntersectionNum(songList, other.getLoveSongId()));
		}

		List<Map.Entry<String, Integer>> entries =
				map.entrySet().stream().sorted((ob1, ob2) -> -(ob1.getValue() - ob2.getValue())).collect(Collectors.toList());

		try (FileWriter fileWriter = new FileWriter(file)) {
			for (Map.Entry<String, Integer> entry : entries) {
				fileWriter.write(entry.getKey());
				fileWriter.write("\t");
				fileWriter.write(entry.getValue().toString());
				fileWriter.write("\n");
			}
		}
	}

	int getIntersectionNum(List<String> idList1, List<String> idList2) {
		int i = 0;
		for (String id : idList2) {
			if (idList1.contains(id)) {
				i++;
			}
		}
		return i;
	}


}
