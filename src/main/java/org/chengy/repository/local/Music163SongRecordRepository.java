package org.chengy.repository.local;

import org.chengy.model.Music163SongRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface Music163SongRecordRepository extends MongoRepository<Music163SongRecord,String> {

}
