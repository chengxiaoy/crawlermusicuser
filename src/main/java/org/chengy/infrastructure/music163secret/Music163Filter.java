package org.chengy.infrastructure.music163secret;


import org.chengy.repository.remote.Music163UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;


/**
 * 该类 提供爬虫系统中的判重机制
 * 爬虫数据太小  用bloomfilter有误判率 直接使用bitmap
 */
@Component
public class Music163Filter {


    private BitSet bitSet = new BitSet();


    private ConcurrentHashMap<Integer,Object> concurrentHashMap=new ConcurrentHashMap<>();

    @Autowired
    Music163UserRepository userRepository;

    @Value("${profile}")
    String env;

    @PostConstruct
    public void init() {
        //做一个开关
        if (!env.equals("test")) {
            loadDbData();
            System.out.println("init filter success");
        }
    }


    public void loadDbData() {

        int pageId = 0;
        int pageSize = 10000;


        List<Integer> uids = userRepository.findAll(new PageRequest(pageId, pageSize)).getContent().stream()
                .map(ob -> Integer.valueOf(ob.getId())).collect(Collectors.toList());

        while (uids.size() > 0) {
            for (Integer uid : uids) {
                putUid(uid);
            }
            uids = userRepository.findAll(new PageRequest(pageId++, pageSize)).getContent()
                    .stream().map(ob -> Integer.valueOf(ob.getId())).collect(Collectors.toList());
        }
        System.out.println("load user data to filter finished");
    }


    public boolean containsUid(Integer uid) {
        synchronized (this) {
            return bitSet.get(uid);
        }
    }

    public boolean putUid(Integer uid) {
        synchronized (this) {
            bitSet.set(uid);
            return true;
        }
    }
}
