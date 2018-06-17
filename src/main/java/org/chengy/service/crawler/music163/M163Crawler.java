package org.chengy.service.crawler.music163;

import java.util.concurrent.CompletableFuture;

public interface M163Crawler {
    CrawlerUserInfo getUserInfo(String uid, boolean relativeUser, boolean userExit);
}
