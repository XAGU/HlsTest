package com.example.demo.controller;

import com.example.demo.services.MediaStreamServiceImpl;
import com.example.demo.utils.JavaCvFFmpegUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;


@RestController
@RequestMapping("/media")
public class MediaStreamController {

    @Autowired
    private MediaStreamServiceImpl mediaStreamService;

    @GetMapping("/hls/play/{filename}")
    public StreamingResponseBody hlsPlay(@PathVariable("filename") String filename) {
        return mediaStreamService.hlsPlay(filename);
    }

    @GetMapping("/hls/split")
    public void hlsSplit() {
        try {
            ClassPathResource resource = new ClassPathResource("static/1992000582.mp4");
            JavaCvFFmpegUtils.convertMediaToHls(resource.getInputStream(), "hls_playlist.m3u8");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
