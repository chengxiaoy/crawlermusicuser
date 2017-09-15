package org.chengy.infrastructure.music163secret;

import org.chengy.model.User;

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
	public static User buildUser(String area,String nickname,String avatar,String uid,String signature,int gender){

		User user = new User();
		user.setArea(area);
		user.setUsername(nickname);
		user.setAvatar(avatar);
		user.setCommunity("163music");
		user.setCommunityId(uid);
		user.setSignature(signature);
		user.setGender(gender);
		return user;
	}

}
