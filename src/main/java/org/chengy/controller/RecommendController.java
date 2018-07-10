package org.chengy.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Stopwatch;
import org.chengy.controller.dto.M163SongBasicDto;
import org.chengy.model.Music163Song;
import org.chengy.service.discovery.Music163Discovery;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@CrossOrigin("*")
@Controller
public class RecommendController {

	@Autowired
	Music163Discovery music163Discovery;

	@RequestMapping(value = "/hello", method = RequestMethod.GET)
	@ResponseBody
	public List<M163SongBasicDto> hello(){
		M163SongBasicDto m163SongBasicDto=new M163SongBasicDto();
		m163SongBasicDto.setTitle("hehe");
		List<M163SongBasicDto> m163SongBasicDtoList=new ArrayList<>();
		m163SongBasicDtoList.add(m163SongBasicDto);
		return m163SongBasicDtoList;
	}

	@RequestMapping(value = "/recommend/model", method = RequestMethod.GET)
	@ResponseBody
	public List<M163SongBasicDto> getUserModelRecommendSongs(@RequestParam("uid") String uid) {

		Stopwatch stopwatch=Stopwatch.createStarted();
		List<Music163Song> music163SongList = music163Discovery.getRecommendSongs(uid, 200, 10);
		System.out.println("model recommend duration is "+stopwatch.elapsed(TimeUnit.SECONDS)+"s");
		return music163SongList.stream().map(this::transfer).collect(Collectors.toList());
	}

	@RequestMapping(value = "/recommend/content", method = RequestMethod.GET)
	@ResponseBody
	public List<M163SongBasicDto> getUserContentRecommendSongs(@RequestParam("uid") String uid){
		Stopwatch stopwatch=Stopwatch.createStarted();
		List<Music163Song> itemBasedRecommend = music163Discovery.itemBasedRecommend(uid, 100, 5);
		System.out.println("content item recommend duration is "+stopwatch.elapsed(TimeUnit.SECONDS)+"s");
		stopwatch.reset().start();
		List<Music163Song> userBasedRecommend = music163Discovery.userBasedRecommend(uid, 100, 5);
		System.out.println("content user recommend duration is "+stopwatch.elapsed(TimeUnit.SECONDS)+"s");
		itemBasedRecommend.addAll(userBasedRecommend);
		return itemBasedRecommend.stream().map(this::transfer).collect(Collectors.toList());
	}


	@RequestMapping(value = "/song/similar",method = RequestMethod.GET)
	@ResponseBody
	public List<M163SongBasicDto> getSimilarSongs(@RequestParam("songId") String songId) throws JsonProcessingException {
		List<Music163Song> music163SongList=music163Discovery.getSimilarSongs(songId,10,false);
		return music163SongList.stream().map(this::transfer).collect(Collectors.toList());
	}


	private M163SongBasicDto transfer(Music163Song music163Song) {
		M163SongBasicDto m163SongBasicDto=new M163SongBasicDto();
		m163SongBasicDto.setAlbumTitle(music163Song.getAlbumTitle());
		m163SongBasicDto.setArts(music163Song.getArts());
		m163SongBasicDto.setCover(music163Song.getCover());
		m163SongBasicDto.setTitle(music163Song.getTitle());
		return m163SongBasicDto;
	}


}
