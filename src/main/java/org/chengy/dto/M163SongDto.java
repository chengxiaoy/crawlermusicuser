package org.chengy.dto;

import lombok.Data;

import java.util.List;

@Data
public class M163SongDto {
    private String title;
    private List<String> arts;
    private String albumTitle;
    private String cover;
}
