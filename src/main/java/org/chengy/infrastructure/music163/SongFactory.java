package org.chengy.infrastructure.music163;

import org.chengy.model.Music163Song;

import java.util.List;

/**
 * Created by nali on 2017/9/27.
 */
public class SongFactory {




	public static Music163Song buildMusic163Song(String communityId, String lyric, List<String> arts, String albumTitle, String albumId, String title, String composer, String lyricist) {

		Music163Song song  = new Music163Song();
		song.setAlbumId(albumId);
		song.setAlbumTitle(albumTitle);
		song.setArts(arts);
		song.setCommunity(Music163ApiCons.communityName);
		song.setId(communityId);
		song.setLyric(lyric);
		song.setLyricist(lyricist);
		song.setComposer(composer);
		song.setTitle(title);
		return song;
	}
}
