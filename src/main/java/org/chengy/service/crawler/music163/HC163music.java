package org.chengy.service.crawler.music163;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.chengy.core.HttpHelper;
import org.chengy.infrastructure.music163secret.EncryptTools;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.infrastructure.music163secret.SongFactory;
import org.chengy.infrastructure.music163secret.UserFactory;
import org.chengy.newmodel.Music163Song;
import org.chengy.newmodel.Music163User;
import org.chengy.repository.remote.Music163SongRepository;
import org.chengy.repository.remote.Music163UserRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    MongoTemplate mongoTemplate;

    public Music163User getUserInfo(String id) throws Exception {


        String html = HttpHelper.get(Music163ApiCons.Music163UserHost + id);
        Document document = Jsoup.parse(html);

        // 听歌总数
        int songNums = 0;
        try {
            String songNumsInfo =
                    document.select("#rHeader > h4").get(0).html();
            songNums = Integer.parseInt(songNumsInfo.substring(4, songNumsInfo.length() - 1));
        } catch (Exception e) {
            LOGGER.warn("获取用户 " + id + " 听歌数量失败");
        }

        //性别
        boolean ismale = document.select("#j-name-wrap > i").hasClass("u-icn-01");
        boolean isfemale = document.select("#j-name-wrap > i").hasClass("u-icn-02");
        int gender = 0;
        if (ismale) {
            gender = 1;
        } else if (isfemale) {
            gender = 2;
        }

        String name = document.select("#j-name-wrap > span.tit.f-ff2.s-fc0.f-thide").get(0).html();
        //个性签名
        Elements signatureinfo = document.select("#head-box > dd > div.inf.s-fc3.f-brk");
        String signature = "";
        if (signatureinfo.size() > 0) {
            signature = signatureinfo.get(0).html().split("：")[1];
        }
        //年龄
        Elements ageinfo = document.select("#age");
        Date age = null;
        if (ageinfo.size() > 0) {
            age = new Date(Long.parseLong(ageinfo.get(0).attr("data-age")));
        }

        //地区的代码逻辑
        Elements elements = document.select("#head-box > dd > div:nth-child(4) > span:nth-child(1)");
        String area = "";
        if (elements.size() > 0) {
            try {
                area = elements.get(0).html().split("：")[1];
            } catch (Exception e) {

                elements = document.select("#head-box > dd > div:nth-child(3) > span:nth-child(1)");
                area = elements.get(0).html().split("：")[1];
            }
        } else {
            elements = document.select("#head-box > dd > div.inf.s-fc3 > span");
            if (elements.size() > 0) {
                area = elements.get(0).html().split("：")[1];
            }
        }

        String avatar = document.select("#ava > img").attr("src");
        Music163User user = UserFactory.buildMusic163User(age, area, name, avatar, id, signature, gender, songNums);


        return user;


    }

    public List<String> getFansId(String uid) throws Exception {

        String fansParam = Music163ApiCons.getFansParams(uid, 1, 100);
        Document document = EncryptTools.commentAPI(fansParam, Music163ApiCons.fansUrl);
        JsonNode root = objectMapper.readTree(document.text());
        List<JsonNode> jsonNodeList =
                root.findValue("followeds").findValues("userId");

        List<String> ids =
                jsonNodeList.stream().map(JsonNode::asText).collect(Collectors.toList());

        return ids;
    }

    public List<String> getFollowedId(String uid) throws Exception {
        String followedParam = Music163ApiCons.getFollowedParams(uid, 1, 30);
        Document document = EncryptTools.commentAPI(followedParam, Music163ApiCons.getFollowedUrl(uid));

        JsonNode root = objectMapper.readTree(document.text());

        List<String> ids =
                root.findValue("follow").findValues("userId").stream().map(ob -> ob.asText()).collect(Collectors.toList());
        return ids;
    }

    public List<Pair<String, Integer>> getUserLikeSong(String uid) throws Exception {
        List<Pair<String, Integer>> pairList = new ArrayList<>();

        try {
            String songRecordParam = Music163ApiCons.getSongRecordALLParams(uid, 1, 100);
            Document document = EncryptTools.commentAPI(songRecordParam, Music163ApiCons.songRecordUrl);
            JsonNode root = objectMapper.readTree(document.text());

            root.findValue("allData").iterator().forEachRemaining(ob -> {
                String songId = ob.get("song").get("id").asText();
                int score = ob.get("score").asInt();
                pairList.add(new ImmutablePair<>(songId, score));
            });

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

    public void getSongInfo(String songId) throws Exception {

        Music163Song exitSong = songRepository.findById(songId).orElse(null);
        if (exitSong != null) {
            return;
        }
        String html =
                HttpHelper.get(Music163ApiCons.songHostUrl + songId);
        Document document = Jsoup.parse(html);

        Elements titleEle = document.select("body > div.g-bd4.f-cb > div.g-mn4 > div > div > div.m-lycifo > div.f-cb > div.cnt > div.hd > div > em");
        String title = titleEle.get(0).html();
        Elements artsELes = document.select("body > div.g-bd4.f-cb > div.g-mn4 > div > div > div.m-lycifo > div.f-cb > div.cnt > p:nth-child(2)");
        String art = artsELes.text().split("：")[1].trim();

        Elements albumEle = document.select("body > div.g-bd4.f-cb > div.g-mn4 > div > div > div.m-lycifo > div.f-cb > div.cnt > p:nth-child(3) > a");
        String albumTitle = albumEle.get(0).html();
        String albumId = albumEle.get(0).attr("href").split("id=")[1];

        List<String> arts = new ArrayList<>();
        Arrays.asList(art.split("/")).forEach(ob -> arts.add(ob.trim()));

        String lyric = getLyric(songId);
        ObjectMapper objectMapper = new ObjectMapper();

        JsonNode root = objectMapper.readTree(lyric);
        try {
            try {
                lyric = root.findValue("lrc").findValue("lyric").asText();
            } catch (Exception e) {
                lyric = root.findValue("tlyric").findValue("lyric").asText();
            }
            String composer = "";
            String pattern = "作曲 : .*?\n";
            Pattern r = Pattern.compile(pattern);
            Matcher matcher = r.matcher(lyric);
            while (matcher.find()) {
                composer = matcher.group().split(":")[1].trim();
            }

            String lyricist = "";
            pattern = "作词 : .*?\n";
            r = Pattern.compile(pattern);
            matcher = r.matcher(lyric);
            while (matcher.find()) {
                lyricist = matcher.group().split(":")[1].trim();
            }

            Music163Song song = SongFactory.buildMusic163Song(songId, lyric, arts, albumTitle, albumId, title, composer, lyricist);
            System.out.println(song);
            songRepository.save(song);
        } catch (Exception e) {
            Music163Song song = SongFactory.buildMusic163Song(songId, "", arts, albumTitle, albumId, title, "", "");
            System.out.println(song);
            songRepository.save(song);
        }

    }

    public String getLyric(String songId) throws Exception {
        String params = Music163ApiCons.getLyricParams(songId);
        System.out.println(params);
        String lyricUrl = Music163ApiCons.lyricUrl;
        Document document = EncryptTools.commentAPI(params, lyricUrl);
        return document.text();
    }

    /**
     * 获取某个用户总共听过多少歌
     *
     * @param uid
     * @return
     * @throws Exception
     */
    public int getRecordSongNum(String uid) throws Exception {

        String html = HttpHelper.get(Music163ApiCons.Music163UserHost + uid);
        Document document = Jsoup.parse(html);
        String songNums =
                document.select("#rHeader > h4").get(0).html();
        return Integer.parseInt(songNums.substring(4, songNums.length() - 1));
    }

    @Override
    public CrawlerUserInfo getCrawlerInfo(String uid, boolean relativeUser, boolean userExit) {
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
