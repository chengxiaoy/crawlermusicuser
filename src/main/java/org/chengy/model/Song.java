package org.chengy.model;

import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

/**
 * Created by nali on 2017/9/13.
 */

public class Song extends BaseEntity {

	private String title;
	private String albumTitle;
	private List<String> arts;
	private List<String> tags;
	private String category;
	private String language;


	private String community;
	private String communityId;


	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getAlbumTitle() {
		return albumTitle;
	}

	public void setAlbumTitle(String albumTitle) {
		this.albumTitle = albumTitle;
	}

	public List<String> getArts() {
		return arts;
	}

	public void setArts(List<String> arts) {
		this.arts = arts;
	}

	public List<String> getTags() {
		return tags;
	}

	public void setTags(List<String> tags) {
		this.tags = tags;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public String getCommunity() {
		return community;
	}

	public void setCommunity(String community) {
		this.community = community;
	}

	public String getCommunityId() {
		return communityId;
	}

	public void setCommunityId(String communityId) {
		this.communityId = communityId;
	}
}
