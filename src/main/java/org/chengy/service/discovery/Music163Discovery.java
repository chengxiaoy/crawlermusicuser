package org.chengy.service.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import jdk.nashorn.internal.ir.WithNode;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.assertj.core.util.FloatComparator;
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
import org.chengy.service.statistics.J2PythonUtil;
import org.chengy.service.statistics.Music163Statistics;
import org.omg.CosNaming.NamingContextExtPackage.StringNameHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger LOGGER = LoggerFactory.getLogger(Music163Discovery.class);

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

    private static final String pythonFile = "/Users/chengxiaoy/PycharmProjects/abracadabra/music163/user/facade.py";


    /**
     * 使用基于模型的协同过滤 funk-SVD 获得推荐歌曲
     *
     * @param uid 推荐人uid
     * @param n
     * @param k   推荐top k首歌曲
     * @return 过滤后的歌曲
     */
    public List<Song> getRecommendSongs(String uid, int n, int k) {

        List<User> userList = getRandomUsers(n);
        List<String> uidList =
                userList.stream().filter(ob -> ob.getSongScore().size() > 0).map(BaseEntity::getCommunityId).collect(Collectors.toList());
        String randomUserIds = String.join(",", uidList);


        String filePath = pythonFile;
        String[] argvs = new String[]{"python", filePath, "model_recommend", uid, randomUserIds, String.valueOf(k)};

        String s = null;

        List<String> recommendSongIds;
        J2PythonUtil.PythonRes pythonRes = J2PythonUtil.callPythonProcess(argvs);
        if (pythonRes.getCode() == 0) {
            recommendSongIds = filterSongs(pythonRes.getScoreMap().keySet(), uid);
            return songRepository.findSongsByCommunityIdInAndCommunity(recommendSongIds, Music163ApiCons.communityName);
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
        User user = userRepository.findByCommunityIdAndCommunity(uid, Music163ApiCons.communityName);

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
    public List<Song> userBasedRecommend(String uid, int n, int k) {
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
    public List<Song> itemBasedRecommend(String uid, int n, int k) {
        return contentBasedRecommend(uid, n, k, "item");
    }


    private List<Song> contentBasedRecommend(String uid, int n, int k, String type) {
        List<User> userList = getRandomUsers(n);
        Set<String> idSet = userList.stream().filter(ob -> ob.getSongScore().size() > 0)
                .map(BaseEntity::getCommunityId).collect(Collectors.toSet());
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
            return songRepository.findSongsByCommunityIdInAndCommunity(songIds, Music163ApiCons.communityName);
        }
        return null;
    }


    private List<User> getRandomUsers(int n) {
        int pageSize = n;
        long count = userRepository.count(UserMatcherFactory.music163BasicUserMatcher());
        int pageCount = (int) count / pageSize;

        Random r = new Random();
        int pageId = r.nextInt(pageCount);
        return userRepository.findAll(UserMatcherFactory.music163BasicUserMatcher(), new PageRequest(pageId, pageSize)).getContent();

    }


    /**
     * @param songId
     * @param k
     * @return
     */
    public List<Song> getSimilarSongs(String songId, int k) throws JsonProcessingException {
        SongRecord songRecord = songRecordRepository.findSongRecordByCommunityIdAndCommunity(songId, Music163ApiCons.communityName);
        if (songRecord == null) {
            LOGGER.warn("have no songRecord of " + songId);
            throw new NullPointerException("have no songRecord of" + songId);
        }
        List<String> userIds = songRecord.getLoverIds();

        ObjectMapper objectMapper = new ObjectMapper();
        String argv1 = "song_similar";
        String argv2 = songId;
        String argv3 = objectMapper.writeValueAsString(userIds);
        String argv4 = String.valueOf(k);
        String filePath = pythonFile;
        String[] argvs = new String[]{"python", filePath, argv1, argv2, argv3, argv4};
        J2PythonUtil.PythonRes pythonRes = J2PythonUtil.callPythonProcess(argvs);
        if (pythonRes.getCode() == 0) {
            return songRepository.findSongsByCommunityIdInAndCommunity(pythonRes.getScoreMap().keySet(), Music163ApiCons.communityName);
        }
        return null;
    }






}
