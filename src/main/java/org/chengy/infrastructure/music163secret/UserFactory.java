package org.chengy.infrastructure.music163secret;

import org.chengy.model.User;
import org.chengy.newmodel.Music163User;

import java.util.Date;

/**
 * Created by nali on 2017/9/15.
 */
public class UserFactory {
	/**
	 * 网易云音乐用户的工厂制造类
	 * @param area
	 * @param nickname
	 * @param avatar
	 * @param uid
	 * @param signature
	 * @param gender
	 * @return
	 */
	public static User buildUser(Date age, String area, String nickname, String avatar, String uid, String signature, int gender){

		User user = new User();
		user.setAge(age);
		user.setArea(area);
		user.setUsername(nickname);
		user.setAvatar(avatar);
		user.setCommunity(Music163ApiCons.communityName);
		user.setCommunityId(uid);
		user.setSignature(signature);
		user.setGender(gender);
		return user;
	}


	public static Music163User buildMusic163User(Date age, String area, String nickname, String avatar, String uid, String signature, int gender,int songNums){

		Music163User user = new Music163User();
		user.setAge(age);
		user.setArea(area);
		user.setUsername(nickname);
		user.setAvatar(avatar);
		user.setCommunity(Music163ApiCons.communityName);
		user.setId(uid);
		user.setSignature(signature);
		user.setGender(gender);
		user.setRecordSongNum(songNums);
		return user;
	}
}
