package org.chengy.service.crawler.music163;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.MultiMap;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.chengy.infrastructure.music163.*;
import org.chengy.net.vertx.VertxClientFactory;
import org.chengy.model.Music163Song;
import org.chengy.model.Music163User;
import org.chengy.repository.remote.Music163SongRepository;
import org.chengy.repository.remote.Music163UserRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.chengy.infrastructure.music163.Music163ApiCons.Music163UserHost;

@SuppressWarnings("Duplicates")
@Component
public class Vertx163Muisc implements M163CrawlerAsync {


    public static Logger LOGGER = LoggerFactory.getLogger(Vertx163Muisc.class);

    @Autowired
    private Music163UserRepository userRepository;

    @Autowired
    private Music163SongRepository songRepository;

    @Autowired
    M163Parser m163Parser;
    @Autowired
    VertxClientFactory vertxClientFactory;

    private ThreadLocal<WebClient> clientThreadLocal = ThreadLocal.withInitial(() -> {
        return vertxClientFactory.newWebClient();
    });


    ExecutorService parseExecutorService = Executors.newFixedThreadPool(5);


    @Override
    public CompletableFuture<CrawlerUserInfo> getUserInfoAsync(String uid, boolean relativeUser, boolean userExit) {
        CompletableFuture<List<String>> relativeUsers = new CompletableFuture<>();
        relativeUsers.complete(new ArrayList<>());
        if (relativeUser) {
            relativeUsers = getRelativeUserIds(uid);
        }
        if (userExit) {
            CrawlerUserInfo res = new CrawlerUserInfo(null, null);
            return relativeUsers.thenApplyAsync((relatives) -> {
                res.setRelativeIds(relatives);
                return res;
            });
        }
        CompletableFuture<String> htmlFuture = getHtml(Music163UserHost + uid);
        CompletableFuture<Music163User> m163UserFuture = htmlFuture.thenApplyAsync(html -> m163Parser.parseUser(html, uid), parseExecutorService);
        CompletableFuture<List<Pair<String, Integer>>> loveSongsFuture = getLoveSongs(uid);
        CompletableFuture<CrawlerUserInfo> crawlerUserInfoCompletableFuture = m163UserFuture.thenCombineAsync(relativeUsers, CrawlerUserInfo::new);
        return crawlerUserInfoCompletableFuture.thenCombine(loveSongsFuture, (userInfo, songScores) -> {
            userInfo.setLoveSongs(songScores);
            return userInfo;
        });

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
            songJsonStr.whenCompleteAsync((jsonStr, t) -> {
                if (t != null) {
                    res.completeExceptionally(t.getCause());
                } else {
                    try {
                        List<Pair<String, Integer>> pairList = m163Parser.userLoveSongScores(jsonStr);
                        res.complete(pairList);
                    } catch (Exception e) {
                        LOGGER.error("get user love song failed" + jsonStr, e);
                        res.completeExceptionally(e);
                    }
                }
            });
        } catch (Exception e) {
            LOGGER.info("get like song failed:" + uid, e);
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

                    futureHtml.complete(html);
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
        Music163Song song = m163Parser.parseSong(html, songId, lyric);

        songRepository.save(song);
    }


}
