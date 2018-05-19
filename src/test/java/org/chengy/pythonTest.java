package org.chengy;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.assertj.core.util.FloatComparator;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.Song;
import org.chengy.model.User;
import org.chengy.repository.SongRepository;
import org.chengy.repository.UserRepository;
import org.chengy.service.analyzer.SongRecordAnalyzer;
import org.chengy.service.discovery.Music163Discovery;
import org.chengy.service.statistics.Music163Statistics;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.*;
import java.util.stream.Collectors;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class pythonTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(pythonTest.class);

    @Value("${profile}")
    String env;
    @Autowired
    Music163Discovery music163Discovery;
    @Autowired
    Music163Statistics music163Statistics;
    @Autowired
    SongRecordAnalyzer songRecordAnalyzer;

    @Autowired
    SongRepository songRepository;
    @Autowired
    UserRepository userRepository;

    @Test
    public void loggerTest() {
        LOGGER.trace("trace logger");
        LOGGER.debug("debug logger");
        LOGGER.info("info logger");
        LOGGER.warn("warn logger");
        LOGGER.error("error logger");
    }


    @Test
    public void getUserRelativeSongTest() throws Exception {
        Map<String, Double> map = music163Statistics.getUserRelativeSong("330313", 10);
        List<Song> songList = songRepository.findSongsByCommunityIdInAndCommunity(map.keySet(), Music163ApiCons.communityName);

        Map<String, Double> scoreMap = songRecordAnalyzer.getSongAverageScore(map.keySet());
        System.out.println("======tf-idf score======");
        Map<String, Double> songInfo = songList.stream().collect(Collectors.toMap(ob -> ob.getTitle(), ob -> map.get(ob.getCommunityId())));
        System.out.println(songInfo);
        System.out.println("======average love score======");
        songInfo = songList.stream().collect(Collectors.toMap(ob -> ob.getTitle(), ob -> scoreMap.get(ob.getCommunityId())));
        System.out.println(songInfo);
    }

    @Test
    public void getSimilarSongsTest() throws JsonProcessingException {
        System.out.println(music163Discovery.getSimilarSongs("29561077", 20));
    }


    @Test
    public void getUserBasedRecommendTest() {
        String uid = "330313";
        System.out.println(env);
        List<Song> songList = music163Discovery.userBasedRecommend(uid, 200, 20);
        System.out.println("====== user recommend======" + songList);
        songList = music163Discovery.itemBasedRecommend(uid, 200, 80);
        System.out.println("====== item recommend======" + songList);
        songList = music163Discovery.getRecommendSongs(uid, 200, 20);
        System.out.println("====== model recommend======" + songList);
    }


    @Test
    public void testScore() {


    }

    @Test
    public void testRecommend() {
        List<Song> songList = music163Discovery.getRecommendSongs("330313", 100, 10);
        System.out.println(songList);
    }


    public static void main(String[] args) {
        Process process;
        try {
            String argv1 = "330313";
            String argv2 = "1428889750,304528598,261810251,442709295,123163229,353085612";
            String filePath = "/Users/chengxiaoy/PycharmProjects/abracadabra/music163/user/user_recommend.py";
            String[] argvs = new String[]{"python", filePath, argv1, argv2};
            process = Runtime.getRuntime().exec(argvs);
            //   process.waitFor();
            BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s;
            while ((s = stdOut.readLine()) != null) {
                System.out.println(s);
            }
            int result = process.waitFor();
            System.out.println(result);
            process.destroy();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
