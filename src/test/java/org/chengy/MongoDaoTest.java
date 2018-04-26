package org.chengy;

import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.model.User;
import org.chengy.repository.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by nali on 2017/11/30.
 */

@RunWith(SpringRunner.class)
@SpringBootTest
public class MongoDaoTest {

	@Autowired
	UserRepository userRepository;

	@Test
	public void getCount() {

		User user = new User();
		user.setCommunity(Music163ApiCons.communityName);
		Example<User> userExample = Example.of(user, ExampleMatcher.matching()
				.withMatcher("community", match -> match.caseSensitive().exact())
				.withIgnorePaths("id"));
		long userCount = userRepository.count(userExample);

	}

}
