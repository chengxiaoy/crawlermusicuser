package org.chengy;

import org.chengy.util.FixCrawlerData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Created by nali on 2017/11/4.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class DataFix {
	@Autowired
	FixCrawlerData fixCrawlerData;

	@Test
	public void fixCrawlerUserInfoTest() {
		fixCrawlerData.fixCrawlerUserInfoforDuplicate();
	}

}
