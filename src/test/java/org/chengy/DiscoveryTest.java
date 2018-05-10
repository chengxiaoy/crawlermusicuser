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
import org.springframework.test.context.ActiveProfiles;
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
@ActiveProfiles("test")
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
    public void getUserRelativedSong() throws Exception {


        Map<String, Double> topSongMap = music163Statistics.getUserRelativeSong("330313", 5);

        System.out.println(topSongMap);

    }

}
