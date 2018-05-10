package org.chengy.service.discovery;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jdk.nashorn.internal.ir.WithNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.BaseEntity;
import org.chengy.model.Song;
import org.chengy.model.SongRecord;
import org.chengy.model.User;
import org.chengy.repository.SongRecordRepository;
import org.chengy.repository.SongRepository;
import org.chengy.repository.UserRepository;
import org.chengy.repository.matcher.UserMatcherFactory;
import org.chengy.service.crawler.music163.Crawler163music;
import org.chengy.service.statistics.Music163Statistics;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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


    public List<Song> getRecommendSongs(String uid, int n, int k) {
        Process process;

        List<User> userList = getRandomUsers(n);
        List<String> uidList =
                userList.stream().filter(ob -> ob.getSongScore().size() > 0).map(BaseEntity::getCommunityId).collect(Collectors.toList());
        String randomUserIds = String.join(",", uidList);


        String argv1 = uid;
        String argv2 = randomUserIds;
        String argv3 = String.valueOf(k);
        String filePath = "/Users/chengxiaoy/PycharmProjects/abracadabra/music163/user/recommend.py";
        String[] argvs = new String[]{"python", filePath, argv1, argv2, argv3};

        String s = null;

        List<String> recommendSongIds = new ArrayList<>();
        try {
            process = Runtime.getRuntime().exec(argvs);
            BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
            while ((s = stdOut.readLine()) != null) {
                recommendSongIds = filterSongs(s, uid);
            }
            // 0是调用进程正常
            int result = 0;
            result = process.waitFor();
            System.out.println(result);
            process.destroy();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }

        return songRepository.findSongsByCommunityIdInAndCommunity(recommendSongIds, Music163ApiCons.communityName);

    }

    /**
     * 过滤掉已经喜欢的歌曲
     *
     * @param s
     * @param uid
     * @return
     */
    public List<String> filterSongs(String s, String uid) {
        User user = userRepository.findByCommunityIdAndCommunity(uid, Music163ApiCons.communityName);
        s = s.substring(1, s.length() - 1);
        Set<String> songIdSet =
                Arrays.stream(s.split(",")).map(ob -> ob.trim()).collect(Collectors.toSet());
        songIdSet.removeAll(user.getLoveSongId());
        return new ArrayList<>(songIdSet);
    }


    /**
     * 随机读取1000个人 根据歌曲获取相关度（余弦）较高的人
     * 获取相关度较高的用户
     *
     * @param userId
     * @throws IOException
     */
    public ScoreReport relativedUser(String userId) throws Exception {

        User user = userRepository.findByCommunityIdAndCommunity(userId, Music163ApiCons.communityName);

        String filename = "datafile/" + user.getUsername() + "_" + System.currentTimeMillis() + ".txt";
        File file = new File(filename);
        file.createNewFile();


        List<User> userList = getRandomUsers(1000);

        Map<String, List<String>> songMap = userList.stream()
                .collect(Collectors.toMap(BaseEntity::getCommunityId, ob -> getIntersectionSongs(user, ob)));
        List<String> uids = userList.stream().filter(ob -> ob.getSongScore()
                .size() > 10).map(BaseEntity::getCommunityId).collect(Collectors.toList());


        Map<String, Float> userScores = getScores(userId, uids);
        try (FileWriter fileWriter = new FileWriter(file)) {
            fileWriter.write(userScores.toString());
        }
        ScoreReport report = new ScoreReport();
        report.setSimilarScore(userScores);
        report.setIntersectionSongs(songMap);
        return report;
    }


    public class ScoreReport {
        private Map<String, Float> similarScore;
        private Map<String, List<String>> intersectionSongs;

        public Map<String, Float> getSimilarScore() {
            return similarScore;
        }

        public void setSimilarScore(Map<String, Float> similarScore) {
            this.similarScore = similarScore;
        }

        public Map<String, List<String>> getIntersectionSongs() {
            return intersectionSongs;
        }

        public void setIntersectionSongs(Map<String, List<String>> intersectionSongs) {
            this.intersectionSongs = intersectionSongs;
        }
    }


    List<String> getIntersectionSongs(User target, User uid2) {
        List<String> res = new ArrayList<>(target.getLoveSongId());
        res.retainAll(uid2.getLoveSongId());
        return res;
    }

    public Map<String, Float> getScores(String uid1, List<String> candidateIds) {
        Process process;
        String s = "0";
        try {

            String filePath = "/Users/chengxiaoy/PycharmProjects/abracadabra/music163/user/facade.py";
            String candidateIdStr = String.join(",", candidateIds);

            String[] argvs = new String[]{"python", filePath, uid1, candidateIdStr};
            process = Runtime.getRuntime().exec(argvs);
            BufferedReader stdOut = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String s1 = "";
            Map<String, Float> res = new HashMap<>();
            while ((s1 = stdOut.readLine()) != null) {
                System.out.println(s1);
                s1 = s1.substring(1, s1.length() - 1);
                List<String> scoreList = Arrays.stream(s1.split(","))
                        .map(String::trim).collect(Collectors.toList());
                for (int i = 0; i < candidateIds.size(); i++) {
                    res.put(candidateIds.get(i), Float.valueOf(scoreList.get(i)));
                }
            }
            process.waitFor();
            process.destroy();
            return res;
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return Maps.newHashMap();
        }

    }

    public List<User> getRandomUsers(int size) {
        long count = userRepository.count(UserMatcherFactory.music163BasicUserMatcher());
        int pageCount = (int) (count / size);
        Random r = new Random();
        int pageId = r.nextInt(pageCount);
        List<User> userList = userRepository.findAll(UserMatcherFactory.music163BasicUserMatcher(), new PageRequest(pageId, size)).getContent();
        return userList;
    }


}
