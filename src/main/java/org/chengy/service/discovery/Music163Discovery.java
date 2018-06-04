package org.chengy.service.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.ComparisonChain;
import com.google.common.collect.Lists;
import org.chengy.newmodel.BaseModel;
import org.chengy.newmodel.Music163Song;
import org.chengy.newmodel.Music163SongRecord;
import org.chengy.newmodel.Music163User;
import org.chengy.repository.remote.Music163SongRecordRepository;
import org.chengy.repository.remote.Music163SongRepository;
import org.chengy.repository.remote.Music163UserRepository;
import org.chengy.service.analyzer.SongRecordAnalyzer;
import org.chengy.service.crawler.music163.Crawler163music;
import org.chengy.service.statistics.J2PythonUtil;
import org.chengy.service.statistics.Music163Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nali on 2017/12/3.
 */
@Service
public class Music163Discovery {

    private static final Logger LOGGER = LoggerFactory.getLogger(Music163Discovery.class);

    @Autowired
    Music163UserRepository userRepository;
    @Autowired
    Music163SongRepository songRepository;
    @Autowired
    Music163Statistics music163Statistics;
    @Autowired
    Crawler163music crawler163music;

    @Autowired
    Music163SongRecordRepository songRecordRepository;

    @Autowired
    SongRecordAnalyzer songRecordAnalyzer;

    private static final String pythonFile = "/Users/chengxiaoy/PycharmProjects/abracadabra/music163/user/facade.py";


    /**
     * 使用基于模型的协同过滤 funk-SVD 获得推荐歌曲
     *
     * @param uid 推荐人uid
     * @param n
     * @param k   推荐top k首歌曲
     * @return 过滤后的歌曲
     */
    public List<Music163Song> getRecommendSongs(String uid, int n, int k) {

        List<Music163User> userList = getRandomUsers(n);
        List<String> uidList =
                userList.stream().filter(ob -> ob.getSongScore().size() > 0).map(BaseModel::getId).collect(Collectors.toList());
        String randomUserIds = String.join(",", uidList);


        String filePath = pythonFile;
        String[] argvs = new String[]{"python", filePath, "model_recommend", uid, randomUserIds, String.valueOf(k)};

        String s = null;

        List<String> recommendSongIds;
        J2PythonUtil.PythonRes pythonRes = J2PythonUtil.callPythonProcess(argvs);
        if (pythonRes.getCode() == 0) {
            recommendSongIds = filterSongs(pythonRes.getScoreMap().keySet(), uid);
            return Lists.newArrayList(songRepository.findAll(recommendSongIds));
        } else {
            throw new RuntimeException("call python exception");
        }

    }

    /**
     * 过滤掉已经喜欢的歌曲
     *
     * @param uid
     * @return
     */
    private List<String> filterSongs(Set<String> songIdSet, String uid) {
        Music163User user = userRepository.findOne(uid);

        songIdSet.removeAll(user.getLoveSongId());
        return new ArrayList<>(songIdSet);
    }


    /**
     * 根据物品相似度推荐
     *
     * @param uid
     * @param n
     * @param k
     * @return
     */
    public List<Music163Song> userBasedRecommend(String uid, int n, int k) {
        return contentBasedRecommend(uid, n, k, "user");
    }

    /**
     * 根据物品相似度推荐歌曲
     *
     * @param uid
     * @param n
     * @param k
     * @return
     */
    public List<Music163Song> itemBasedRecommend(String uid, int n, int k) {
        return contentBasedRecommend(uid, n, k, "item");
    }


