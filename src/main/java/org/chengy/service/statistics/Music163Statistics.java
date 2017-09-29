package org.chengy.service.statistics;

import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.Song;
import org.chengy.model.User;
import org.chengy.repository.SongRepository;
import org.chengy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import sun.jvm.hotspot.runtime.VM;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
	public void getMostPopSong() {

		ConcurrentHashMap<String, Integer> concurrentHashMap = new ConcurrentHashMap<>();

		int pageIndex = 1;
		int pageSize = 100;
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
				concurrentHashMap.entrySet().stream().sorted((ob1,ob2)->-(ob1.getValue()-ob2.getValue())).collect(Collectors.toList());

		for (int i = 0; i < 100; i++) {
			List<Song> songList =
					songRepository.findSongByCommunityIdAndCommunity(sorted.get(i).getKey(), Music163ApiCons.communityName);
			if (songList.size() > 0) {
				System.out.println(songList.get(0).getTitle() + "========" + sorted.get(i).getValue());
			} else {
				System.out.println(sorted.get(i).getKey() + "=========" + sorted.get(i).getValue());
			}

		}


	}


}
