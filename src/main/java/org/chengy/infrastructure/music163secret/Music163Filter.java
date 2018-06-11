package org.chengy.infrastructure.music163secret;


import io.vertx.core.impl.ConcurrentHashSet;
import org.chengy.model.User;
import org.chengy.repository.remote.Music163UserRepository;
import org.chengy.util.RedisUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import javax.annotation.PostConstruct;
import java.util.BitSet;


/**
 * 该类 提供爬虫系统中的判重机制
 * 爬虫数据太小  用bloomfilter有误判率  可以使用bitmap
 * 每次启动从mongo移到内存太慢
 * 从redis中取
 */
@Component
public class Music163Filter {


    private BitSet bitSet = new BitSet();

    private static final String USER_KEY = "user_id";
    private static final String SONG_KEY = "song_id";


    private ThreadLocal<Jedis> jedisThreadLocal = ThreadLocal.withInitial(RedisUtil::getJedis);


    @Value("${profile}")
    String env;

    @PostConstruct
    public void init() {

    }


    public boolean containsUid(Integer uid) {
        Jedis jedis=jedisThreadLocal.get();
        return jedis.sismember(USER_KEY, String.valueOf(uid));
    }

    public boolean putUid(Integer uid) {
        Jedis jedis=jedisThreadLocal.get();

        jedis.sadd(USER_KEY, String.valueOf(uid));
        return true;
    }

    public boolean containsSongId(String songId) {
        Jedis jedis=jedisThreadLocal.get();

        return jedis.sismember(SONG_KEY, songId);
    }

    public boolean putSongId(String songId) {
        Jedis jedis=jedisThreadLocal.get();

        jedis.sadd(SONG_KEY, songId);
        return true;
    }
}
