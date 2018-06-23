package org.chengy.repository.local;

import org.chengy.model.Music163Song;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

import java.util.List;


public interface LocalMusic163SongRepository extends MongoRepository<Music163Song, String> {
     List<Music163Song> findMusic163SongsByLyricist(String lyricist);
}
