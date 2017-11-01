package org.chengy.configuration;

import org.apache.http.HttpHost;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Created by nali on 2017/10/28.
 */
@Component
public class HttpConfig {


	private static String proxyHost;
	private static String proxyPort;

	public static HttpHost getHttpProxy(){
		return new HttpHost(proxyHost,Integer.parseInt(proxyPort));
	}
	@Value("${http.proxy.host}")
	public void setProxyHost(String proxyHost) {
		HttpConfig.proxyHost = proxyHost;
	}
	@Value("${http.proxy.port}")
	public void setProxyPort(String proxyPort) {
		HttpConfig.proxyPort = proxyPort;
	}
}
