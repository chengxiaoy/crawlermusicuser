package org.chengy;

import org.chengy.core.HttpHelper;
import org.chengy.service.crawler.music163.Vertx163Muisc;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.Set;


@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest
@ActiveProfiles("test")
public class RedisTest {


    @Autowired
    Vertx163Muisc vertx163Muisc;



    @Test
    public void getSongImage() throws Exception {

        vertx163Muisc.getSongInfo("536570127");
    }

    public static void main(String[] args) throws Exception {
        String html = HttpHelper.get("https://music.163.com/#/song?id=334981");

        Document document = new Document(html);
        Elements elements = document.select("div.u-cover.u-cover-6.f-fl > img");

        System.out.println(elements.get(0).text());
    }
}
