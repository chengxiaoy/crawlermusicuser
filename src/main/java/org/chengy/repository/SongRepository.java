package org.chengy.repository;

import org.chengy.model.Song;
import org.springframework.data.mongodb.repository.MongoRepository;

/**
 * Created by nali on 2017/9/27.
 */
public interface SongRepository extends MongoRepository<Song,String> {


}
