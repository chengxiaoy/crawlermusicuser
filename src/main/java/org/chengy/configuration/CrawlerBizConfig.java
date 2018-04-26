package org.chengy.configuration;

import com.google.common.collect.Lists;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.User;
import org.chengy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
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

    private static List<String> seeds;
    private static String crawlerUserThreadNum;
    @Autowired
    UserRepository userRepository;

    @PostConstruct
    public void init() {

        User user = new User();
        user.setCommunity(Music163ApiCons.communityName);
        Example<User> userExample = Example.of(user, ExampleMatcher.matching()
                .withMatcher("community", match -> match.caseSensitive().exact())
                .withIgnorePaths("id","gender").withIgnoreNullValues());
        long userCount = userRepository.count(userExample);
        if (userCount == 0) {
            seeds = Lists.newArrayList(crawlerUserSeed.split(","));
        } else {
            if (userCount < 100) {
                seeds = userRepository.findAll(userExample, new PageRequest(0, 100)).getContent()
                        .stream().map(ob -> (ob.getCommunityId())).limit(20).collect(Collectors.toList());
            } else {
                int page = (int) (userCount / 100);
                Random r = new Random();
                int pageId = r.nextInt(page);
                int pageSize = 100;
                seeds = userRepository.findAll(userExample, new PageRequest(pageId, pageSize)).getContent()
                        .stream().map(ob -> (ob.getCommunityId())).limit(20).collect(Collectors.toList());
            }
        }
        System.out.println("init CrawlerBizConfig success");
    }


    public static List<String> getCrawlerUserSeeds() {
        return seeds;
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
