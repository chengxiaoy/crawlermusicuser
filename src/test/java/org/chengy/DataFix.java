package org.chengy;

import org.chengy.service.statistics.Music163Statistics;
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
	@Autowired
	Music163Statistics music163Statistics;

	@Test
	public void fixCrawlerUserInfoTest() {
		fixCrawlerData.fixCrawlerUserInfoforDuplicate();
	}

	@Test
	public void getIdfTest() {
		double d =
				music163Statistics.calculateIDF((long) 4892, (long) 40);
		System.out.println(d);
	}


}
