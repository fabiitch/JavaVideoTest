package com.nz.recorder.backend.gst.builder;

import com.nz.recorder.backend.gst.tt.GstVideoChain;
import com.nz.recorder.backend.gst.config.settings.GstEncoderSettings;
import com.nz.recorder.backend.gst.source.GstAudioSource;
import com.nz.recorder.backend.gst.source.GstVideoSource;
import com.nz.recorder.backend.gst.source.video.GstScreenCapture;
import com.nz.share.gst.recorder.GstElementProbe;
import com.nz.share.gst.recorder.GstVideoPipelinePlanner;
import org.freedesktop.gstreamer.*;

public class GstChainBuilders {
    public static GstVideoChain buildVideoChain(GstVideoSource src, GstEncoderSettings enc) {
        Bin vsrc = src.build();

        int fps = src instanceof GstScreenCapture sc ? sc.getFps() : 60;
        GstEncoderPick encPick = GstEncoderFactory.pickH264(enc, fps);
        Element venc = encPick.getEncoder();
        Element vparse = ElementFactory.make("h264parse", "h264parse");
        Element vq = ElementFactory.make("queue", "vq_enc");

        GstElementProbe probe = GstElementProbe.defaultProbe();
        Element srcCapsFilter = null;
        Element convert = null;
        Element download = null;
        Element sysConvert = null;
        Element encCapsFilter = null;
        String debugPlan = "source=generic";

        if (src instanceof GstScreenCapture sc && sc.getApi() == GstScreenCapture.API.D3D11) {
            GstVideoPipelinePlanner.Plan plan = GstVideoPipelinePlanner.planD3d11Capture(
                encPick.isHardware() ? GstVideoPipelinePlanner.EncoderTarget.H264_HARDWARE : GstVideoPipelinePlanner.EncoderTarget.H264_SOFTWARE,
                sc.getWidth(),
                sc.getHeight(),
                sc.getFps(),
                probe
            );
            StringBuilder pipeline = new StringBuilder("pipeline=vsrc");
            pipeline.append(" ! caps(").append(plan.sourceCaps()).append(")");
            if (plan.useD3d11Convert()) pipeline.append(" ! d3d11convert");
            if (plan.useD3d11Download()) pipeline.append(" ! d3d11download");
            if (plan.useVideoConvert()) pipeline.append(" ! videoconvert");
            pipeline.append(" ! caps(").append(plan.encoderCaps()).append(")");
            pipeline.append(" ! ").append(encPick.isHardware() ? "nvh264enc" : "x264enc");
            debugPlan = "d3d11=" + plan.reason() + " " + pipeline;

            srcCapsFilter = ElementFactory.make("capsfilter", "vsrc_caps");
            if (srcCapsFilter != null) {
                srcCapsFilter.set("caps", Caps.fromString(plan.sourceCaps()));
            }
            if (plan.useD3d11Convert()) {
                convert = ElementFactory.make("d3d11convert", "vconv_d3d11");
            }
            if (plan.useD3d11Download()) {
                download = ElementFactory.make("d3d11download", "vdownload");
            }
            if (plan.useVideoConvert()) {
                sysConvert = ElementFactory.make("videoconvert", "vconv_sys");
            }
            encCapsFilter = ElementFactory.make("capsfilter", "venc_caps");
            if (encCapsFilter != null) {
                encCapsFilter.set("caps", Caps.fromString(plan.encoderCaps()));
            }
        } else if (probe.isElementAvailable("videoconvert")) {
            sysConvert = ElementFactory.make("videoconvert", "vconv_sys");
            encCapsFilter = ElementFactory.make("capsfilter", "venc_caps");
            String caps = encPick.isHardware()
                ? "video/x-raw,format=NV12"
                : "video/x-raw,format=I420";
            if (encCapsFilter != null) {
                encCapsFilter.set("caps", Caps.fromString(caps));
            }
            debugPlan = "system pipeline=vsrc ! videoconvert ! caps(" + caps + ") ! "
                + (encPick.isHardware() ? "nvh264enc" : "x264enc");
        }

        Bin bin = new Bin("video_chain");
        bin.addMany(vsrc, venc, vparse, vq);
        if (srcCapsFilter != null) bin.add(srcCapsFilter);
        if (convert != null) bin.add(convert);
        if (download != null) bin.add(download);
        if (sysConvert != null) bin.add(sysConvert);
        if (encCapsFilter != null) bin.add(encCapsFilter);

        Element upstream = vsrc;
        if (srcCapsFilter != null) {
            if (!upstream.link(srcCapsFilter)) {
                throw new IllegalStateException("Failed to link source caps");
            }
            upstream = srcCapsFilter;
        }
        if (convert != null) {
            if (!upstream.link(convert)) {
                throw new IllegalStateException("Failed to link d3d11convert");
            }
            upstream = convert;
        }
        if (download != null) {
            if (!upstream.link(download)) {
                throw new IllegalStateException("Failed to link d3d11download");
            }
            upstream = download;
        }
        if (sysConvert != null) {
            if (!upstream.link(sysConvert)) {
                throw new IllegalStateException("Failed to link videoconvert");
            }
            upstream = sysConvert;
        }
        if (encCapsFilter != null) {
            if (!upstream.link(encCapsFilter)) {
                throw new IllegalStateException("Failed to link encoder caps");
            }
            upstream = encCapsFilter;
        }
        if (!upstream.link(venc)) {
            throw new IllegalStateException("Failed to link encoder");
        }
        if (!Element.linkMany(venc, vparse, vq)) {
            throw new IllegalStateException("Failed to link encoder → parser → queue");
        }

        bin.addPad(new GhostPad("src", vq.getStaticPad("src")));
        return new GstVideoChain(bin, debugPlan);
    }

    public static Element buildAudioChain(GstAudioSource src, GstEncoderSettings enc) {
        Bin asrc = src.build();
        Element aenc = ElementFactory.make("avenc_aac", "aac"); // windows-friendly; alt: fdkaac or voaacenc
        if (aenc == null) aenc = ElementFactory.make("faac", "aac"); // fallback
        try {
            aenc.set("bitrate", enc.audioKbps * 1000);
        } catch (Exception ignored) {
        }

        Element aparse = ElementFactory.make("aacparse", "aacparse");
        Element aq = ElementFactory.make("queue", "aq_enc");

        Bin bin = new Bin("audio_chain");
        bin.addMany(asrc, aenc, aparse, aq);
        asrc.link(aenc);
        Element.linkMany(aenc, aparse, aq);

        bin.addPad(new GhostPad("src", aq.getStaticPad("src")));
        return bin;
    }
}
