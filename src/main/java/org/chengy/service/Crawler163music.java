package org.chengy.service;

import org.chengy.core.HttpHelper;
import org.chengy.model.Song;
import org.chengy.model.User;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import us.codecraft.xsoup.Xsoup;

import java.io.IOException;
import java.util.List;

/**
 * Created by nali on 2017/9/12.
 */
public class Crawler163music {


	private static final String Music163UserHost = "https://music.163.com/user/home?id=";
	private static final String Music163UserFans = "https://music.163.com/user/fans?id";

	public void getUserInfo(String id) throws Exception {
		String html = HttpHelper.get(Music163UserHost + id);
		Document document = Jsoup.parse(html);

		int gender = document.select("#j-name-wrap > i").hasClass("u-icn-01") ? 1 : 0;
		String name = document.select("#j-name-wrap > span.tit.f-ff2.s-fc0.f-thide").get(0).html();
		String signature = document.select("#head-box > dd > div.inf.s-fc3.f-brk").get(0).html().split("：")[1];
		String area = document.select("#head-box > dd > div:nth-child(4) > span:nth-child(1)").get(0).html().split("：")[1];
		String avatar = document.select("#ava > img").attr("src");
		User user = new User();
		user.setArea(area);
		user.setUsername(name);
		user.setAvatar(avatar);
		user.setCommunity("163music");
		user.setCommunityId(id);
		user.setSignature(signature);
		user.setGender(gender);
		System.out.println(user);
	}

	public List<String> getFansId(String id) throws Exception {
		String html = HttpHelper.get(Music163UserFans + id);
		Document document = Jsoup.parse(html);


		return null;
	}


	public List<Song> getUserLikeSong(Document document) {

		Elements elements =
				document.select("#auto-id-K0WplQveJGEMIk40 > ul");

		for (Element element : elements) {

		}
		return null;

	}


	public static void main(String[] args) throws Exception {

		Crawler163music crawler163music = new Crawler163music();
		crawler163music.getUserInfo("330313");

	}


}
