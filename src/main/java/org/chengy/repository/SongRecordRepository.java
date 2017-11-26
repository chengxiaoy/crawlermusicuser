package org.chengy.repository;

import org.chengy.model.SongRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by nali on 2017/11/26.
 */
public interface SongRecordRepository extends MongoRepository<SongRecord,String>{

	SongRecord findSongRecordByCommunityIdAndCommunity(String commuId,String commuName);


}
