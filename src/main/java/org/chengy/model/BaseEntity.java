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

	public String getId() {
		return id;
	}

}
