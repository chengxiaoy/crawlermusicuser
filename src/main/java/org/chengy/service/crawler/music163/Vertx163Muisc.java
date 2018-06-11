package org.chengy.service.crawler.music163;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.chengy.configuration.CrawlerBizConfig;
import org.chengy.infrastructure.music163secret.*;
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
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.chengy.infrastructure.music163secret.Music163ApiCons.Music163UserHost;

@SuppressWarnings("Duplicates")
@Component
public class Vertx163Muisc implements M163Crawler{


    public static Logger LOGGER = LoggerFactory.getLogger(Vertx163Muisc.class);

    @Autowired
    private Music163UserRepository userRepository;

    @Autowired
    private Music163SongRepository songRepository;


    @Autowired
    VertxClientFactory vertxClientFactory;

    private ThreadLocal<WebClient> clientThreadLocal = ThreadLocal.withInitial(() -> {
        return vertxClientFactory.newWebClient();
    });


    ExecutorService executorService = Executors.newFixedThreadPool(5);


    public CrawlerUserInfo getCrawlerInfo(String uid, boolean relativeUser, boolean userExit) {

        CompletableFuture<List<String>> relativeUserIds = CompletableFuture.completedFuture(new ArrayList<>());
        if (relativeUser) {
            relativeUserIds = getRelativeUserIds(uid);
        }
        if (userExit) {
            List<String> uids = null;
            try {
                uids = relativeUserIds.get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("get relative user error" + e.getMessage(), e);
            }
            return new CrawlerUserInfo(null, uids);
        }

        CompletableFuture<String> futureHomeHtml = getFutureHomeHtml(uid);

        CompletableFuture<List<Pair<String, Integer>>> songInfoFuture = getLoveSongs(uid);
        CompletableFuture<Music163User> userInfoCompletableFuture = new CompletableFuture<>();
        futureHomeHtml.whenCompleteAsync((html, t) -> extractUserInfo(uid, html, t, userInfoCompletableFuture), executorService);

        List<String> relaUserIds;
        Music163User user = null;
        List<Pair<String, Integer>> songInfos = new ArrayList<>();
        try {
            relaUserIds = relativeUserIds.get(10*1000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            relaUserIds = new ArrayList<>(0);
        }
        try {
            user = userInfoCompletableFuture.get(10*1000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.info("crawler user " + uid + "failed ", e);
            if (e.getCause() instanceof IllegalStateException) {
                clientThreadLocal.set(vertxClientFactory.newWebClientWithProxy());
                return getCrawlerInfo(uid, relativeUser, userExit);
            }
        }
        try {
            songInfos = songInfoFuture.get(3000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            LOGGER.info("get user " + uid + "love songs failed ", e);
            if (e.getCause() instanceof IllegalStateException) {
                clientThreadLocal.set(vertxClientFactory.newWebClientWithProxy());
                return getCrawlerInfo(uid, relativeUser, userExit);
            }
        }

        CrawlerUserInfo crawlerInfo = new CrawlerUserInfo(user, relaUserIds);
        crawlerInfo.setLoveSongs(songInfos);
        return crawlerInfo;
    }

    /**
     * 获取个人主页
     *
     * @param uid
     * @return
     */
    private CompletableFuture<String> getFutureHomeHtml(String uid) {
        return getHtml(Music163UserHost + uid);
    }

    /**
     * 解析个人主页
     *
     * @param id
     * @param html
     * @param t
     * @param userCompletableFuture
     */
    private void extractUserInfo(String id, String html, Throwable t, CompletableFuture<Music163User> userCompletableFuture) {
        if (t != null) {
            userCompletableFuture.completeExceptionally(t);

        } else {
            try {
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
                System.out.println(user);
                userCompletableFuture.complete(user);
            } catch (Exception e) {
                userCompletableFuture.completeExceptionally(e);
            }
        }
    }

    /**
     * 获取用户相关的用户
     *
     * @param uid
     * @return
     */
    private CompletableFuture<List<String>> getRelativeUserIds(String uid) {
        // todo
        CompletableFuture<List<String>> completableFuture = new CompletableFuture<>();
        try {
            String fansParam = Music163ApiCons.getFansParams(uid, 1, 100);
            CompletableFuture<String> fansFutureJsonStr = commonWebAPI(fansParam, Music163ApiCons.fansUrl);
            String followedParam = Music163ApiCons.getFollowedParams(uid, 1, 100);
            CompletableFuture<String> followedFutureJsonStr = commonWebAPI(followedParam, Music163ApiCons.getFollowedUrl(uid));
            ObjectMapper objectMapper = new ObjectMapper();

            AtomicInteger steps = new AtomicInteger(0);
            List<String> relativeIds = new ArrayList<>();

            fansFutureJsonStr.whenCompleteAsync((jsonStr, t) -> {
                        if (t != null) {
                            completableFuture.completeExceptionally(t.getCause());
                        } else {
                            try {
                                JsonNode root = objectMapper.readTree(jsonStr);
                                List<JsonNode> jsonNodeList =
                                        root.findValue("followeds").findValues("userId");
                                List<String> ids =
                                        jsonNodeList.stream().map(JsonNode::asText).collect(Collectors.toList());
                                relativeIds.addAll(ids);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (steps.getAndIncrement() == 1) {
                                    completableFuture.complete(relativeIds);
                                }
                            }
                        }

                    }
            );

            followedFutureJsonStr.whenCompleteAsync((jsonStr, t) -> {
                        if (t != null) {
                            completableFuture.completeExceptionally(t.getCause());
                        } else {
                            try {
                                JsonNode root = objectMapper.readTree(jsonStr);
                                List<JsonNode> jsonNodeList =
                                        root.findValue("follow").findValues("userId");
                                List<String> ids =
                                        jsonNodeList.stream().map(JsonNode::asText).collect(Collectors.toList());
                                relativeIds.addAll(ids);
                            } catch (IOException e) {
                                e.printStackTrace();
                            } finally {
                                if (steps.getAndIncrement() == 1) {
                                    completableFuture.complete(relativeIds);
                                }
                            }
                        }
                    }
            );
            return completableFuture;
        } catch (Exception e) {
            e.printStackTrace();
            completableFuture.completeExceptionally(e);
        }
        return completableFuture;
    }

    /**
     * 获取用户喜爱的歌曲
     *
     * @param uid
     * @return
     */
    public CompletableFuture<List<Pair<String, Integer>>> getLoveSongs(String uid) {
        CompletableFuture<List<Pair<String, Integer>>> res = new CompletableFuture<>();
        try {
            String songRecordParam = Music163ApiCons.getSongRecordALLParams(uid, 1, 100);
            CompletableFuture<String> songJsonStr = commonWebAPI(songRecordParam, Music163ApiCons.songRecordUrl);
            ObjectMapper objectMapper = new ObjectMapper();
            songJsonStr.whenCompleteAsync((jsonStr, t) -> {
                if (t != null) {
                    res.completeExceptionally(t.getCause());
                } else {
                    try {
                        List<Pair<String, Integer>> pairList = new ArrayList<>();
                        JsonNode root = objectMapper.readTree(jsonStr);
                        root.findValue("allData").iterator().forEachRemaining(ob -> {
                            String songId = ob.get("song").get("id").asText();
                            int score = ob.get("score").asInt();
                            pairList.add(new ImmutablePair<>(songId, score));
                        });
                        res.complete(pairList);
                    } catch (Exception e) {
                        LOGGER.error("get user love song failed" + jsonStr, e);
                        res.completeExceptionally(e);
                    }
                }
            });


        } catch (Exception e) {
            System.out.println("get like song failed:" + uid);
        }
        return res;

    }

    /**
     * 获取ajax api的数据
     *
     * @param text
     * @param url
     * @return
     */
    private CompletableFuture<String> commonWebAPI(String text, String url) {
        WebClient webClient = clientThreadLocal.get();

        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        try {
            Pair<String, String> pair = EncryptTools.encryptCommonAPI(text);
            String params = pair.getLeft();
            String encSecKey = pair.getRight();
            HttpRequest<Buffer> httpRequest = webClient.postAbs(url);
            MultiMap form = MultiMap.caseInsensitiveMultiMap();
            form.add("params", params);
            form.add("encSecKey", encSecKey);
            httpRequest.putHeader("Referer", "http://music.163.com/").sendForm(form, ar -> {
                if (ar.succeeded()) {
                    HttpResponse<Buffer> response = ar.result();
                    if (response.statusCode() == 503) {
                        completableFuture.completeExceptionally(new IllegalStateException("ip has been temporarily bloked"));
                    }
                    if (response.statusCode() == 200) {
                        String html = response.body().toString(StandardCharsets.UTF_8);
                        if (html.contains("Cheating")) {
                            completableFuture.completeExceptionally(new IllegalStateException("ip has been temporarily bloked"));
                        } else {
                            completableFuture.complete(html);
                        }
                    } else {
                        completableFuture.completeExceptionally(new IllegalStateException("http response is " + response.statusCode()));
                    }
                }
            });
        } catch (Exception e) {
            completableFuture.completeExceptionally(e);
        }
        return completableFuture;
    }


    /**
     * 获取 absurl的静态网页
     *
     * @param absUrl
     * @return
     */
    private CompletableFuture<String> getHtml(String absUrl) {
        WebClient webClient = clientThreadLocal.get();
        HttpRequest<Buffer> request = webClient.requestAbs(HttpMethod.GET, absUrl);
        CompletableFuture<String> futureHtml = new CompletableFuture<>();
        request.send(ar -> {
            if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                if (response.statusCode() == 503) {
                    futureHtml.completeExceptionally(new IllegalStateException("ip has been temporarily bloked"));
                }
                if (response.statusCode() == 200) {
                    String html = response.body().toString(StandardCharsets.UTF_8);
//                    if (html.contains("你要查找的网页找不到")) {
//                        futureHtml.completeExceptionally(new IllegalStateException("ip has been temporarily bloked"));
//                    } else {
                        futureHtml.complete(html);
//                    }
                } else {
                    futureHtml.completeExceptionally(new IllegalStateException("http response is " + response.statusCode()));
                }
            } else if (ar.failed()) {
                futureHtml.completeExceptionally(ar.cause());
            }
        });


        return futureHtml;


    }




    public void getSongInfo(String songId) throws Exception {

        Music163Song exitSong = songRepository.findById(songId).orElse(null);
        if (exitSong != null) {
            return;
        }
        CompletableFuture<String> html =
                getHtml(Music163ApiCons.songHostUrl + songId);
        String params = Music163ApiCons.getLyricParams(songId);
        String lyricUrl = Music163ApiCons.lyricUrl;

        CompletableFuture<String> lyric = commonWebAPI(params, lyricUrl);


        html.thenCombine(lyric, (h, l) -> {
            try {
                saveSongInfo(h, songId, l);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        });

    }

    public void saveSongInfo(String html, String songId, String lyric) throws IOException {
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


}
