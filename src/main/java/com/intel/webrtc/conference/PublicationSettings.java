//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intel.webrtc.conference;

import com.intel.webrtc.base.AudioCodecParameters;
import com.intel.webrtc.base.CheckCondition;
import com.intel.webrtc.base.VideoCodecParameters;
import com.intel.webrtc.base.MediaCodecs.AudioCodec;
import com.intel.webrtc.base.MediaCodecs.VideoCodec;
import org.json.JSONObject;

public final class PublicationSettings {
    public final PublicationSettings.AudioPublicationSettings audioPublicationSettings;
    public final PublicationSettings.VideoPublicationSettings videoPublicationSettings;

    PublicationSettings(JSONObject mediaInfo) {
        CheckCondition.DCHECK(mediaInfo);
        JSONObject audio = JsonUtils.getObj(mediaInfo, "audio");
        this.audioPublicationSettings = audio == null ? null : new PublicationSettings.AudioPublicationSettings(audio);
        JSONObject video = JsonUtils.getObj(mediaInfo, "video");
        this.videoPublicationSettings = video == null ? null : new PublicationSettings.VideoPublicationSettings(video);
    }

    public static class VideoPublicationSettings {
        public final VideoCodecParameters codec;
        public final int resolutionWidth;
        public final int resolutionHeight;
        public final int frameRate;
        public final int bitrate;
        public final int keyFrameInterval;

        VideoPublicationSettings(JSONObject videoObj) {
            JSONObject format = JsonUtils.getObj(videoObj, "format", true);
            VideoCodec videoCodec = VideoCodec.get(JsonUtils.getString(format, "codec", ""));
            this.codec = new VideoCodecParameters(videoCodec);
            JSONObject param = JsonUtils.getObj(videoObj, "parameters");
            if (param != null) {
                this.resolutionWidth = JsonUtils.getInt(JsonUtils.getObj(param, "resolution"), "width", 0);
                this.resolutionHeight = JsonUtils.getInt(JsonUtils.getObj(param, "resolution"), "height", 0);
                this.frameRate = JsonUtils.getInt(param, "framerate", 0);
                this.bitrate = JsonUtils.getInt(param, "bitrate", 0);
                this.keyFrameInterval = JsonUtils.getInt(param, "keyFrameInterval", 0);
            } else {
                this.resolutionWidth = 0;
                this.resolutionHeight = 0;
                this.frameRate = 0;
                this.bitrate = 0;
                this.keyFrameInterval = 0;
            }

        }
    }

    public static class AudioPublicationSettings {
        public final AudioCodecParameters codec;

        AudioPublicationSettings(JSONObject audioObj) {
            JSONObject format = JsonUtils.getObj(audioObj, "format", true);
            AudioCodec audioCodec = AudioCodec.get(JsonUtils.getString(format, "codec", ""));
            int sampleRate = JsonUtils.getInt(format, "sampleRate", 0);
            int channelNum = JsonUtils.getInt(format, "channelNum", 0);
            this.codec = new AudioCodecParameters(audioCodec, sampleRate, channelNum);
        }
    }
}
