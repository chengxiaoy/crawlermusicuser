package org.chengy.core;


import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Created by nali on 2017/9/12.
 */

public class HttpHelper {

	public static CloseableHttpClient client() {
		return HttpClients.custom().
				setConnectionManager(HTTPConnectionManager.getConnectionManager()).setMaxConnPerRoute(200)
				.setMaxConnPerRoute(200).build();

	}

	public static String get(String url) throws Exception {

		HttpGet get = new HttpGet(url);
		get.addHeader(HttpConstants.USER_AGENT, HttpConstants.CHROME_V55);
		get.addHeader(HttpConstants.REFERER, "https://music.163.com/");
		get.addHeader(HttpConstants.HOST, "music.163.com");
		CloseableHttpResponse response = null;
		try {
			response = client().execute(get);
			if (response.getStatusLine().getStatusCode()==200){
				return EntityUtils.toString(response.getEntity());
			}else {
				throw new Exception("response code is "+response.getStatusLine().getStatusCode());
			}
		} finally {
			if (response != null) {
				response.close();
			}
		}
	}

}
