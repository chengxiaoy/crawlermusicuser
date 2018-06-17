package org.chengy.service.crawler.music163;

import java.util.concurrent.CompletableFuture;

public interface M163CrawlerAsync {
    CompletableFuture<CrawlerUserInfo> getUserInfoAsync(String uid, boolean relativeUser, boolean userExit);

}
