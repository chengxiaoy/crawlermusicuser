package org.chengy;


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
    Music163SongRepository songRepository;
    @Autowired
    Vertx163Muisc vertx163Muisc;



    @Test
    public void getSongRecord() {
        songRecordAnalyzer.getSongRecordInfo();
    }

}
