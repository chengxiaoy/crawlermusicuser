package org.chengy.repository.remote;

import org.chengy.model.Song;
import org.chengy.newmodel.Music163Song;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface Music163SongRepository extends MongoRepository<Music163Song, String> {
     List<Music163Song> findMusic163SongsByLyricist(String lyricist);
}
