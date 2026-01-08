package com.backend.farmon.dto.home;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PopularExpertPostRow {
    private Long postId;
    private String title;
    private String content;
    private String writer;
    private String profileImageUrl;
    private String firstImageStoredFileName;
}
