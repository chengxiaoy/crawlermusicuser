package org.chengy;


import com.baidu.aip.nlp.AipNlp;
import org.chengy.repository.remote.Music163SongRepository;
import org.chengy.util.BaiduApiUtil;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.HashMap;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class BaiduApiTest {

	@Autowired
	Music163SongRepository songRepository;




	@Test
	public void keywordTest() {

		String songId="214333";

		String title = "年轮";

		AipNlp client = BaiduApiUtil.getClient();


		JSONObject res = client.keyword(title, lyric, new HashMap<>());

		System.out.println(res.toString(2));

	}


	@Test
	public void lexTest() {

		AipNlp client = BaiduApiUtil.getClient();

		String text = "今天是一个好天气";
		JSONObject res = client.lexer(text, new HashMap<>());
		System.out.println(res.toString(2));

	}
}
