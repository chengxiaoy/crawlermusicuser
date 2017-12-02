package org.chengy;

import org.chengy.repository.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
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

		long count = userRepository.countAllBySongRecordIsTrue();
		System.out.println(count);

	}

}
