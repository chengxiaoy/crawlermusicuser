package org.chengy.service.analyzer;

import com.google.common.collect.Lists;
import org.apache.commons.lang3.tuple.Pair;
import org.chengy.infrastructure.music163.Music163ApiCons;
import org.chengy.infrastructure.music163.SongRecordFactory;
import org.chengy.model.BaseModel;
import org.chengy.model.Music163SongRecord;
import org.chengy.model.Music163User;
import org.chengy.repository.remote.Music163SongRecordRepository;
import org.chengy.repository.remote.Music163UserRepository;
import org.chengy.util.RedisUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;


@Component
public class SongRecordAnalyzer {

	private static final Logger LOGGER = LoggerFactory.getLogger(SongRecordAnalyzer.class);


	@Autowired
	Music163UserRepository userRepository;
	@Autowired
	Music163SongRecordRepository songRecordRepository;

	@Autowired
	@Qualifier("songExecutor")
	ThreadPoolTaskExecutor threadPoolTaskExecutor;


	public void saveSongRecordInfo() {
		Set<String> ids = RedisUtil.smembers("user_id");

		for (String uid : ids) {
			if (!RedisUtil.sismember("u_record", uid)) {

				Music163User user = userRepository.findById(uid).orElse(null);

				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						try {
							saveUserSongRecord(user);
						} catch (Exception e) {
							e.printStackTrace();
						}
						RedisUtil.sadd("u_record", uid);
						LOGGER.info("song info of {} record success", uid);
					}
				};
				threadPoolTaskExecutor.execute(runnable);

			}
		}


	}


	/**
	 * 记录用户的歌曲信息
	 *
	 * @param user
	 * @throws Exception
	 */
	public void saveUserSongRecord(Music163User user) throws Exception {

		if (user == null) {
			return;
		}

		List<Pair<String, Integer>> recordInfo = user.getSongScore();

		for (Pair<String, Integer> pair : recordInfo) {
			Music163SongRecord songRecord =
					songRecordRepository.findById(pair.getKey()).orElse(null);
			if (songRecord == null) {
				Music163SongRecord newSongRecord = SongRecordFactory
						.buildMusic163SongRecord(pair.getKey(), Music163ApiCons.communityName, 1, (long) pair.getValue(), user.getId());
				songRecordRepository.save(newSongRecord);
			} else {
				try {
					if (songRecord.getLoverIds().contains(user.getId())) {
						continue;
					}

					songRecord.setScore(songRecord.getScore() + pair.getValue());
					songRecord.setLoveNum(songRecord.getLoveNum() + 1);
					songRecord.getLoverIds().add(user.getId());
					songRecordRepository.save(songRecord);
				} catch (OptimisticLockingFailureException e) {
					System.out.println("retry update songRecord");
					songRecord = songRecordRepository.findById(songRecord.getId()).orElse(null);
					songRecord.setScore(songRecord.getScore() + pair.getValue());
					songRecord.setLoveNum(songRecord.getLoveNum() + 1);
					songRecord.getLoverIds().add(user.getId());
					songRecordRepository.save(songRecord);
				}
			}
		}
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
