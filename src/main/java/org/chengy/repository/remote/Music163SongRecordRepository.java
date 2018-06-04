package org.chengy.repository.remote;

import org.chengy.model.SongRecord;
import org.chengy.newmodel.Music163SongRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;
import java.util.List;

public interface Music163SongRecordRepository extends MongoRepository<Music163SongRecord,String> {

}
