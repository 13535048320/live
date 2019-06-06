package com.soft;

import javafx.application.Application;
import org.bytedeco.javacpp.*;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.imageio.ImageIO;
import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@RunWith(SpringRunner.class)
@SpringBootTest
public class LiveApplicationTests {

    @Test
    public void contextLoads() {
    }

    /**
     *  本地音频(话筒设备)和视频(摄像头)抓取、混合并推送(录制)到服务器(本地)
     * @throws FrameGrabber.Exception
     */
    @Test
    public void recordWebcamAndMicrophone() throws FrameGrabber.Exception {
        int WEBCAM_DEVICE_INDEX = 0;   // 视频设备，本机默认是0
        int AUDIO_DEVICE_INDEX = 4;   // 音频设备，本机默认是4
        String outputFile = "rtmp://192.168.70.128:1935/live/test?username=admin&password=admin";  // 输出文件/地址(可以是本地文件，也可以是流媒体服务器地址)
        int captureWidth = 200;     // 摄像头宽
        int captureHeight = 150;   // 摄像头高
        int FRAME_RATE = 25;     // 视频帧率:最低 25(即每秒25张图片,低于25就会出现闪屏)
        long startTime = 0;
        long videoTS = 0;         // 时间戳
        /**
         * FrameGrabber 类包含：OpenCVFrameGrabber
         * (opencv_videoio),C1394FrameGrabber, FlyCaptureFrameGrabber,
         * OpenKinectFrameGrabber,PS3EyeFrameGrabber,VideoInputFrameGrabber, 和
         * FFmpegFrameGrabber.
         */
        OpenCVFrameGrabber grabber = new OpenCVFrameGrabber(WEBCAM_DEVICE_INDEX);  // 视频设备
        grabber.setImageWidth(captureWidth);
        grabber.setImageHeight(captureHeight);

        System.out.println("开始抓取摄像头...");
        int isTrue = 0;    // 摄像头开启状态
        try {
            grabber.start();
            isTrue += 1;
        } catch (org.bytedeco.javacv.FrameGrabber.Exception e2) {
            if (grabber != null) {
                try {
                    grabber.restart();
                    isTrue += 1;
                } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                    isTrue -= 1;
                    try {
                        grabber.stop();
                    } catch (org.bytedeco.javacv.FrameGrabber.Exception e1) {
                        isTrue -= 1;
                    }
                }
            }
        }
        if (isTrue < 0) {
            System.out.println("摄像头首次开启失败，尝试重启也失败！");
            return;
        } else if (isTrue < 1) {
            System.out.println("摄像头开启失败！");
            return;
        } else if (isTrue == 1) {
            System.out.println("摄像头开启成功！");
        } else if (isTrue == 1) {
            System.out.println("摄像头首次开启失败，重新启动成功！");
        }

