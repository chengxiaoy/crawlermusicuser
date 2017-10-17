package org.chengy.service.crawler;

import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.User;
import org.chengy.repository.SongRepository;
import org.chengy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * Created by nali on 2017/9/28.
 */
@Component
public class CrawlerLauncher {
	@Autowired
	UserRepository userRepository;
	@Autowired
	SongRepository songRepository;
	@Autowired
	Crawler163music crawler163music;
	@Autowired
	ThreadPoolTaskExecutor threadPoolTaskExecutor;

	public void saveMusic163SongByUser() {
		int pageIndex = 1;
		while (true) {
			int pageSize = 100;
			Pageable pageable = new PageRequest(pageIndex, pageSize);
			List<User> userList = userRepository.findAll(pageable).getContent();

			for (User user : userList) {
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						List<String> songIdlist = user.getLoveSongId();
						if (songIdlist != null && !CollectionUtils.isEmpty(songIdlist)) {
							for (String songId : songIdlist) {
								try {
									crawler163music.getSongInfo(songId);
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
			System.out.println("page is at :" + pageIndex);
		}

	}

	public void saveMusic163User() {
		Random random = new Random();
		int rand = random.nextInt(200);
		System.out.println(rand);
		int threadNums = 13;
		Pageable pageable = new PageRequest(rand, threadNums);
		List<String> listStr = userRepository.findAll(pageable).getContent().stream().map(ob -> ob.getCommunityId()).collect(Collectors.toList());
		System.out.println(listStr);
		Iterator strItr = listStr.iterator();
		for (int i = 0; i < threadNums; i++) {
			Thread thread = new Thread(new Runnable() {
				@Override
				public void run() {
					String communityId = (String) strItr.next();
					User user = userRepository.findByCommunityIdAndCommunity(communityId, Music163ApiCons.communityName);
					userRepository.delete(user);
					crawler163music.getUserInfo(communityId);
				}
			});
			thread.start();
		}
	}

	/**
	 * 取用户十条最爱的歌曲信息不够 扩大为100条
	 */
	public void fixMuser163User() {
		while (true) {
			int pageSize = 100;
			Pageable pageable = new PageRequest(0, pageSize);
			List<User> userList = userRepository.findAll(pageable).getContent();

			for (User user : userList) {
				Runnable runnable = new Runnable() {
					@Override
					public void run() {
						try {
							String uid = user.getCommunityId();
							List<String> songids = null;
							songids = crawler163music.getUserLikeSong(uid);
							user.setLoveSongId(songids);
							userRepository.save(user);
						}catch (Exception e){
							System.out.println("failed get song for user:"+user);
						}

					}
				};
				threadPoolTaskExecutor.execute(runnable);
			}
		}
	}

}
