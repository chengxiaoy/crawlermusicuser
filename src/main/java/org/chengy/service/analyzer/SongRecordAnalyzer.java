package org.chengy.service.analyzer;

import com.oracle.tools.packager.mac.MacAppBundler;
import org.apache.commons.lang3.tuple.Pair;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.infrastructure.music163secret.SongRecordFactory;
import org.chengy.model.SongRecord;
import org.chengy.model.User;
import org.chengy.repository.SongRecordRepository;
import org.chengy.repository.SongRepository;
import org.chengy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Component
public class SongRecordAnalyzer {

    @Autowired
    UserRepository userRepository;
    @Autowired
    SongRecordRepository songRecordRepository;

    @Autowired
    @Qualifier("songExecutor")
    ThreadPoolTaskExecutor threadPoolTaskExecutor;

    public void getSongRecordInfo() {
        int pageIndex = 0;
        int pageSize = 100;
        Pageable pageable = new PageRequest(pageIndex, pageSize);
        List<User> userList = userRepository.findAll(pageable).getContent();

        while (userList.size() > 0) {
            userList = userList.stream().filter(ob -> ob.getLoveSongId().size() > 0).collect(Collectors.toList());
            List<String> userIds = userList.stream().filter(ob -> ob.getSongRecord() == null || !ob.getSongRecord()).map(ob -> ob.getCommunityId()).collect(Collectors.toList());
            for (String uid : userIds) {
                Runnable runnable = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            getSongRecord(uid);
                            System.out.println("get uid " + uid + " song record info success");
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("get uid " + uid + " song record info failed");
                            User user =
                                    userRepository.findByCommunityIdAndCommunity(uid, Music163ApiCons.communityName);
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
     * 记录歌曲信息
     *
     * @param userid
     * @throws Exception
     */
    public void getSongRecord(String userid) throws Exception {

        User user = userRepository.findByCommunityIdAndCommunity(userid, Music163ApiCons.communityName);
        if (user.getSongRecord() != null) {
            return;
        }


        List<Pair<String, Integer>> recordInfo = user.getSongScore();

        for (Pair<String, Integer> pair : recordInfo) {
            SongRecord songRecord =
                    songRecordRepository.findSongRecordByCommunityIdAndCommunity(pair.getKey(), Music163ApiCons.communityName);

            if (songRecord == null) {
                SongRecord newSongRecord = SongRecordFactory.buildSongRecord(pair.getKey(), Music163ApiCons.communityName, 1, (long) pair.getValue(), userid);
                songRecordRepository.save(newSongRecord);
            } else {
                try {
                    songRecord.setScore(songRecord.getScore() + pair.getValue());
                    songRecord.setLoveNum(songRecord.getLoveNum() + 1);
                    songRecord.getLoverIds().add(userid);
                    songRecordRepository.save(songRecord);
                } catch (OptimisticLockingFailureException e) {
                    System.out.println("retry update songRecord");

                    songRecord = songRecordRepository.findSongRecordByCommunityIdAndCommunity(songRecord.getCommunityId(), Music163ApiCons.communityName);
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
     * @param songIds
     * @return
     */
    public Map<String, Double> getSongAverageScore(Collection<String> songIds) {

        List<SongRecord> songRecordList = songRecordRepository.findSongRecordsByCommunityIdInAndCommunity(songIds, Music163ApiCons.communityName);
        Map<String, Double> map = songRecordList.stream().collect(Collectors.toMap(ob -> ob.getCommunityId(), ob -> {
            int loveNums = ob.getLoveNum();
            Long sumScore = ob.getScore();
            return  new BigDecimal(sumScore).divide(new BigDecimal(loveNums),5,BigDecimal.ROUND_HALF_DOWN).doubleValue();
        }));
        return map;
    }

}
