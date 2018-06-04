package org.chengy.repository.local;

import org.chengy.newmodel.Music163Song;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface SongRepository extends MongoRepository<Music163Song, String> {
}
