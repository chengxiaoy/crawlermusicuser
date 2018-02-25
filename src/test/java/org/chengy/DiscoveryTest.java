package org.chengy;

import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.Song;
import org.chengy.model.SongRecord;
import org.chengy.repository.SongRecordRepository;
import org.chengy.repository.SongRepository;
import org.chengy.service.discovery.Music163Discovery;
import org.chengy.service.statistics.Music163Statistics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by nali on 2017/12/24.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DiscoveryTest {
	@Autowired
	Music163Discovery music163Discovery;
	@Autowired
	Music163Statistics music163Statistics;
	@Autowired
	SongRepository songRepository;
	@Autowired
	SongRecordRepository songRecordRepository;

	@Test
	public void getDiscoverySongtest() throws Exception {
		List<Song> songList =
				music163Discovery.getDiscoverySong("252839335");

		music163Discovery.getSongInfo(songList);
	}

	@Test
	public void getTopicSong() throws Exception {

		String uid = "330313";


		Map<String, Double> stringDoubleMap =
				music163Statistics.getRelativeSongByAlldata(uid, 20);

		List<Song> songList =
				songRepository.findSongsByCommunityIdInAndCommunity(stringDoubleMap.keySet(), Music163ApiCons.communityName);
		Map<String, Song> songMap = songList.stream().collect(Collectors.toMap(ob -> ob.getCommunityId(), ob1 -> ob1));


		List<SongRecord> songRecordList =
				songRecordRepository.findSongRecordsByCommunityIdInAndCommunity(stringDoubleMap.keySet(), Music163ApiCons.communityName);

		songRecordList = songRecordList.stream()
				.sorted((ob1, ob2) -> -(int) (ob1.getScore() / ob1.getLoveNum() - ob2.getScore() / ob2.getLoveNum()))
				.collect(Collectors.toList());

		File file = new File("datafile/topicSongFor" + uid + ".txt");

		if (!file.getParentFile().exists()) {
			file.getParentFile().mkdirs();
		}
		if (!file.exists()) {
			file.createNewFile();
		}


		FileWriter fileWriter = new FileWriter(file);

		for (SongRecord songRecord : songRecordList) {

			Song song = songMap.get(songRecord.getCommunityId());

			fileWriter.write(song.getTitle() + "\t" + song.getArts().get(0) + "\t" + stringDoubleMap.get(song.getCommunityId()));
			if (songRecord != null) {
				fileWriter.write("\t" + songRecord.getLoveNum() + "\t" + songRecord.getScore() + "\t" + songRecord.getScore() / songRecord.getLoveNum());
			}
			fileWriter.write("\n");
		}

		fileWriter.close();

	}

}
