package org.chengy.model;


import org.springframework.data.annotation.Id;

import java.util.UUID;

/**
 * Created by nali on 2017/9/12.
 */
public class BaseEntity {
	public BaseEntity() {
		this.id = UUID.randomUUID().toString();
	}

	@Id
	private String id;

	String communityId;
	String community;

	public String getId() {
		return id;
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
}
