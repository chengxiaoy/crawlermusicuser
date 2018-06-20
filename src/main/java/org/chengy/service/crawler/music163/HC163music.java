package org.chengy.service.crawler.music163;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.Pair;
import org.chengy.net.hc.HttpHelper;
import org.chengy.infrastructure.music163.EncryptTools;
import org.chengy.infrastructure.music163.Music163ApiCons;
import org.chengy.model.Music163Song;
import org.chengy.model.Music163User;
import org.chengy.repository.remote.Music163SongRepository;
import org.chengy.repository.remote.Music163UserRepository;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by nali on 2017/9/12.
 */
@Service
public class HC163music implements M163Crawler {

    private static final Logger LOGGER = LoggerFactory.getLogger(HC163music.class);

    @Autowired
    private Music163UserRepository userRepository;
    @Autowired
    private Music163SongRepository songRepository;

    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    M163Parser m163Parser;


    public Music163User getUserInfo(String id) throws Exception {

        String html = HttpHelper.get(Music163ApiCons.Music163UserHost + id);
        return m163Parser.parseUser(html, id);

    }

    public List<String> getFansId(String uid) throws Exception {

        String fansParam = Music163ApiCons.getFansParams(uid, 1, 100);
        Document document = EncryptTools.commentAPI(fansParam, Music163ApiCons.fansUrl);
        return m163Parser.fansUsers(document.text());
    }

    public List<String> getFollowedId(String uid) throws Exception {
        String followedParam = Music163ApiCons.getFollowedParams(uid, 1, 30);
        Document document = EncryptTools.commentAPI(followedParam, Music163ApiCons.getFollowedUrl(uid));

        return m163Parser.followedUsers(document.text());
    }

    public List<Pair<String, Integer>> getUserLikeSong(String uid) throws Exception {
        List<Pair<String, Integer>> pairList = new ArrayList<>();

        try {
            String songRecordParam = Music163ApiCons.getSongRecordALLParams(uid, 1, 100);
            Document document = EncryptTools.commentAPI(songRecordParam, Music163ApiCons.songRecordUrl);
            pairList.addAll(m163Parser.userLoveSongScores(document.text()));
        } catch (Exception e) {
            System.out.println("get like song failed:" + uid);
        }
        return pairList;
    }

    /**
     * 获取用户最近在听的歌曲
     *
     * @param uid 用户id
     * @throws Exception
     */
    public List<String> getUserRecentSong(String uid) throws Exception {
        List<String> songIds = new ArrayList<>();

        String songRecordWeek = Music163ApiCons.getSongRecordofWeek(uid, 1, 100);
        Document document = EncryptTools.commentAPI(songRecordWeek, Music163ApiCons.songRecordUrl);
        JsonNode root = objectMapper.readTree(document.text());
        songIds =
                root.findValue("weekData").findValues("song").stream()
                        .map(ob -> ob.get("id").asText()).collect(Collectors.toList());
        return songIds;
    }

    public Music163Song getSongInfo(String songId) throws Exception {

        Music163Song exitSong = songRepository.findById(songId).orElse(null);
        if (exitSong != null) {
            return null;
        }
        String html =
                HttpHelper.get(Music163ApiCons.songHostUrl + songId);
        String lyric = getLyric(songId);
        return m163Parser.parseSong(html, songId, lyric);
    }

    public String getLyric(String songId) throws Exception {
        String params = Music163ApiCons.getLyricParams(songId);
        System.out.println(params);
        String lyricUrl = Music163ApiCons.lyricUrl;
        Document document = EncryptTools.commentAPI(params, lyricUrl);
        return document.text();
    }



    @Override
    public CrawlerUserInfo getUserInfo(String uid, boolean relativeUser, boolean userExit) {
        try {
            Music163User music163User = getUserInfo(uid);
            List<String> relativeUid = new ArrayList<>();
            relativeUid.addAll(getFansId(uid));
            relativeUid.addAll(getFollowedId(uid));
            List<Pair<String, Integer>> songRecord = getUserLikeSong(uid);

            List<String> loveSongids = songRecord.stream().map(ob -> ob.getLeft()).collect(Collectors.toList());
            music163User.setSongScore(songRecord);
            music163User.setLoveSongId(loveSongids);

            CrawlerUserInfo crawlerUserInfo = new CrawlerUserInfo(music163User, relativeUid);

            crawlerUserInfo.setLoveSongs(songRecord);
            return crawlerUserInfo;
        } catch (Exception e) {
            LOGGER.error("get user info error", e);
            return null;
        }


    }
}
