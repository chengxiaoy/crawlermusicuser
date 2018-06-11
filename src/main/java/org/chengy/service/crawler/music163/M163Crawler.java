package org.chengy.service.crawler.music163;

public interface M163Crawler {
    CrawlerUserInfo getCrawlerInfo(String uid, boolean relativeUser, boolean userExit);
}
