package org.chengy.repository;

import org.chengy.model.Song;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * Created by nali on 2017/9/27.
 */
public interface SongRepository extends MongoRepository<Song, String> {

	Song findSongByCommunityIdAndCommunity(String songId, String community);

	List<Song> findSongsByCommunityIdInAndCommunity(List<String> songList,String community);

	List<Song> findSongsByLyricist(String lyricist);

}
