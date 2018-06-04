package org.chengy.configuration;

import com.google.common.collect.Lists;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.User;
import org.chengy.repository.remote.Music163UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by nali on 2017/11/4.
 */
@Component
public class CrawlerBizConfig {

    private static String crawlerUserSeed;

    private static List<String> m163userSeeds;
    private static String crawlerUserThreadNum;
    @Autowired
    Music163UserRepository userRepository;

    @PostConstruct
    public void init() {

        User user = new User();
        user.setCommunity(Music163ApiCons.communityName);

        long userCount = userRepository.count();
        if (userCount == 0) {
            m163userSeeds = Lists.newArrayList(crawlerUserSeed.split(","));
        } else {
            if (userCount < 100) {
                m163userSeeds = userRepository.findAll(new PageRequest(0, 100)).getContent()
                        .stream().map(ob -> (ob.getId())).limit(20).collect(Collectors.toList());
            } else {
                int page = (int) (userCount / 100);
                Random r = new Random();
                int pageId = r.nextInt(page);
                int pageSize = 100;
                m163userSeeds = userRepository.findAll(new PageRequest(pageId, pageSize)).getContent()
                        .stream().map(ob -> (ob.getId())).limit(20).collect(Collectors.toList());
            }
        }
        System.out.println("init CrawlerBizConfig success");
    }


    public static List<String> getCrawlerUserSeeds() {
        return m163userSeeds;
    }

    @Value("${crawler.user.seed}")
    public void setCrawlerUserSeed(String crawlerUserSeed) {
        CrawlerBizConfig.crawlerUserSeed = crawlerUserSeed;
    }

    public static String getCrawlerUserThreadNum() {
        return crawlerUserThreadNum;
    }

    @Value("${crawler.user.threadnum}")
    public void setCrawlerUserThreadNum(String crawlerUserThreadNum) {
        CrawlerBizConfig.crawlerUserThreadNum = crawlerUserThreadNum;
    }
}
