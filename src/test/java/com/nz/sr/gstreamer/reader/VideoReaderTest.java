//package com.nz.sr.gstreamer.reader;
//
//import com.badlogic.gdx.Game;
//import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
//import com.github.fabiitch.gdxunit.utils.TestUtils;
//import com.nz.sr.gstreamer.reader.basic.BasicV2VideoReader;
//import com.nz.sr.gstreamer.reader.basic.BasicVideoReader;
//import com.nz.sr.gstreamer.reader.basic.CpuVideoReader;
//import com.nz.sr.gstreamer.reader.nv12.NV12VideoReader;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//
//import java.io.File;
//import java.util.function.Function;
//
//@Disabled
//public class VideoReaderTest {
//
//    File hdFolder = TestUtils.getFileFromRessource("video/1080p");
//    Function<File, IOldVideoReader> defaultFactory = BasicVideoReader::new;
//    Function<File, IOldVideoReader> v2Factory = BasicV2VideoReader::new;
//    Function<File, IOldVideoReader> nv12Factory = NV12VideoReader::new;
//    Function<File, IOldVideoReader> cpuReaderFactory = CpuVideoReader::new;
//
//    @Test
//    public void hdV1() {
//        grid(16, hdFolder, nv12Factory);
//    }
//
//    @Test
//    public void square() {
//        square(hdFolder, nv12Factory);
//    }
//
//    public static void grid(int playerCount, File folderVideos, Function<File, IOldVideoReader> videoReaderFactory) {
//        Game game = new Game() {
//
//            @Override
//            public void create() {
//                setScreen(new GridVideoTest(playerCount, videoReaderFactory, folderVideos));
//            }
//        };
//        new Lwjgl3Application(game);
//    }
//
//    public static void square(File folderVideos, Function<File, IOldVideoReader> videoReaderFactory) {
//        Game game = new Game() {
//
//            @Override
//            public void create() {
//                setScreen(new SquareVideoTest(folderVideos, videoReaderFactory));
//            }
//        };
//        new Lwjgl3Application(game);
//    }
//}
