package org.chengy.repository;

import org.chengy.model.SongRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

/**
 * Created by nali on 2017/11/26.
 */
public interface SongRecordRepository extends MongoRepository<SongRecord,String>{

	SongRecord findSongRecordByCommunityIdAndCommunity(String commuId,String commuName);

	List<SongRecord> findSongRecordsByCommunityIdInAndCommunity(Collection<String> commuids, String commuName);
}
