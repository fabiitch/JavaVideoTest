//package com.nz.sr.gstreamer.recorder;
//
//import org.freedesktop.gstreamer.Gst;
//import org.junit.jupiter.api.Disabled;
//import org.junit.jupiter.api.Test;
//
//import java.io.File;
//
//import static com.nz.recorder.backend.gst.recorder.ModularGstRecorder.*;
//
//@Disabled
//public class GStreamerRecorderTest {
//
//
//    @Test
//    public void test2() throws InterruptedException {
//
//        File outputFile = new File("C:/temp/output2.mp4");
//
//        // Crée l'instance du Recorder avec des sources vidéo/audio et autres configurations
//        Recorder recorder = newBuilder()
//            .video(new ScreenCapture(ScreenCapture.API.D3D11, 0, 0, 1920, 1080, 60)) // Capture d'écran 1920x1080 à 60 fps
//            .audio(new WasapiMic(true, 2, 48000)) // Audio loopback, 2 canaux, 48000 Hz
//            .encoder(new EncoderSettings(true, 8000, 160, 2)) // Paramètres d'encodage H.264 (préférence NVENC)
//            .mux(MuxSettings.fromExtension(outputFile.getName())) // Mux en MP4
//            .output(outputFile) // Fichier de sortie
//            .build();
//
//        // Démarre l'enregistrement
//        recorder.start();
//        System.out.println("Recording started!");
//
//        // Attends un certain temps ou un signal de fin (ici on attend 10 secondes par exemple)
//        try {
//            Thread.sleep(10000); // Enregistrer pendant 10 secondes
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//        // Arrête l'enregistrement après 10 secondes
//        recorder.stop();
//        System.out.println("Recording stopped!");
//
//        Gst.deinit();
//        //https://www.youtube.com/watch?v=lLqfdTUjnuU
//    }
//}
