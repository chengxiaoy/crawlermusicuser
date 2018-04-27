package org.chengy.infrastructure.music163secret;


import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import io.vertx.core.impl.ConcurrentHashSet;
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
import java.util.stream.Collectors;

@Component
public class Music163BloomFilter {

    private BloomFilter<Integer> userBloomFilter;

    private ConcurrentHashSet<Integer> hashSet = new ConcurrentHashSet<>();

    @Autowired
    UserRepository userRepository;

    @Value("${bloomfilter.nums}")
    private Integer expectInsertions;

    @PostConstruct
    public void init() {
        userBloomFilter = BloomFilter.create(Funnels.integerFunnel(), expectInsertions);
        loadDbData();
        System.out.println("init filter success");
    }


    public void loadDbData() {

        int pageId = 0;
        int pageSize = 1000;
        User user = new User();
        user.setCommunity(Music163ApiCons.communityName);
        Example<User> userExample = Example.of(user, ExampleMatcher.matching().withMatcher("community",
                match -> match.caseSensitive().exact()).withIgnorePaths("id", "gender").withIgnoreNullValues());
        List<Integer> uids = userRepository.findAll(userExample, new PageRequest(pageId++, pageSize)).getContent()
                .stream().map(ob -> Integer.valueOf(ob.getCommunityId())).collect(Collectors.toList());
        while (uids.size() > 0) {
            //userBloomFilter.put(uid);
            hashSet.addAll(uids);
            uids = userRepository.findAll(userExample, new PageRequest(pageId++, pageSize)).getContent()
                    .stream().map(ob -> Integer.valueOf(ob.getCommunityId())).collect(Collectors.toList());
        }
        System.out.println("load user data to BloomFilter finished");
    }


    public boolean containsUid(Integer uid) {
        return hashSet.contains(uid);
    }

    public boolean putUid(Integer uid) {
        return hashSet.add(uid);
    }
}
