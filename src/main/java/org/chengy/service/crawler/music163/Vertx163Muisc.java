package org.chengy.service.crawler.music163;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.chengy.configuration.CrawlerBizConfig;
import org.chengy.core.HttpHelper;
import org.chengy.infrastructure.music163secret.*;
import org.chengy.model.Song;
import org.chengy.model.User;
import org.chengy.repository.SongRepository;
import org.chengy.repository.UserRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.chengy.infrastructure.music163secret.Music163ApiCons.Music163UserHost;

@Component
public class Vertx163Muisc {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SongRepository songRepository;

    @Autowired
    Music163BloomFilter bloomFilter;

    @Autowired
    VertxClientFactory vertxClientFactory;

    private ThreadLocal<WebClient> clientThreadLocal = ThreadLocal.withInitial(() -> {
        return vertxClientFactory.newWebClient();
    });


    @Autowired
    @Qualifier("userExecutor")
    ThreadPoolTaskExecutor threadPoolTaskExecutor;

    ExecutorService executorService = Executors.newFixedThreadPool(3);


    public static void main(String[] args) {
        Vertx163Muisc vertxTest = new Vertx163Muisc();
        CrawlerInfo crawlerInfo = vertxTest.getCrawlerInfo("330313", true, false);
        System.out.println(crawlerInfo);

    }

    public void crawlUser() throws InterruptedException {
        List<String> seeds = CrawlerBizConfig.getCrawlerUserSeeds();

        BlockingQueue<String> uidQueue = new ArrayBlockingQueue<>(10000);
        uidQueue.addAll(seeds);
        while (true) {
            String uid = uidQueue.take();
            try {
                boolean userExit = bloomFilter.containsUid(Integer.valueOf(uid));
                if (userExit && uidQueue.size() > 100) {
                    continue;
                }
                Runnable crawlerUserInfoTask = new Runnable() {
                    @Override
                    public void run() {
                        boolean flag = false;
                        if (uidQueue.size() < 1000) {
                            flag = true;
                        }
                        CrawlerInfo crawlerInfo = getCrawlerInfo(uid, flag, userExit);
                        if (!CollectionUtils.isEmpty(crawlerInfo.getRelativeIds())) {
                            uidQueue.addAll(crawlerInfo.getRelativeIds());
                        }
                        if (crawlerInfo.getUser() != null && !userExit) {
                            User user = crawlerInfo.getUser();
                            List<Pair<String, Integer>> songInfo = crawlerInfo.getLoveSongs();
                            List<String> songIds = songInfo.stream()
                                    .map(Pair::getLeft).collect(Collectors.toList());
                            user.setLoveSongId(songIds);
                            user.setSongScore(songInfo);
                            userRepository.save(user);
                            bloomFilter.putUid(Integer.parseInt(uid));
                        }
                    }
                };
                threadPoolTaskExecutor.execute(crawlerUserInfoTask);
            } catch (Exception e) {
                System.out.println(uid + " get info failed");
                e.printStackTrace();
            }
        }
    }

    private CrawlerInfo getCrawlerInfo(String uid, boolean relativeUser, boolean userExit) {

        CompletableFuture<List<String>> relativeUserIds = CompletableFuture.completedFuture(new ArrayList<>());
        if (relativeUser) {
            relativeUserIds = getRelativeUserIds(uid);
        }
        if (userExit) {
            List<String> uids = null;
            try {
                uids = relativeUserIds.get();
            } catch (InterruptedException | ExecutionException e) {
                System.out.println(e.getMessage());
            }
            return new CrawlerInfo(null, uids);
        }

        CompletableFuture<String> futureHomeHtml = getFutureHomeHtml(uid);

        CompletableFuture<List<Pair<String, Integer>>> songInfoFuture = getLoveSongs(uid);
        CompletableFuture<User> userInfoCompletableFuture = new CompletableFuture<>();
        futureHomeHtml.whenCompleteAsync((html, t) -> extractUserInfo(uid, html, t, userInfoCompletableFuture), executorService);

        List<String> relaUserIds;
        User user = null;
        List<Pair<String, Integer>> songInfos = new ArrayList<>();
        try {
            relaUserIds = relativeUserIds.get();
        } catch (Exception e) {
            relaUserIds = new ArrayList<>(0);
        }
        try {
            user = userInfoCompletableFuture.get();
        } catch (Exception e) {
            System.out.println("crawler user " + uid + "failed");
        }
        try {
            songInfos = songInfoFuture.get();
        } catch (Exception e) {
            System.out.println("get user love songs failed" + e.getMessage());
        }

        CrawlerInfo crawlerInfo = new CrawlerInfo(user, relaUserIds);
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
    private void extractUserInfo(String id, String html, Throwable t, CompletableFuture<User> userCompletableFuture) {
        if (t != null) {
            userCompletableFuture.completeExceptionally(t);

        } else {
            try {
                Document document = Jsoup.parse(html);
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
                User user = UserFactory.buildUser(age, area, name, avatar, id, signature, gender);
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
            );

            followedFutureJsonStr.whenCompleteAsync((jsonStr, t) -> {
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
    private CompletableFuture<List<Pair<String, Integer>>> getLoveSongs(String uid) {
        CompletableFuture<List<Pair<String, Integer>>> res = new CompletableFuture<>();
        try {
            String songRecordParam = Music163ApiCons.getSongRecordALLParams(uid, 1, 100);
            CompletableFuture<String> songJsonStr = commonWebAPI(songRecordParam, Music163ApiCons.songRecordUrl);
            ObjectMapper objectMapper = new ObjectMapper();
            songJsonStr.whenCompleteAsync((jsonStr, t) -> {
                if (t != null) {
                    res.completeExceptionally(t);
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
                        clientThreadLocal.set(vertxClientFactory.newWebClient());
                        try {
                            completableFuture.complete(commonWebAPI(text, url).get());
                        } catch (InterruptedException | ExecutionException e) {
                            throw new RuntimeException("future get method interrupted");
                        }
                    }
                    if (response.statusCode() == 200) {
                        String html =
                                response.body().toString(StandardCharsets.UTF_8);
                        completableFuture.complete(html);
                    }
                    completableFuture.complete(response.statusCode() + "");
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
                    clientThreadLocal.set(vertxClientFactory.newWebClient());
                    CompletableFuture<String> completableFuture = getHtml(absUrl);
                    try {
                        futureHtml.complete(completableFuture.get());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException("get method interrupt", e);
                    }
                }
                if (response.statusCode() == 200) {
                    String html = response.body().toString(StandardCharsets.UTF_8);
                    futureHtml.complete(html);
                }
                futureHtml.complete(response.statusCode() + "");
            } else if (ar.failed()) {
                futureHtml.completeExceptionally(ar.cause());
            }
        });
        return futureHtml;


    }

    class CrawlerInfo {
        private User user;
        private List<String> relativeIds;
        private List<Pair<String, Integer>> loveSongs;


        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public List<String> getRelativeIds() {
            return relativeIds;
        }

        public void setRelativeIds(List<String> relativeIds) {
            this.relativeIds = relativeIds;
        }

        public CrawlerInfo(User user, List<String> relativeIds) {
            this.user = user;
            this.relativeIds = relativeIds;
        }


        public List<Pair<String, Integer>> getLoveSongs() {
            return loveSongs;
        }

        public void setLoveSongs(List<Pair<String, Integer>> loveSongs) {
            this.loveSongs = loveSongs;
        }
    }

}
