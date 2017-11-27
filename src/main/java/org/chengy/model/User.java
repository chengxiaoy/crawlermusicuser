package org.chengy.model;

import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * Created by nali on 2017/9/12.
 */

@Document
@CompoundIndexes({
		@CompoundIndex(name = "userCommunity",def = "{'communityId':1,'community':1}")
})
public class User extends BaseEntity {

	private String username;
	private String avatar;
	private String signature;
	private Date age;
	/**
	 * 1:male 2:female
	 * 0 present unknow
	 */
	private int gender;
	private String area;

	private Boolean SongRecord;

	private List<String> loveSongId;


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


	@Override
	public String toString() {
		return username + "@" + community + " in " + area + "：" + signature;
	}

	public List<String> getLoveSongId() {
		return loveSongId;
	}

	public void setLoveSongId(List<String> loveSongId) {
		this.loveSongId = loveSongId;
	}

	public Date getAge() {
		return age;
	}

	public void setAge(Date age) {
		this.age = age;
	}

	public Boolean getSongRecord() {
		return SongRecord;
	}

	public void setSongRecord(Boolean songRecord) {
		SongRecord = songRecord;
	}
}
