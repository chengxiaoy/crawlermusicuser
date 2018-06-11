package org.chengy.controller;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Preconditions;
import org.apache.commons.lang3.StringUtils;
import org.chengy.dto.M163SongDto;
import org.chengy.newmodel.Music163Song;
import org.chengy.service.discovery.Music163Discovery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;
import java.util.stream.Collectors;

@RequestMapping("/status")
@Controller
@CrossOrigin("*")
public class DiscoveryController {

    @Autowired
    Music163Discovery music163Discovery;

    public List<M163SongDto> getM163SimilarSong(String songId) throws JsonProcessingException {
        Preconditions.checkArgument(StringUtils.isNotEmpty(songId));
        List<Music163Song> music163SongList = music163Discovery.getSimilarSongs(songId, 5, false);
        return music163SongList.stream().map(ob -> transferSong(ob)).collect(Collectors.toList());
    }



    public M163SongDto transferSong(Music163Song music163Song) {
        return null;
    }
}
