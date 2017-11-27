package org.chengy.model;

/**
 * Created by nali on 2017/11/26.
 */

import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.Date;
import java.util.List;

/**
 * 对一条歌曲的信息记录
 */
@Document
@CompoundIndexes({
		@CompoundIndex(name = "userCommunity",def = "{'communityId':1,'community':1}")
})
public class SongRecord extends BaseEntity {
	/**
	 * 喜爱的分值
	 */
	private Long score;
	/**
	 * 喜爱的人数（top100中包含此歌的人数）
	 */
	private Integer loveNum;
	/**
	 * 可以将此歌作为特征的人的id记录
	 */
	private List<String> loverIds;

	@Version
	private Integer version;

	public Long getScore() {
		return score;
	}

	public void setScore(Long score) {
		this.score = score;
	}

	public Integer getLoveNum() {
		return loveNum;
	}

	public void setLoveNum(Integer loveNum) {
		this.loveNum = loveNum;
	}

	public List<String> getLoverIds() {
		return loverIds;
	}

	public void setLoverIds(List<String> loverIds) {
		this.loverIds = loverIds;
	}


	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}
}
