package org.chengy.service.analyzer;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.infrastructure.music163secret.SongRecordFactory;
import org.chengy.newmodel.Music163SongRecord;
import org.chengy.newmodel.Music163User;
import org.chengy.repository.remote.Music163SongRecordRepository;
import org.chengy.repository.remote.Music163UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
public class SongRecordAnalyzer {

    @Autowired
    Music163UserRepository userRepository;
    @Autowired
    Music163SongRecordRepository songRecordRepository;

    @Autowired
    @Qualifier("songExecutor")
    ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public void getSongRecordInfo() {
        int pageIndex = 0;
        int pageSize = 100;
        Pageable pageable = new PageRequest(pageIndex, pageSize);
        List<Music163User> userList = userRepository.findAll(pageable).getContent();

        while (userList.size() > 0) {
            userList = userList.stream().filter(ob -> ob.getLoveSongId().size() > 0).collect(Collectors.toList());
            List<String> userIds = userList.stream().filter(ob -> ob.getSongRecord() == null || !ob.getSongRecord()).map(ob -> ob.getId()).collect(Collectors.toList());
            for (String uid : userIds) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            saveUserSongRecord(uid);
                            System.out.println("save uid " + uid + " song record info success");
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("save uid " + uid + " song record info failed");
                            Music163User user =
                                    userRepository.findById(uid).orElse(null);
                            user.setSongRecord(false);
                            userRepository.save(user);
                        }
                    }
                };
                threadPoolTaskExecutor.execute(runnable);
            }
            pageIndex++;
            pageable = new PageRequest(pageIndex, pageSize);
            userList = userRepository.findAll(pageable).getContent();
        }

        System.out.println("======getSongRecordInfo over======");

    }


    /**
     * 记录用户的歌曲信息
     *
     * @param userid
     * @throws Exception
     */
    public void saveUserSongRecord(String userid) throws Exception {

        Music163User user = userRepository.findById(userid).orElse(null);
        if (user.getSongRecord() != null) {
            return;
        }


        List<Pair<String, Integer>> recordInfo = user.getSongScore();

        for (Pair<String, Integer> pair : recordInfo) {
            Music163SongRecord songRecord =
                    songRecordRepository.findById(pair.getKey()).orElse(null);
            if (songRecord == null) {
                Music163SongRecord newSongRecord = SongRecordFactory.buildMusic163SongRecord(pair.getKey(), Music163ApiCons.communityName, 1, (long) pair.getValue(), userid);
                songRecordRepository.save(newSongRecord);
            } else {
                try {
                    songRecord.setScore(songRecord.getScore() + pair.getValue());
                    songRecord.setLoveNum(songRecord.getLoveNum() + 1);
                    songRecord.getLoverIds().add(userid);
                    songRecordRepository.save(songRecord);
                } catch (OptimisticLockingFailureException e) {
                    System.out.println("retry update songRecord");
                    songRecord = songRecordRepository.findById(songRecord.getId()).orElse(null);
                    songRecord.setScore(songRecord.getScore() + pair.getValue());
                    songRecord.setLoveNum(songRecord.getLoveNum() + 1);
                    songRecord.getLoverIds().add(userid);
                    songRecordRepository.save(songRecord);
                }
            }
        }

        user.setSongRecord(true);
        userRepository.save(user);
    }


    /**
     * 歌曲的平均得分
     *
     * @param songIds
     * @return
     */
    public Map<String, Double> getSongAverageScore(Collection<String> songIds) {

        Iterator<Music163SongRecord> songRecordIterator = songRecordRepository.findAllById(songIds).iterator();
        List<Music163SongRecord> songRecordList = Lists.newArrayList(songRecordIterator);
        Map<String, Double> map = songRecordList.stream().collect(Collectors.toMap(ob -> ob.getId(), ob -> {
            int loveNums = ob.getLoveNum();
            Long sumScore = ob.getScore();
            return new BigDecimal(sumScore).divide(new BigDecimal(loveNums), 5, BigDecimal.ROUND_HALF_DOWN).doubleValue();
        }));
        return map;
    }

}
