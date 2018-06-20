package org.chengy.service.crawler;

import org.apache.commons.lang3.tuple.Pair;
import org.chengy.configuration.CrawlerBizConfig;
import org.chengy.infrastructure.music163.Music163Filter;
import org.chengy.model.Music163User;
import org.chengy.repository.remote.Music163SongRepository;
import org.chengy.repository.remote.Music163UserRepository;
import org.chengy.service.crawler.music163.CrawlerUserInfo;
import org.chengy.service.crawler.music163.M163CrawlerAsync;
import org.chengy.service.statistics.Music163Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * Created by nali on 2017/9/28.
 */
@Component
public class CrawlerLauncher {

    private static final Logger LOGGER = LoggerFactory.getLogger(CrawlerLauncher.class);

    @Autowired
    Music163UserRepository userRepository;
    @Autowired
    Music163SongRepository songRepository;

    @Autowired
    Music163Statistics music163Statistics;

    @Autowired
    @Qualifier("vertx163Muisc")
    M163CrawlerAsync m163CrawlerAsync;

    @Autowired
    @Qualifier("songExecutor")
    ThreadPoolTaskExecutor threadPoolTaskExecutor;


    @Autowired
    @Qualifier("userExecutor")
    ThreadPoolTaskExecutor userExecutor;



    @Autowired
    CrawlerBizConfig crawlerBizConfig;

    @Autowired
    Music163Filter filter;


    public void crawlM163User() throws InterruptedException {

        while (true) {
            String uid = crawlerBizConfig.getCrawlerUid();
            try {
                boolean userExit = filter.containsUid(uid);
                if (userExit && !crawlerBizConfig.needAdd()) {
                    continue;
                }

                Runnable crawlerUserInfoTask = new Runnable() {
                    @Override
                    public void run() {
                        boolean flag = crawlerBizConfig.needAdd();
                        CompletableFuture<CrawlerUserInfo> crawlerInfoFuture = m163CrawlerAsync.getUserInfoAsync(uid, flag, userExit);

                        crawlerInfoFuture.whenComplete((crawlerInfo, throwable) -> {
                            if (throwable != null) {
                                LOGGER.warn("craw user {} failed", uid, throwable);
                            }
                            List<String> relativeIds = crawlerInfo.getRelativeIds();
                            if (!org.apache.commons.collections.CollectionUtils.isEmpty(relativeIds)) {
                                crawlerBizConfig.setCrawlUids(relativeIds);
                            }
                            if (crawlerInfo.getUser() != null && !userExit) {
                                Music163User user = crawlerInfo.getUser();
                                List<Pair<String, Integer>> songInfo = crawlerInfo.getLoveSongs();
                                List<String> songIds = songInfo.stream()
                                        .map(Pair::getLeft).collect(Collectors.toList());
                                user.setLoveSongId(songIds);
                                user.setSongScore(songInfo);
                                userRepository.save(user);
                                filter.putUid(uid);
                                LOGGER.info("craw " + uid + " succeed!");
                            }
                        });

                    }
                };
                userExecutor.execute(crawlerUserInfoTask);
            } catch (Exception e) {
                System.out.println(uid + " get info failed");
                e.printStackTrace();
            }
        }
    }


}
