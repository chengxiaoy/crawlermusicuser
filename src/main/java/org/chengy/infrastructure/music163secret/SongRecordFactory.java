package org.chengy.infrastructure.music163secret;

import org.chengy.model.SongRecord;
import org.chengy.newmodel.Music163SongRecord;

import java.util.Arrays;
import java.util.Collections;

/**
 * Created by nali on 2017/11/27.
 */
public class SongRecordFactory {


    public static SongRecord buildSongRecord(String commuId, String commuName, int loveNum, long score, String uid) {
        SongRecord songRecord = new SongRecord();
        songRecord.setCommunity(commuName);
        songRecord.setCommunityId(commuId);
        songRecord.setLoveNum(loveNum);
        songRecord.setScore(score);
        songRecord.setLoverIds(Collections.singletonList(uid));
        return songRecord;
    }

    public static Music163SongRecord buildMusic163SongRecord(String commuId, String commuName, int loveNum, long score, String uid) {
        Music163SongRecord music163SongRecord = new Music163SongRecord();
        music163SongRecord.setCommunity(commuName);
        music163SongRecord.setId(commuId);
        music163SongRecord.setLoveNum(loveNum);
        music163SongRecord.setScore(score);
        music163SongRecord.setLoverIds(Collections.singletonList(uid));
        return music163SongRecord;
    }


}
