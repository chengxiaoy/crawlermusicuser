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
    public void testScore() {

        Set<String> uidSet =
                music163Discovery.getRandomUsers(100).stream().filter(ob -> ob.getSongScore().size() > 0).map(ob -> ob.getCommunityId()).collect(Collectors.toSet());

        System.out.println(uidSet);
    }

    @Test
    public void testRecommend() {
        List<Song> songList = music163Discovery.getRecommendSongs("330313", 100, 10);
        System.out.println(songList);
    }


    @Test
    public void testPython() throws Exception {
        System.out.println(env);
        Music163Discovery.ScoreReport scoreReport = music163Discovery.relativedUser("330313");
        List<Map.Entry<String, Float>> entryList = scoreReport.getSimilarScore().entrySet().stream().sorted(Collections.reverseOrder(Comparator.comparing(Map.Entry::getValue)))
                .limit(10).collect(Collectors.toList());

        System.out.println(entryList);

        List<Map.Entry<String, List<String>>> songList = scoreReport.getIntersectionSongs().entrySet().stream().sorted(Collections.reverseOrder(Comparator.comparing(ob -> ob.getValue().size())))
                .limit(10).collect(Collectors.toList());
        System.out.println(songList);

    }

    public static void main(String[] args) {
        Process process;
        try {
            String argv1 = "330313";
            String argv2 = "556726207";
            String filePath = "/Users/chengxiaoy/PycharmProjects/abracadabra/music163/user/facade.py";
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
