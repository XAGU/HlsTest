package com.example.demo.utils;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.ffmpeg.global.avutil;
import org.bytedeco.javacv.*;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import static org.bytedeco.ffmpeg.global.avutil.AV_LOG_ERROR;

/**
 * 媒体处理
 */
public class JavaCvFFmpegUtils {


    static {
        FFmpegLogCallback.set();
        //只打印错误日志
        avutil.av_log_set_level(AV_LOG_ERROR);
    }

    /**
     * 流媒体HLS切片
     *
     * @param inputStream 输入流
     * @param path        输出路径可以为本地路径，也可为网络地址
     */
    public static void convertMediaToHls(InputStream inputStream, String path) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream)) {
            grabber.start();
            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(path, grabber.getImageWidth(),
                    grabber.getImageHeight(), grabber.getAudioChannels())) {
                recorder.setFormat("hls");

                //设置单个ts切片的时间长度（以秒为单位）。默认值为5秒
                recorder.setOption("hls_time", "5");
                // HLS播放的列表长度，0标识不做限制
                recorder.setOption("hls_list_size", "0");
                /*hls的切片类型：
                 * 'mpegts'：以MPEG-2传输流格式输出ts切片文件，可以与所有HLS版本兼容。
                 * 'fmp4':以Fragmented MP4(简称：fmp4)格式输出切片文件，类似于MPEG-DASH，fmp4文件可用于HLS version 7和更高版本。
                 */
                recorder.setOption("hls_segment_type", "mpegts");
                double frameRate = getFrameRate(grabber);
                recorder.setFrameRate(frameRate);
                recorder.setGopSize((int) frameRate * 2);
                recorder.setVideoBitrate(grabber.getVideoBitrate());
                recorder.setAudioBitrate(grabber.getAudioBitrate());
                recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
                recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);

                //是否支持转复用
                if (supportReuse(grabber)) {
                    recorder.start(grabber.getFormatContext());
                    AVPacket pkt;
                    while ((pkt = grabber.grabPacket()) != null) {
                        recorder.setTimestamp(grabber.getTimestamp());
                        recorder.recordPacket(pkt);
                    }
                } else {
                    recorder.start();
                    Frame frame;
                    while ((frame = grabber.grabFrame()) != null) {
                        recorder.setTimestamp(grabber.getTimestamp());
                        recorder.record(frame);
                    }
                }
            } catch (FrameRecorder.Exception e) {
                throw new RuntimeException(e);
            }
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取帧率
     *
     * @param grabber 抓取器
     */
    private static double getFrameRate(FFmpegFrameGrabber grabber) {
        double framerate = 25.0;
        // 异常的framerate，强制使用25帧
        if (grabber.getFrameRate() > 0 && grabber.getFrameRate() < 100) {
            framerate = grabber.getFrameRate();
        }
        return framerate;
    }

    /**
     * 是否支持转复用
     *
     * @return boolean
     */
    private static boolean supportReuse(FFmpegFrameGrabber grabber) {
        return (avcodec.AV_CODEC_ID_H264 == grabber.getVideoCodec()) &&
                (avcodec.AV_CODEC_ID_AAC == grabber.getAudioCodec());
    }

    /**
     * 视频添加水印
     *
     * @param inputStream  输入文件
     * @param outputStream 输出流
     * @param watermark    水印文字
     */
    public static void addWaterMark(FileInputStream inputStream, OutputStream outputStream, String watermark) {
        try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputStream)) {
            grabber.start();

            try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputStream, grabber.getImageWidth(),
                    grabber.getImageHeight(), grabber.getAudioChannels())) {
                recorder.setFormat(grabber.getFormat());
                recorder.setFrameRate(grabber.getFrameRate());
                recorder.setGopSize((int) grabber.getFrameRate() * 2);
                recorder.setVideoCodec(grabber.getVideoCodec());
                recorder.setVideoBitrate(grabber.getVideoBitrate());
                recorder.setAudioCodec(grabber.getAudioCodec());
                recorder.setAudioBitrate(grabber.getAudioBitrate());
                recorder.start();

                String fontPath = getSystemFontPath();
                // 调整字体、大小、颜色、透明度
                String filterContent = "drawtext=text='" + watermark + "':fontfile='" + fontPath + "':fontsize=20:x=10:y=10:fontcolor=gray,";
                try (FFmpegFrameFilter frameFilter = new FFmpegFrameFilter(filterContent, grabber.getImageWidth(),
                        grabber.getImageHeight())) {
                    frameFilter.start();
                    Frame frame;
                    while ((frame = grabber.grabFrame()) != null) {
                        recorder.setTimestamp(grabber.getTimestamp());
                        if (frame.image != null) {
                            frameFilter.push(frame);
                            recorder.record(frameFilter.pull());
                        }
                        //else if 注释掉可以正常播放
/*                        else if (frame.samples != null) {
                            recorder.record(frame);
                        }*/
                    }
                } catch (FrameFilter.Exception e) {
                    throw new RuntimeException(e);
                }
            } catch (FrameRecorder.Exception e) {
                throw new RuntimeException(e);
            }
        } catch (FrameGrabber.Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 获取系统字体路径
     */
    private static String getSystemFontPath() {
        return "C\\:/Windows/Fonts/msyh.ttc";
    }
}
