package org.chengy;


import org.chengy.model.Music163Song;
import org.chengy.model.Music163User;
import org.chengy.repository.local.SongRepository;
import org.chengy.repository.local.UserRepository;
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
    SongRecordAnalyzer songRecordAnalyzer;
    @Autowired
    UserRepository localUserRepository;
    @Autowired
    SongRepository localSongRepository;
    @Autowired
    Music163SongRepository songRepository;
    @Autowired
    Vertx163Muisc vertx163Muisc;

    @Test
    public void vertxTest() throws InterruptedException {

        vertx163Muisc.getCrawlerInfo("330313", true, false);
       // vertx163Muisc.getLoveSongs("330313");

        Thread.sleep(1000 * 60);
    }


    @Test
    public void dataTransfer() {
        transferSong();
        transferUser();
    }

    @Test
    public void transferUser() {
        int pageId = 0;
        int pageSize = 10000;


        List<Music163User> music163Users = localUserRepository.findAll(new PageRequest(pageId++, pageSize)).getContent();
        while (music163Users.size() > 0) {
            userRepository.saveAll(music163Users);
            music163Users = localUserRepository.findAll(new PageRequest(pageId++, pageSize)).getContent();
        }

        System.out.println("transfer user data over");
    }

    @Test
    public void transferSong() {
        int pageId = 0;
        int pageSize = 1000;


        List<Music163Song> music163Songs = localSongRepository.findAll(new PageRequest(pageId++, pageSize)).getContent();
        while (music163Songs.size() > 0) {
            songRepository.saveAll(music163Songs);
            music163Songs = localSongRepository.findAll(new PageRequest(pageId++, pageSize)).getContent();
        }

        System.out.println("transfer song data over");
    }


    @Test
    public void getSongRecord() {
        songRecordAnalyzer.getSongRecordInfo();
    }

}
