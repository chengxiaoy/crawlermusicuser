package org.chengy.util;

import org.chengy.model.User;
import org.chengy.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

/**
 * Created by nali on 2017/11/4.
 */
@Component
public class FixCrawlerData {
	@Autowired
	UserRepository userRepository;

	/**
	 * remove the duplicate data
	 */
	public void fixCrawlerUserInfoforDuplicate() {

		HashSet<String> music163Ids = new HashSet<>();

		int pageId = 0;
		int pageSize = 1000;
		Pageable pageable = new PageRequest(pageId, pageSize);
		int pageMax = userRepository.findAll(pageable).getTotalPages();
		int deleteNum = 0;
		while (pageId < pageMax) {
			List<User> userList = userRepository.findAll(pageable).getContent();
			for (User user : userList) {
				if (!music163Ids.add(user.getCommunityId())) {
					userRepository.delete(user.getId());
					deleteNum++;
				}
			}


		}
		System.out.println("already delete "+deleteNum+" duplicate user");
	}


}
