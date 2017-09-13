package org.chengy.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import java.util.UUID;

/**
 * Created by nali on 2017/9/12.
 */
@MappedSuperclass
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
