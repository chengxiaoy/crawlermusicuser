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
import org.chengy.repository.UserRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import us.codecraft.xsoup.Xsoup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by nali on 2017/9/12.
 */
@Service
public class Crawler163music {
	@Autowired
	private UserRepository userRepository;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	MongoTemplate mongoTemplate;

	public void getUserInfo(String startId) throws Exception {
		LinkedList<String> ids = new LinkedList<>();
		ids.add(startId);
		while (ids.size() > 0) {
			try {
				String id = ids.peek();

				List<User> users =
						userRepository.findByCommunityIdAndCommunity(id, "163music");
				if (users.size() > 0) {
					continue;
				}

				String html = HttpHelper.get(Music163ApiCons.Music163UserHost + id);
				Document document = Jsoup.parse(html);
				int gender = document.select("#j-name-wrap > i").hasClass("u-icn-01") ? 1 : 0;
				String name = document.select("#j-name-wrap > span.tit.f-ff2.s-fc0.f-thide").get(0).html();
				//个性签名
				Elements signatureinfo = document.select("#head-box > dd > div.inf.s-fc3.f-brk");
				String signature = "";
				if (signatureinfo.size() > 0) {
					signature = signatureinfo.get(0).html().split("：")[1];
				}
				//年龄
				Elements ageinfo = document.select("#age");
				Date age = null;
				if (ageinfo.size() > 0) {
					age = new Date(Long.parseLong(ageinfo.get(0).attr("data-age")));
				}

				//地区的代码逻辑
				Elements elements = document.select("#head-box > dd > div:nth-child(4) > span:nth-child(1)");
				String area = "";
				if (elements.size() > 0) {
					area = elements.get(0).html().split("：")[1];
				} else {
					elements = document.select("#head-box > dd > div.inf.s-fc3 > span");
					if (elements.size() > 0) {
						area = elements.get(0).html().split("：")[1];
					}
				}

				String avatar = document.select("#ava > img").attr("src");
				User user = UserFactory.buildUser(age, area, name, avatar, id, signature, gender);
				System.out.println(user);
				List<String> songIds = getUserLikeSong(id);
				user.setLoveSongId(songIds);

				userRepository.save(user);
				System.out.println("save user succeed:" + user.getCommunityId());
				List<String> fansIds = getFansId(id);
				List<String> followedIds = getFollowedId(id);
				ids.addAll(fansIds);
				ids.addAll(followedIds);
			} catch (Exception e) {
				System.out.println(ids.get(0) + " get info failed");
				e.printStackTrace();
			} finally {
				ids.poll();
			}
		}

	}

	public List<String> getFansId(String uid) throws Exception {

		String fansParam = Music163ApiCons.getFansParams(uid, 1, 30);
		Document document = EncryptTools.commentAPI(fansParam, Music163ApiCons.fansUrl);
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

		JsonNode root = objectMapper.readTree(document.text());

		List<String> ids =
				root.findValue("follow").findValues("userId").stream().map(ob -> ob.asText()).collect(Collectors.toList());
		return ids;
	}


	public List<String> getUserLikeSong(String uid) throws Exception {

		String songRecordParam = Music163ApiCons.getSongRecordALLParams(uid, 1, 10);
		Document document = EncryptTools.commentAPI(songRecordParam, Music163ApiCons.songRecordUrl);
		JsonNode root = objectMapper.readTree(document.text());
		List<String> songIds =
				root.findValue("allData").findValues("song").stream()
						.limit(10).map(ob -> ob.get("id").asText()).collect(Collectors.toList());

		System.out.println(songIds);
		return songIds;
	}


	public static void main(String[] args) throws Exception {

		Crawler163music crawler163music = new Crawler163music();
		crawler163music.getUserInfo("13887683");
	}


}