        /**
         * FFmpegFrameRecorder(String filename, int imageWidth, int imageHeight,
         * int audioChannels) fileName可以是本地文件（会自动创建），也可以是RTMP路径（发布到流媒体服务器）
         * imageWidth = width （为捕获器设置宽） imageHeight = height （为捕获器设置高）
         * audioChannels = 2（立体声）；1（单声道）；0（无音频）
         */
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, captureWidth, captureHeight, 2);
        recorder.setInterleaved(true);

        /**
         * 该参数用于降低延迟 参考FFMPEG官方文档：https://trac.ffmpeg.org/wiki/StreamingGuide
         * 官方原文参考：ffmpeg -f dshow -i video="Virtual-Camera" -vcodec libx264
         * -tune zerolatency -b 900k -f mpegts udp://10.1.0.102:1234
         */

        recorder.setVideoOption("tune", "zerolatency");
        /**
         * 权衡quality(视频质量)和encode speed(编码速度) values(值)：
         * ultrafast(终极快),superfast(超级快), veryfast(非常快), faster(很快), fast(快),
         * medium(中等), slow(慢), slower(很慢), veryslow(非常慢)
         * ultrafast(终极快)提供最少的压缩（低编码器CPU）和最大的视频流大小；而veryslow(非常慢)提供最佳的压缩（高编码器CPU）的同时降低视频流的大小
         * 参考：https://trac.ffmpeg.org/wiki/Encode/H.264 官方原文参考：-preset ultrafast
         * as the name implies provides for the fastest possible encoding. If
         * some tradeoff between quality and encode speed, go for the speed.
         * This might be needed if you are going to be transcoding multiple
         * streams on one machine.
         */
        recorder.setVideoOption("preset", "ultrafast");
        /**
         * 参考转流命令: ffmpeg
         * -i'udp://localhost:5000?fifo_size=1000000&ovoutun_nonfatal=1' -crf 30
         * -preset ultrafast -acodec aac -strict experimental -ar 44100 -ac
         * 2-b:a 96k -vcodec libx264 -r 25 -b:v 500k -f flv 'rtmp://<wowza
         * serverIP>/live/cam0' -crf 30
         * -设置内容速率因子,这是一个x264的动态比特率参数，它能够在复杂场景下(使用不同比特率，即可变比特率)保持视频质量；
         * 可以设置更低的质量(quality)和比特率(bit rate),参考Encode/H.264 -preset ultrafast
         * -参考上面preset参数，与视频压缩率(视频大小)和速度有关,需要根据情况平衡两大点：压缩率(视频大小)，编/解码速度 -acodec
         * aac -设置音频编/解码器 (内部AAC编码) -strict experimental
         * -允许使用一些实验的编解码器(比如上面的内部AAC属于实验编解码器) -ar 44100 设置音频采样率(audio sample
         * rate) -ac 2 指定双通道音频(即立体声) -b:a 96k 设置音频比特率(bit rate) -vcodec libx264
         * 设置视频编解码器(codec) -r 25 -设置帧率(frame rate) -b:v 500k -设置视频比特率(bit
         * rate),比特率越高视频越清晰,视频体积也会变大,需要根据实际选择合理范围 -f flv
         * -提供输出流封装格式(rtmp协议只支持flv封装格式) 'rtmp://<FMS server
         * IP>/live/cam0'-流媒体服务器地址
         */
        recorder.setVideoOption("crf", "25");
        // 2000 kb/s, 720P视频的合理比特率范围
        recorder.setVideoBitrate(2000000);
        // h264编/解码器
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
        // 封装格式flv
        recorder.setFormat("flv");
        // 视频帧率(保证视频质量的情况下最低25，低于25会出现闪屏)
        recorder.setFrameRate(FRAME_RATE);
        // 关键帧间隔，一般与帧率相同或者是视频帧率的两倍
        recorder.setGopSize(FRAME_RATE * 2);
        // 不可变(固定)音频比特率
        recorder.setAudioOption("crf", "0");
        // 最高质量
        recorder.setAudioQuality(0);
        // 音频比特率
        recorder.setAudioBitrate(192000);
        // 音频采样率
        recorder.setSampleRate(44100);
        // 双通道(立体声)
        recorder.setAudioChannels(2);
        // 音频编/解码器
        recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
        System.out.println("开始录制...");

        try {
            recorder.start();
        } catch (org.bytedeco.javacv.FrameRecorder.Exception e2) {
            if (recorder != null) {
                System.out.println("关闭失败，尝试重启");
                try {
                    recorder.stop();
                    recorder.start();
                } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                    try {
                        System.out.println("开启失败，关闭录制");
                        recorder.stop();
                        return;
                    } catch (org.bytedeco.javacv.FrameRecorder.Exception e1) {
                        return;
                    }
                }
            }

        }
        // 音频捕获
        new Thread(new Runnable() {
            public void run() {
                /**
                 * 设置音频编码器 最好是系统支持的格式，否则getLine() 会发生错误
                 * 采样率:44.1k;采样率位数:16位;立体声(stereo);是否签名;true:
                 * big-endian字节顺序,false:little-endian字节顺序(详见:ByteOrder类)
                 */
                AudioFormat audioFormat = new AudioFormat(44100.0F, 16, 2, true, false);

                // 通过AudioSystem获取本地音频混合器信息
                Mixer.Info[] minfoSet = AudioSystem.getMixerInfo();
                // 通过AudioSystem获取本地音频混合器
                Mixer mixer = AudioSystem.getMixer(minfoSet[AUDIO_DEVICE_INDEX]);
                // 通过设置好的音频编解码器获取数据线信息
                DataLine.Info dataLineInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
                try {
                    // 打开并开始捕获音频
                    // 通过line可以获得更多控制权
                    // 获取设备：TargetDataLine line
                    // =(TargetDataLine)mixer.getLine(dataLineInfo);
                    TargetDataLine line = (TargetDataLine) AudioSystem.getLine(dataLineInfo);
                    line.open(audioFormat);
                    line.start();
                    // 获得当前音频采样率
                    int sampleRate = (int) audioFormat.getSampleRate();
                    // 获取当前音频通道数量
                    int numChannels = audioFormat.getChannels();
                    // 初始化音频缓冲区(size是音频采样率*通道数)
                    int audioBufferSize = sampleRate * numChannels;
                    byte[] audioBytes = new byte[audioBufferSize];

                    ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
                    exec.scheduleAtFixedRate(new Runnable() {
                        public void run() {
                            try {
                                // 非阻塞方式读取
                                int nBytesRead = line.read(audioBytes, 0, line.available());
                                // 因为我们设置的是16位音频格式,所以需要将byte[]转成short[]
                                int nSamplesRead = nBytesRead / 2;
                                short[] samples = new short[nSamplesRead];
                                /**
                                 * ByteBuffer.wrap(audioBytes)-将byte[]数组包装到缓冲区
                                 * ByteBuffer.order(ByteOrder)-按little-endian修改字节顺序，解码器定义的
                                 * ByteBuffer.asShortBuffer()-创建一个新的short[]缓冲区
                                 * ShortBuffer.get(samples)-将缓冲区里short数据传输到short[]
                                 */
                                ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
                                // 将short[]包装到ShortBuffer
                                ShortBuffer sBuff = ShortBuffer.wrap(samples, 0, nSamplesRead);
                                // 按通道录制shortBuffer
                                recorder.recordSamples(sampleRate, numChannels, sBuff);
                            } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }, 0, (long) 1000 / FRAME_RATE, TimeUnit.MILLISECONDS);
                } catch (LineUnavailableException e1) {
                    e1.printStackTrace();
                }
            }
        }).start();

        // javaCV提供了优化非常好的硬件加速组件来帮助显示我们抓取的摄像头视频
        CanvasFrame cFrame = new CanvasFrame("Capture Preview", CanvasFrame.getDefaultGamma() / grabber.getGamma());
        Frame capturedFrame = null;
        // 执行抓取（capture）过程
        while ((capturedFrame = grabber.grab()) != null) {
            if (cFrame.isVisible()) {
                //本机预览要发送的帧
                cFrame.showImage(capturedFrame);
            }
            //定义我们的开始时间，当开始时需要先初始化时间戳
            if (startTime == 0)
                startTime = System.currentTimeMillis();

            // 创建一个 timestamp用来写入帧中
            videoTS = 1000 * (System.currentTimeMillis() - startTime);
            //检查偏移量
            if (videoTS > recorder.getTimestamp()) {
                System.out.println("Lip-flap correction: " + videoTS + " : " + recorder.getTimestamp() + " -> "
                        + (videoTS - recorder.getTimestamp()));
                //告诉录制器写入这个timestamp
                recorder.setTimestamp(videoTS);
            }
            // 发送帧
            try {
                recorder.record(capturedFrame);
            } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
                System.out.println("录制帧发生异常，什么都不做");
            }
        }

        cFrame.dispose();
        try {
            if (recorder != null) {
                recorder.stop();
            }
        } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
            System.out.println("关闭录制器失败");
            try {
                if (recorder != null) {
                    grabber.stop();
                }
            } catch (org.bytedeco.javacv.FrameGrabber.Exception e1) {
                System.out.println("关闭摄像头失败");
                return;
            }
        }
        try {
            if (recorder != null) {
                grabber.stop();
            }
        } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
            System.out.println("关闭摄像头失败");
        }
    }


    /**
     * 取视频帧并添加水印后推流到指定地址
     *
     * @throws FrameGrabber.Exception
     * @throws FrameRecorder.Exception
     * @throws InterruptedException
     */
    @Test
    public void recordPushMp4() throws FrameGrabber.Exception, FrameRecorder.Exception, InterruptedException {
        String inputFile = "E:\\56.mp4";
        String outputFile = "rtmp://192.168.70.128:1935/live/test?username=admin&password=admin";
        int frameRate = 25;    // 帧率
        int gopSize = 25;      // 关键帧的周期，一般与帧率相同或者是视频帧率的两倍
        Loader.load(opencv_objdetect.class); //加载分类器
        long startTime = 0;    //
        FrameGrabber grabber = FFmpegFrameGrabber.createDefault(inputFile);
        try {
            grabber.start();
        } catch (Exception e) {
            e.printStackTrace();
            try {
                grabber.restart();
            } catch (Exception e1) {
                throw e;
            }
        }

        OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage(); // 用于将Frame转换为Mat和IplImage，Mat和IplImage转为Frame
        Frame grabframe = grabber.grab();           // 抓取帧
//        opencv_core.IplImage grabbedImage = null;
        opencv_core.Mat mat = null;
        if (grabframe != null) {
            System.out.println("取到第一帧");
//            grabbedImage = converter.convert(grabframe);
            mat = converter.convertToMat(grabframe);            // 将帧转为Mat，Mat可用于添加水印
        } else {
            System.out.println("没有取到第一帧");
        }

        // 视频帧加文字和图片水印
        opencv_core.Mat logo = opencv_imgcodecs.imread("E://logo1.jpg");                // 取水印图片
        opencv_core.Mat mask = opencv_imgcodecs.imread("E://logo1.jpg", 0);           // 取水印图片掩码
        opencv_imgproc.resize(logo, logo, new opencv_core.Size(150, 100));// 调整图片大小
        opencv_imgproc.resize(mask, mask, new opencv_core.Size(150, 100));

        // 文字水印
        // opencv_imgproc.putText（图片，水印文字，文字位置，字体，字体大小，字体颜色，字体粗度，平滑字体，是否翻转文字）
        opencv_imgproc.putText(mat, "test", new opencv_core.Point(10, 50), opencv_imgproc.CV_FONT_VECTOR0, 1.2, new opencv_core.Scalar(0, 255, 255, 0));

        // 图片水印
        opencv_core.Rect rect = new opencv_core.Rect(mat.cols() - 150, 0, 100, 100);   // 设置水印的位置和大小
        opencv_core.Mat logoMat = mat.apply(rect);     // 获取Mat添加水印的位置和大小
        logo.copyTo(logoMat, mask);            // 添加水印，并用掩码覆盖水印背景

//        如果想要保存图片,可以使用 opencv_imgcodecs.cvSaveImage("hello.jpg", grabbedImage);来保存图片
//        opencv_imgcodecs.cvSaveImage("E://hello.jpg", grabbedImage);
        opencv_imgcodecs.imwrite("E://hello.jpg", mat);          // 保存添加水印后的Mat为图片


        FrameRecorder recorder;
        try {
            recorder = FrameRecorder.createDefault(outputFile, 1280, 720);
        } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
            throw e;
        }
        recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // avcodec.AV_CODEC_ID_H264 视频编码标准
        recorder.setFormat("flv");              // 封装格式，如果是推送到rtmp就必须是flv封装格式
        recorder.setFrameRate(frameRate);       // 设置帧率
        recorder.setGopSize(gopSize);          // 设置关键帧的周期
        System.out.println("准备开始推流...");
        try {
            recorder.start();
        } catch (org.bytedeco.javacv.FrameRecorder.Exception e) {
            try {
                System.out.println("录制器启动失败，正在重新启动...");
                if (recorder != null) {
                    System.out.println("尝试关闭录制器");
                    recorder.stop();
                    System.out.println("尝试重新开启录制器");
                    recorder.start();
                }

            } catch (org.bytedeco.javacv.FrameRecorder.Exception e1) {
                throw e;
            }
        }
        System.out.println("开始推流");

        CanvasFrame frame = new CanvasFrame("camera", CanvasFrame.getDefaultGamma() / grabber.getGamma()); // 窗口显示
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setAlwaysOnTop(true);

        while (frame.isVisible() && (grabframe = grabber.grab()) != null) {  // 循环取帧
            System.out.println("推流...");
            frame.showImage(grabframe);                        // 将帧显示在窗口
//            grabbedImage = converter.convert(grabframe);
//            Frame rotatedFrame = converter.convert(grabbedImage);
            mat = converter.convertToMat(grabframe);           // 将帧转为Mat，Mat可用于添加水印
            Frame rotatedFrame = converter.convert(mat);       // 再讲Mat转为帧

            if (startTime == 0) {
                startTime = System.currentTimeMillis();
            }
            recorder.setTimestamp(1000 * (System.currentTimeMillis() - startTime));//时间戳
            if (rotatedFrame != null) {
                recorder.record(rotatedFrame);
            }
            Thread.sleep(40);
        }
        frame.dispose();
        recorder.stop();
        recorder.release();
        grabber.stop();
        System.exit(2);
    }

    /**
     *  从nginx服务器拉流并输出成test.flv
     */
    @Test
    public void recordPull() {
        String inputFile = "rtmp://192.168.70.128:1935/live/test";
        String outputFile = "E:\\test.flv";
        int audioChannel = 1;     // 声道，2 立体声，1 单声道，0 无音频
        boolean status = true;
        boolean isStart = true;

        // 获取视频源
        FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile);
        // 流媒体输出地址，分辨率（长，高），是否录制音频（0:不录制/1:录制）
        FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, 1280, 720, audioChannel);
        // 开始取视频源
        System.out.println(inputFile + "的推流已經開始推送，推送目標是：" + outputFile);

        System.out.println("开始启动无敌爬流线程");
        try {
            grabber.start();
            recorder.setInterleaved(true);

            // 该参数用于降低延迟
            // recorder.setVideoOption("tune", "zerolatency");
            // ultrafast(终极快)提供最少的压缩（低编码器CPU）和最大的视频流大小；
            // 参考以下命令: ffmpeg -i '' -crf 30 -preset ultrafast
            //ultrafast、superfast、veryfast、faster、fast、medium、slow、slower、veryslow、placebo。从快到慢，参数越来越EP。默认是medium。
            recorder.setVideoOption("preset", "ultrafast");

            // 提供输出流封装格式(rtmp协议只支持flv封装格式)
            recorder.setFormat("flv");

            // video的编码格式
            recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
            // 在优先保证画面质量（也不太在乎转码时间）的情况下，使用-crf参数来控制转码是比较适宜的。
            // 这个参数的取值范围为0~51，其中0为无损模式，数值越大，画质越差，生成的文件却越小。
            // 从主观上讲，18~28是一个合理的范围。
            recorder.setVideoOption("crf", "30");
            recorder.setAudioOption("crf", "0");

            recorder.setVideoQuality(0);

            // 视频帧率(保证视频质量的情况下最低25，低于25会出现闪屏
            recorder.setFrameRate(25);

            // 关键帧间隔，一般与帧率相同或者是视频帧率的两倍
            recorder.setGopSize(25 * 2);

            // 最高质量
            recorder.setAudioQuality(0);


            // 不可变(固定)音频比特率
            // 2000 kb/s, 720P视频的合理比特率范围
            // recorder.setVideoBitrate(2000000);
            recorder.setAudioBitrate(192000);

            // 音频采样率
            recorder.setSampleRate(44100);

            // 双通道(立体声)
            recorder.setAudioChannels(2);

            // 音频编/解码器
            recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
            recorder.start();

            Frame frame = null;
            long startTime = 0, videoTS = 0;
            // frame会自动回收?
            while (status && (frame = grabber.grabFrame()) != null) {
                if (startTime == 0) {
                    startTime = System.currentTimeMillis();
                }

                videoTS = 1000 * (System.currentTimeMillis() - startTime);
                recorder.setTimestamp(videoTS);
                recorder.record(frame);
            }

            System.out.println("推流已结束");
            recorder.stop();
            grabber.stop();
        } catch (org.bytedeco.javacv.FrameGrabber.Exception | org.bytedeco.javacv.FrameRecorder.Exception e) {
            System.out.println("触发异常，回收grabber");
            e.printStackTrace();
            if (grabber != null) {
                try {
                    grabber.stop();
                } catch (org.bytedeco.javacv.FrameGrabber.Exception e1) {
                    e1.printStackTrace();
                }
            }
        } finally {
            if (grabber != null) {
                try {
                    System.out.println("触发finally模块，回收grabber，frame自動回收无需处理");
                    grabber.stop();
                } catch (org.bytedeco.javacv.FrameGrabber.Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
