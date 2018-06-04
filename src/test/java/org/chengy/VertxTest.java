package org.chengy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.vertx.core.MultiMap;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.chengy.infrastructure.music163secret.EncryptTools;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.infrastructure.music163secret.UserFactory;
import org.chengy.model.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.chengy.infrastructure.music163secret.Music163ApiCons.Music163UserHost;

@SuppressWarnings("Duplicates")
public class VertxTest {

    private static WebClient webClient;

    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    static {
        Vertx vertx = Vertx.vertx();
        WebClientOptions webClientOptions = new WebClientOptions();
        webClientOptions.setMaxPoolSize(50).setConnectTimeout(1000).setKeepAlive(true)
                .setDefaultHost("music.163.com");
        webClient = WebClient.create(vertx, webClientOptions);
    }


    public static void main(String[] args) {
        VertxTest vertxTest = new VertxTest();
        CrawlerInfo crawlerInfo = vertxTest.getCrawlerInfo("330313");
        System.out.println(crawlerInfo);

//        vertxTest.getLoveSongs("330313");
    }

    public void crawlUser() {
        String seed = "330313";
        LinkedList<String> uidQueue = new LinkedList<>();
        while (uidQueue.size() > 0) {
            String uid = uidQueue.pop();
            CrawlerInfo crawlerInfo = getCrawlerInfo(uid);
            if (CollectionUtils.isEmpty(crawlerInfo.getRelativeIds())) {
                uidQueue.addAll(crawlerInfo.getRelativeIds());
            }
            if (crawlerInfo.getUser() != null) {

                // todo save user
            }
        }
    }

    public CrawlerInfo getCrawlerInfo(String uid) {

        CompletableFuture<String> futureHomeHtml = getFutureHomeHtml(uid);
        CompletableFuture<List<String>> relativeUserIds = getRelativeUserIds(uid);

        CompletableFuture<List<Pair<String, Integer>>> songInfoFuture = getLoveSongs(uid);

        CompletableFuture<User> userInfoCompletableFuture = new CompletableFuture<>();
        futureHomeHtml.whenCompleteAsync((html, t) -> extractUserInfo(uid, html, t, userInfoCompletableFuture), executorService);

        List<String> relaUserIds;
        User user = null;
        List<Pair<String, Integer>> songInfos = new ArrayList<>();
        try {
            relaUserIds = relativeUserIds.get(1000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            relaUserIds = new ArrayList<>(0);
        }
        try {
            user = userInfoCompletableFuture.get(1000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            songInfos = songInfoFuture.get(1000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            e.printStackTrace();
        }

        CrawlerInfo crawlerInfo = new CrawlerInfo(user, relaUserIds);
        crawlerInfo.setLoveSongs(songInfos);
        return crawlerInfo;
    }

    public CompletableFuture<String> getFutureHomeHtml(String uid) {
        HttpRequest<Buffer> request = webClient.requestAbs(HttpMethod.GET, Music163UserHost + uid);
        CompletableFuture<String> futureHtml = new CompletableFuture<>();
        request.send(ar -> {
            if (ar.succeeded()) {
                HttpResponse<Buffer> response = ar.result();
                if (response.statusCode() == 200) {
                    String html =
                            response.body().toString(StandardCharsets.UTF_8);
                    futureHtml.complete(html);
                }
                futureHtml.complete(response.statusCode() + "");
            } else if (ar.failed()) {
                futureHtml.completeExceptionally(ar.cause());
            }
        });
        return futureHtml;
    }

    public void extractUserInfo(String id, String html, Throwable t, CompletableFuture<User> userCompletableFuture) {
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
                    e.printStackTrace();
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
                User user = UserFactory.buildUser(age, area, name, avatar, id, signature, gender);
                System.out.println(user);
                userCompletableFuture.complete(user);
            } catch (Exception e) {
                userCompletableFuture.completeExceptionally(e);
            }
        }
    }

    public CompletableFuture<List<String>> getRelativeUserIds(String uid) {
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

    public CompletableFuture<List<Pair<String, Integer>>> getLoveSongs(String uid) {
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


    public CompletableFuture<String> commonWebAPI(String text, String url) {
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