    private List<Music163Song> contentBasedRecommend(String uid, int n, int k, String type) {
        List<Music163User> userList = getRandomUsers(n);
        Set<String> idSet = userList.stream().filter(ob -> ob.getSongScore().size() > 0)
                .map(BaseModel::getId).collect(Collectors.toSet());
        String candidate_id_str = String.join(",", idSet);
        String argv2 = uid;
        String argv3 = candidate_id_str;
        String filePath = pythonFile;

        String argv1 = "";
        if (type.equals("user")) {
            argv1 = "user_recommend";
        } else if (type.equals("item")) {
            argv1 = "item_recommend";
        }
        String[] argvs = new String[]{"python", filePath, argv1, argv2, argv3, String.valueOf(k)};
        J2PythonUtil.PythonRes pythonRes = J2PythonUtil.callPythonProcess(argvs);
        if (pythonRes.getCode() == 0) {
            Map<String, Object> songScore = pythonRes.getScoreMap();
            Set<String> topSongIds =
                    songScore.entrySet().stream().filter(ob -> !String.valueOf(ob.getValue()).equals("NaN")).sorted(Collections.reverseOrder(Comparator.comparingDouble(x -> (double) (x.getValue()))))
                            .limit(k).map(ob -> ob.getKey()).collect(Collectors.toSet());
            List<String> songIds = filterSongs(topSongIds, uid);
            return Lists.newArrayList(songRepository.findAll(songIds));
        }
        return null;
    }


    /**
     * 随机获取用户记录
     * mongo 如何随机获取文档
     *
     * @param n
     * @return
     */
    private List<Music163User> getRandomUsers(int n) {
        List<Music163User> res = new ArrayList<>();
        long count = userRepository.count();
        int pageCount = (int) count / 50;
        Set<Integer> pageIdSet = new HashSet<>();
        int times = n / 50;
        for (int i = 1; i <= times; i++) {
            Random random = new Random();
            int pageId = random.nextInt(pageCount);
            while (!pageIdSet.add(pageId)) {
                pageId = random.nextInt(pageCount);
            }
            res.addAll(userRepository.findAll(new PageRequest(pageId, 50)).getContent());
        }
        int remainder = n % 50;
        Random random = new Random();
        int pageId = random.nextInt(pageCount);
        while (!pageIdSet.add(pageId)) {
            pageId = random.nextInt(pageCount);
        }
        res.addAll(userRepository.findAll(new PageRequest(pageId, remainder)).getContent());
        return res;
    }


    /**
     * 根据余弦相似度 获取最相似的100首歌，再按歌曲的平均分推荐前n首
     *
     * @param songId
     * @param k
     * @return
     */
    public List<Music163Song> getSimilarSongs(String songId, int k, boolean useAverageScore) throws JsonProcessingException {
        Music163SongRecord songRecord = songRecordRepository.findOne(songId);
        if (songRecord == null) {
            LOGGER.warn("have no songRecord of " + songId);
            throw new NullPointerException("have no songRecord of" + songId);
        }
        List<String> userIds = songRecord.getLoverIds();
        if (userIds.size() > 200) {
            Random r = new Random();
            Set<String> chooseUid = new HashSet<>();
            Set<Integer> indexSet = new HashSet<>();
            for (int i = 0; i < 200; i++) {
                int index = r.nextInt(userIds.size());
                if (indexSet.add(index)) {
                    chooseUid.add(userIds.get(index));
                } else {
                    i--;
                }
            }
            userIds = new ArrayList<>(chooseUid);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        String argv1 = "song_similar";
        String argv2 = songId;
        String argv3 = objectMapper.writeValueAsString(userIds);

        String argv4 = "100";
        if (!useAverageScore) {
            argv4 = String.valueOf(k);
        }
        String filePath = pythonFile;
        String[] argvs = new String[]{"python", filePath, argv1, argv2, argv3, argv4};
        J2PythonUtil.PythonRes pythonRes = J2PythonUtil.callPythonProcess(argvs);
        if (pythonRes.getCode() == 0) {
            List<Music163Song> songList = Lists.newArrayList(songRepository.findAll(pythonRes.getScoreMap().keySet()));
            if (!useAverageScore) {
                return songList;
            }
            Map<String, Double> songScoreMap = songRecordAnalyzer.getSongAverageScore(pythonRes.getScoreMap().keySet());
            Comparator<Music163Song> comparator = (s1, s2) -> ComparisonChain.start().compare(songScoreMap.get(s1.getId()), songScoreMap.get(s2.getId())).result();

            return songList.stream().sorted(comparator.reversed()).limit(k).collect(Collectors.toList());

        }
        return null;
    }


}
