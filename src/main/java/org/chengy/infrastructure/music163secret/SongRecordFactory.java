package org.chengy.infrastructure.music163secret;

import org.chengy.model.SongRecord;

/**
 * Created by nali on 2017/11/27.
 */
public class SongRecordFactory {


	public static SongRecord buildSongRecord(String commuId,String commuName,int loveNum,long score){
		SongRecord songRecord=new SongRecord();
		songRecord.setCommunity(commuName);
		songRecord.setCommunityId(commuId);
		songRecord.setLoveNum(loveNum);
		songRecord.setScore(score);
		return songRecord;
	}
}
