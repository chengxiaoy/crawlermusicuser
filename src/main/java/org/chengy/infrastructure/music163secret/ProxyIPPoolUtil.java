package org.chengy.infrastructure.music163secret;

import com.fasterxml.jackson.core.SerializableString;
import com.google.common.base.Stopwatch;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.ConcurrentHashSet;
import io.vertx.core.net.ProxyOptions;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.http.StreamingHttpOutputMessage;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;


@Component
public class ProxyIPPoolUtil {

    private static String xiciUrl = "http://www.xicidaili.com/nn";

    private static WebClient webClient = null;

    private static Vertx vertx = null;

    private static String targetUrl = Music163ApiCons.Music163UserHost + "330313";

    private static Set<Pair<String, Integer>> ipSet = new HashSet<>();


    @PostConstruct
    public void init(){
        vertx = Vertx.vertx();
        WebClientOptions webClientOptions = new WebClientOptions();
        webClientOptions.setMaxPoolSize(5).setConnectTimeout(1000).setKeepAlive(true);
        webClient = WebClient.create(vertx, webClientOptions);
        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                try {
                    Set<Pair<String, Integer>> pairSet = getXiciProxyIps();
                    ipSet.addAll(validateProxyIp(pairSet, targetUrl));
                } catch (Exception e) {
                    System.out.println("get xici ipsets error");
                }

            }
        }, 0, 3, TimeUnit.MINUTES);
    }


    public  Pair<String, Integer> peekIp() {
        return ipSet.iterator().next();
    }


    public static void main(String[] args) throws ExecutionException, InterruptedException {
        Stopwatch stopwatch = Stopwatch.createStarted();

        Set<Pair<String, Integer>> list = getXiciProxyIps();
        System.out.println(list);
        System.out.println("before validate, ip size :" + list.size());
        list = validateProxyIp(list, Music163ApiCons.Music163UserHost + "330313");
        System.out.println("after validate, ip size :" + list.size());

        stopwatch.stop();
        System.out.println("process running: " + stopwatch.elapsed(TimeUnit.SECONDS) + "s");
    }


    /**
     * 通过目标网址校验 ip的可用性
     *
     * @param needValidateIps
     * @param targetUrl
     * @return
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static Set<Pair<String, Integer>> validateProxyIp(Set<Pair<String, Integer>> needValidateIps, String targetUrl) throws ExecutionException, InterruptedException {
        CompletableFuture<Set<Pair<String, Integer>>> future = new CompletableFuture<>();
        Set<Pair<String, Integer>> res = new ConcurrentHashSet<>();
        AtomicInteger count = new AtomicInteger();

        needValidateIps.stream().forEach(pair -> {
            WebClientOptions webClientOptions = new WebClientOptions();
            webClientOptions.setMaxPoolSize(1).setConnectTimeout(1000).setKeepAlive(false);
            webClientOptions.setProxyOptions(new ProxyOptions().setHost(pair.getKey()).setPort(pair.getValue()));
            WebClient webClient = WebClient.create(vertx, webClientOptions);

            HttpRequest<io.vertx.core.buffer.Buffer> httpRequest = webClient.getAbs(targetUrl);
            httpRequest.send(ar -> {
                if (ar.succeeded()) {
                    HttpResponse<Buffer> response = ar.result();
                    if (response.statusCode() == 200) {
                        res.add(new ImmutablePair<>(pair.getLeft(), pair.getRight()));
                    }
                    if (count.getAndIncrement() == needValidateIps.size() - 1) {
                        future.complete(res);
                    }
                } else if (ar.failed()) {
                    if (count.getAndIncrement() == needValidateIps.size() - 1) {
                        future.complete(res);
                    }
                }
            });
        });
        return future.get();
    }


    /**
     * 获取xici的高匿ip
     *
     * @return
     */
    public static Set<Pair<String, Integer>> getXiciProxyIps() {

        Set<Pair<String, Integer>> pairSet = new ConcurrentHashSet<>(16);
        // 爬去xici的前三页
        List<String> urls = Arrays.asList(xiciUrl, xiciUrl + "/2", xiciUrl + "/3");
        urls.forEach(url -> {
                    CompletableFuture<String> futureHtml = new CompletableFuture<>();

                    HttpRequest<io.vertx.core.buffer.Buffer> httpRequest = webClient.getAbs(url);
                    httpRequest.send(ar -> {
                        if (ar.succeeded()) {
                            HttpResponse<Buffer> response = ar.result();
                            if (response.statusCode() == 200) {
                                String html = response.body().toString(StandardCharsets.UTF_8);
                                futureHtml.complete(html);
                            }
                            futureHtml.complete(response.statusCode() + "");
                        } else if (ar.failed()) {
                            futureHtml.completeExceptionally(ar.cause());
                        }
                    });

                    try {
                        String html = futureHtml.get();

                        Document document = Jsoup.parse(html);
                        Element element = document.select("#ip_list > tbody").get(0);
                        Elements elements = element.getElementsByTag("tr");
                        List<Pair<String, Integer>> pageList = elements.subList(1, elements.size()).stream().map(ob -> {
                            String ip = ob.getElementsByTag("td").get(1).text().trim();
                            Integer port = Integer.valueOf(ob.getElementsByTag("td").get(2).text().trim());
                            return new ImmutablePair<String, Integer>(ip, port);
                        }).collect(Collectors.toList());
                        pairSet.addAll(pageList);
                    } catch (Exception e) {
                        System.out.println("page url extract info error" + e.getMessage());
                    }
                }
        );
        return pairSet;
    }
}
