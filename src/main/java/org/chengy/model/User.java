package org.chengy.model;

import javax.persistence.Entity;

/**
 * Created by nali on 2017/9/12.
 */

@Entity
public class User extends BaseEntity {

	private String username;
	private String avatar;
	private String signature;
	private int age;
	private int gender;
	private String area;
	private String communityId;
	private String community;


	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getAvatar() {
		return avatar;
	}

	public void setAvatar(String avatar) {
		this.avatar = avatar;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public int getGender() {
		return gender;
	}

	public void setGender(int gender) {
		this.gender = gender;
	}

	public String getArea() {
		return area;
	}

	public void setArea(String area) {
		this.area = area;
	}

	public String getCommunityId() {
		return communityId;
	}

	public void setCommunityId(String communityId) {
		this.communityId = communityId;
	}


	public String getCommunity() {
		return community;
	}

	public void setCommunity(String community) {
		this.community = community;
	}

	@Override
	public String toString() {
		return username + "@" + community + " in " + area + "：" + signature;
	}
}
