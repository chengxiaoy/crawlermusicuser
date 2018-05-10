package org.chengy;


import org.chengy.repository.UserRepository;
import org.chengy.service.analyzer.SongRecordAnalyzer;
import org.chengy.service.crawler.CrawlerLauncher;
import org.chengy.service.discovery.Music163Discovery;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class SongRecordTest {

    @Value("${profile}")
    String env;
    @Autowired
    Music163Discovery music163Discovery;

    @Autowired
    UserRepository userRepository;
    @Autowired
    SongRecordAnalyzer songRecordAnalyzer;


    @Test
    public void getSongRecord() {
        songRecordAnalyzer.getSongRecordInfo();
    }

}
