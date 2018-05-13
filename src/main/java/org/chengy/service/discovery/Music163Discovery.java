package org.chengy.service.discovery;

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


        String argv1 = uid;
        String argv2 = randomUserIds;
        String argv3 = String.valueOf(k);
        String filePath = "/Users/chengxiaoy/PycharmProjects/abracadabra/music163/user/model_recommend.py";
        String[] argvs = new String[]{"python", filePath, argv1, argv2, argv3};

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
    public List<String> filterSongs(Set<String> songIdSet, String uid) {
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
        String argv1 = uid;
        String argv2 = candidate_id_str;
        String filePath = "/Users/chengxiaoy/PycharmProjects/abracadabra/music163/user/user_recommend.py";
        String[] argvs = new String[]{"python", filePath, argv1, argv2, String.valueOf(k), type};
        J2PythonUtil.PythonRes pythonRes = J2PythonUtil.callPythonProcess(argvs);
        if (pythonRes.getCode() == 0) {

        Map<String, Object> songScore = pythonRes.getScoreMap();
        Set<String> topSongIds =
                songScore.entrySet().stream().sorted(Collections.reverseOrder(Comparator.comparingDouble(x -> (double) (x.getValue()))))
                        .limit(k).map(ob -> ob.getKey()).collect(Collectors.toSet());


            List<String> songIds = filterSongs(topSongIds, uid);
            return songRepository.findSongsByCommunityIdInAndCommunity(songIds, Music163ApiCons.communityName);
        }
        return null;
    }


    public List<User> getRandomUsers(int n) {
        int pageSize = n;
        long count = userRepository.count(UserMatcherFactory.music163BasicUserMatcher());
        int pageCount = (int) count / pageSize;

        Random r = new Random();
        int pageId = r.nextInt(pageCount);

        return userRepository.findAll(UserMatcherFactory.music163BasicUserMatcher(), new PageRequest(pageId, pageSize)).getContent();

    }


}
