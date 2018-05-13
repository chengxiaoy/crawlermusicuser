package org.chengy;

import org.assertj.core.util.FloatComparator;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.Song;
import org.chengy.model.User;
import org.chengy.repository.UserRepository;
import org.chengy.service.discovery.Music163Discovery;
import org.junit.Test;
import org.junit.runner.RunWith;
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

    @Value("${profile}")
    String env;
    @Autowired
    Music163Discovery music163Discovery;

    @Autowired
    UserRepository userRepository;

    @Test
    public void getUserBasedRecommendTest() {
        String uid="250038717";
        System.out.println(env);
        List<Song> songList = music163Discovery.userBasedRecommend(uid, 100, 10);
        System.out.println("====== user recommend======" + songList);
        songList = music163Discovery.itemBasedRecommend(uid, 100, 10);
        System.out.println("====== item recommend======" + songList);
        songList = music163Discovery.getRecommendSongs(uid, 100, 10);
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
