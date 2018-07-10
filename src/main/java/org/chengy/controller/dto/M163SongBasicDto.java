package org.chengy.controller.dto;

import lombok.Data;

import java.util.List;
//@Data
public class M163SongBasicDto {

	private String title;
	private List<String> arts;
	private String albumTitle;
	private String cover;

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public List<String> getArts() {
		return arts;
	}

	public void setArts(List<String> arts) {
		this.arts = arts;
	}

	public String getAlbumTitle() {
		return albumTitle;
	}

	public void setAlbumTitle(String albumTitle) {
		this.albumTitle = albumTitle;
	}

	public String getCover() {
		return cover;
	}

	public void setCover(String cover) {
		this.cover = cover;
	}
}
