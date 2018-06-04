package org.chengy.service.crawler;

import org.chengy.configuration.CrawlerBizConfig;
import org.chengy.configuration.HttpConfig;
import org.chengy.newmodel.Music163User;
import org.chengy.repository.remote.Music163SongRepository;
import org.chengy.repository.remote.Music163UserRepository;
import org.chengy.service.crawler.music163.Crawler163music;
import org.chengy.service.crawler.music163.Vertx163Muisc;
import org.chengy.service.statistics.Music163Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
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
    Crawler163music crawler163music;
    @Autowired
    Music163Statistics music163Statistics;
    @Autowired
    Vertx163Muisc vertx163Muisc;
    @Autowired
    @Qualifier("songExecutor")
    ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public void saveMusic163SongByUser() {
        int pageIndex = 0;
        while (true) {
            int pageSize = 100;
            Pageable pageable = new PageRequest(pageIndex, pageSize);
            List<Music163User> userList = userRepository.findAll(pageable).getContent();

            for (Music163User user : userList) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        List<String> songIdlist = user.getLoveSongId();
                        if (songIdlist != null && !CollectionUtils.isEmpty(songIdlist)) {
                            for (String songId : songIdlist) {
                                try {
                                    vertx163Muisc.getSongInfo(songId);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                };
                threadPoolTaskExecutor.execute(runnable);
            }
            pageIndex++;
            LOGGER.info("page is at :" + pageIndex);
        }

    }

    public void crawlMusic163User() throws InterruptedException {
        boolean flag = true;
        while (flag) {
            long userCount = userRepository.count();
            HttpConfig.getHttpProxy();
            int threadNums = Integer.valueOf(CrawlerBizConfig.getCrawlerUserThreadNum());

            if (userCount >= threadNums) {
                int pageId = (int) userCount / threadNums;
                Pageable pageable = new PageRequest(pageId - 1, threadNums);
                List<String> listStr = userRepository.findAll(pageable).getContent().stream().map(ob -> ob.getId()).collect(Collectors.toList());
                System.out.println(listStr);
                Iterator strItr = listStr.iterator();
                for (int i = 0; i < threadNums; i++) {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            String communityId = (String) strItr.next();
                            Music163User user = userRepository.findOne(communityId);
                            userRepository.delete(user);
                            crawler163music.getUserInfo(Arrays.asList(communityId));
                        }
                    });
                    thread.start();
                }
                flag = false;
            }

            //第一次启动的时候的时候
            if (userCount < threadNums) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        crawler163music.getUserInfo(CrawlerBizConfig.getCrawlerUserSeeds());

                    }
                }).start();
            }
            Thread.sleep(1000 * 60 * 5);

        }


    }

    /**
     * 取用户十条最爱的歌曲信息不够 扩大为100条
     */
    public void fixMuser163User() {
        while (true) {
            int pageSize = 100;
            Pageable pageable = new PageRequest(0, pageSize);
            List<Music163User> userList = userRepository.findAll(pageable).getContent();

            for (Music163User user : userList) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            String uid = user.getId();
                            List<String> songids = null;
                            songids = crawler163music.getUserLikeSong(uid);
                            user.setLoveSongId(songids);
                            userRepository.save(user);
                        } catch (Exception e) {
                            System.out.println("failed get song for user:" + user);
                        }

                    }
                };
                threadPoolTaskExecutor.execute(runnable);
            }
        }
    }


}
