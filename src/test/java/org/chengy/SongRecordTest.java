package org.chengy;


import org.apache.catalina.LifecycleState;
import org.chengy.model.Music163Song;
import org.chengy.model.Music163User;
import org.chengy.repository.remote.Music163SongRepository;
import org.chengy.repository.remote.Music163UserRepository;
import org.chengy.service.analyzer.SongRecordAnalyzer;
import org.chengy.service.crawler.music163.Vertx163Muisc;
import org.chengy.service.discovery.Music163Discovery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class SongRecordTest {

	@Value("${profile}")
	String env;
	@Autowired
	Music163Discovery music163Discovery;

	@Autowired
	Music163UserRepository userRepository;

	@Autowired
	org.chengy.repository.local.LocalMusic163UserRepository localUserRepository;

	@Autowired
	org.chengy.repository.local.LocalMusic163SongRepository localSongRepository;
	@Autowired
	SongRecordAnalyzer songRecordAnalyzer;

	@Autowired
	Music163SongRepository songRepository;
	@Autowired
	Vertx163Muisc vertx163Muisc;


	@Test
	public void transferData() {

		transferSong();
	}

	public void transferUser() {
		int pageId = 0;
		int pageSize = 1000;
		List<Music163User> music163Users = localUserRepository.findAll(PageRequest.of(pageId++, pageSize)).getContent();
		while (music163Users.size() > 0) {
			for (Music163User user : music163Users) {
				if (!userRepository.existsById(user.getId())) {
					userRepository.save(user);
				}
			}
			music163Users = localUserRepository.findAll(PageRequest.of(pageId++, pageSize)).getContent();
		}
	}

	public void transferSong() {
		int pageId = 0;
		int pageSize = 1000;
		List<Music163Song> music163Songs = localSongRepository.findAll(PageRequest.of(pageId++, pageSize)).getContent();
		while (music163Songs.size() > 0) {
			for (Music163Song song : music163Songs) {
				if (songRepository.existsById(song.getId())) {
					songRepository.save(song);
				}
			}
			music163Songs = localSongRepository.findAll(PageRequest.of(pageId++, pageSize)).getContent();
		}
	}


	@Test
	public void getSongRecord() {
		songRecordAnalyzer.getSongRecordInfo();
	}

}
