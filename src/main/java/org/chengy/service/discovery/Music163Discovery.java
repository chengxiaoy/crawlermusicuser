package org.chengy.service.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.Song;
import org.chengy.model.SongRecord;
import org.chengy.model.User;
import org.chengy.repository.SongRecordRepository;
import org.chengy.repository.SongRepository;
import org.chengy.repository.UserRepository;
import org.chengy.service.crawler.Crawler163music;
import org.chengy.service.statistics.Music163Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nali on 2017/12/3.
 */
@Service
public class Music163Discovery {
	@Autowired
	UserRepository userRepository;
	@Autowired
	SongRepository songRepository;
	@Autowired
	Music163Statistics music163Statistics;
	@Autowired
	Crawler163music crawler163music;

	@Autowired
	SongRecordRepository songRecordRepository;


	public List<Song> getDiscoverySong(String uid) throws Exception {
		List<User> relativedUser = relativedUser(uid);

		List<String> songidList = relativedUser.stream().map(ob -> ob.getLoveSongId().get(0)).collect(Collectors.toList());
		User user = userRepository.findByCommunityIdAndCommunity(uid, Music163ApiCons.communityName);
		if (user != null) {
			songidList.removeAll(user.getLoveSongId());
		}
		List<String> recentSongids = crawler163music.getUserRecentSong(uid);
		songidList.removeAll(recentSongids);

		List<SongRecord> songRecordList = songRecordRepository.findSongRecordsByCommunityIdInAndCommunity(songidList, Music163ApiCons.communityName);
		songidList = songRecordList.stream().sorted((ob1, ob2) -> (-ob1.getLoveNum() + ob2.getLoveNum())).limit(5).map(ob -> ob.getCommunityId()).collect(Collectors.toList());
		return songRepository.findSongsByCommunityIdInAndCommunity(songidList, Music163ApiCons.communityName);
	}

	/**
	 * 随机读取1000个人 根据歌曲获取相关度较高的人
	 * 获取相关度较高的用户
	 *
	 * @param userId
	 * @throws IOException
	 */
	public List<User> relativedUser(String userId) throws Exception {

		//	User user = userRepository.findByCommunityIdAndCommunity(userId, Music163ApiCons.communityName);

		String filename = "datafile/sameUserfor" + userId + ".txt";
		File file = new File(filename);
		file.createNewFile();


		//List<String> songList = user.getLoveSongId();
		List<String> songList = new ArrayList<>(music163Statistics.getRelativeSongByAlldata(userId, 20).keySet());
		songList.addAll(music163Statistics.getRelativeSongByWeekdata(userId, 10).keySet());


		Random random = new Random();
		int pageIndex = random.nextInt(400);
		System.out.println("===========" + pageIndex);
		int pageSize = 1000;
		List<User> userList = userRepository.findAll(new PageRequest(pageIndex, pageSize)).getContent();
		Map<String, Integer> userIdInfo = new HashMap<>();
		Map<String, List<String>> userSongInfo = new HashMap<>();

		for (User other : userList) {
			List<String> userSongs = other.getLoveSongId();
			userIdInfo.put(other.getCommunityId(), getIntersectionNum(songList, userSongs));
			userSongInfo.put(other.getCommunityId(), userSongs);
		}

		List<Map.Entry<String, Integer>> entries =
				userIdInfo.entrySet().stream().sorted((ob1, ob2) -> -(ob1.getValue() - ob2.getValue())).collect(Collectors.toList());


		List<String> relativeUser = new ArrayList<>();
		int i = 0;
		for (Map.Entry<String, Integer> entry : entries) {
			relativeUser.add(entry.getKey());
			i++;
			if (i == 20) {
				break;
			}
		}

		List<User> relativeUserList = userRepository.findUsersByCommunityIdInAndCommunity(relativeUser, Music163ApiCons.communityName);

//写文件
		try (FileWriter fileWriter = new FileWriter(file)) {
			for (User item : relativeUserList) {
				fileWriter.write(item.getUsername());
				fileWriter.write("\t");
				String communityId = item.getCommunityId();
				fileWriter.write(communityId);
				fileWriter.write("\t");
				List<Song> relativeSong = songRepository.findSongsByCommunityIdInAndCommunity(userSongInfo.get(item.getCommunityId()), Music163ApiCons.communityName);
				for (Song song : relativeSong) {
					fileWriter.write(song.getTitle());
					fileWriter.write("\t");
				}
				fileWriter.write("\n");
			}
		}
		return relativeUserList;
	}

	/**
	 * 获取歌曲的交集
	 *
	 * @param idList1
	 * @param idList2
	 * @return
	 */
	int getIntersectionNum(List<String> idList1, List<String> idList2) {
		int i = 0;
		List<String> intersection = new ArrayList<>();
		HashSet<String> set = new HashSet<>(idList1);
		for (String id : idList2) {
			if (set.contains(id)) {
				i++;
				intersection.add(id);
			}
		}
		idList2.clear();
		idList2.addAll(intersection);
		return i;
	}
}
