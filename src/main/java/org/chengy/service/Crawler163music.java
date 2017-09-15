package org.chengy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.chengy.core.HttpHelper;
import org.chengy.infrastructure.music163secret.EncryptTools;
import org.chengy.infrastructure.music163secret.Music163ApiCons;
import org.chengy.infrastructure.music163secret.UserFactory;
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
import java.util.stream.Collectors;

/**
 * Created by nali on 2017/9/12.
 */
public class Crawler163music {


	public void getUserInfo(String id) throws Exception {
		String html = HttpHelper.get(Music163ApiCons.Music163UserHost + id);
		Document document = Jsoup.parse(html);
		int gender = document.select("#j-name-wrap > i").hasClass("u-icn-01") ? 1 : 0;
		String name = document.select("#j-name-wrap > span.tit.f-ff2.s-fc0.f-thide").get(0).html();
		String signature = document.select("#head-box > dd > div.inf.s-fc3.f-brk").get(0).html().split("：")[1];
		String area = document.select("#head-box > dd > div:nth-child(4) > span:nth-child(1)").get(0).html().split("：")[1];
		String avatar = document.select("#ava > img").attr("src");
		User user= UserFactory.buildUser(area,name,avatar,id,signature,gender);
		



	}

	public List<String> getFansId(String uid) throws Exception {

		String fansParam = Music163ApiCons.getFansParams(uid, 1, 30);
		Document document = EncryptTools.commentAPI(fansParam, Music163ApiCons.fansUrl);
		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode root = objectMapper.readTree(document.text());
		List<JsonNode> jsonNodeList =
				root.findValue("followeds").findValues("userId");
		List<String> ids =
				jsonNodeList.stream().map(JsonNode::asText).collect(Collectors.toList());
		return ids;
	}

	public List<String> getFollowedId(String uid) throws Exception {
		String followedParam = Music163ApiCons.getFollowedParams(uid, 1, 30);
		Document document = EncryptTools.commentAPI(followedParam, Music163ApiCons.getFollowedUrl(uid));

		ObjectMapper objectMapper = new ObjectMapper();
		JsonNode root = objectMapper.readTree(document.text());
		List<String> ids =
				root.findValue("follow").findValues("userId").stream().map(ob -> ob.asText()).collect(Collectors.toList());
		return ids;
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
		crawler163music.getFollowedId("330313");
	}


}
