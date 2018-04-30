//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intel.webrtc.conference;

import com.intel.webrtc.base.AudioCodecParameters;
import com.intel.webrtc.base.CheckCondition;
import com.intel.webrtc.base.VideoCodecParameters;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public final class SubscribeOptions {
    public final SubscribeOptions.AudioSubscriptionConstraints audioOption;
    public final SubscribeOptions.VideoSubscriptionConstraints videoOption;

    public static SubscribeOptions.Builder builder(boolean subAudio, boolean subVideo) {
        return new SubscribeOptions.Builder(subAudio, subVideo);
    }

    private SubscribeOptions(SubscribeOptions.AudioSubscriptionConstraints audioOption, SubscribeOptions.VideoSubscriptionConstraints videoOption) {
        this.audioOption = audioOption;
        this.videoOption = videoOption;
    }

    public static class Builder {
        private boolean subAudio;
        private boolean subVideo;
        private SubscribeOptions.AudioSubscriptionConstraints audioOption;
        private SubscribeOptions.VideoSubscriptionConstraints videoOption;

        Builder(boolean subAudio, boolean subVideo) {
            this.subAudio = subAudio;
            this.subVideo = subVideo;
        }

        public SubscribeOptions.Builder setAudioOption(SubscribeOptions.AudioSubscriptionConstraints audioOption) {
            this.audioOption = audioOption;
            return this;
        }

        public SubscribeOptions.Builder setVideoOption(SubscribeOptions.VideoSubscriptionConstraints videoOption) {
            this.videoOption = videoOption;
            return this;
        }

        public SubscribeOptions build() {
            CheckCondition.RCHECK(!this.subAudio || this.audioOption != null);
            CheckCondition.RCHECK(!this.subVideo || this.videoOption != null);
            return new SubscribeOptions(this.subAudio ? this.audioOption : null, this.subVideo ? this.videoOption : null);
        }
    }

    public static class VideoSubscriptionConstraints {
        final List<VideoCodecParameters> codecs;
        private int resolutionWidth;
        private int resolutionHeight;
        private int frameRate;
        private int keyFrameInterval;
        private double bitrateMultiplier;

        public static SubscribeOptions.VideoSubscriptionConstraints.Builder builder() {
            return new SubscribeOptions.VideoSubscriptionConstraints.Builder();
        }

        private VideoSubscriptionConstraints(List<VideoCodecParameters> codecs) {
            this.resolutionWidth = 0;
            this.resolutionHeight = 0;
            this.frameRate = 0;
            this.keyFrameInterval = 0;
            this.bitrateMultiplier = 0.0D;
            this.codecs = codecs;
        }

        JSONObject generateOptionsMsg() throws JSONException {
            JSONObject videoParams = new JSONObject();
            if (this.resolutionWidth != 0 && this.resolutionHeight != 0) {
                JSONObject reso = new JSONObject();
                reso.put("width", this.resolutionWidth);
                reso.put("height", this.resolutionHeight);
                videoParams.put("resolution", reso);
            }

            if (this.frameRate != 0) {
                videoParams.put("framerate", this.frameRate);
            }

            if (this.bitrateMultiplier != 0.0D) {
                videoParams.put("bitrate", "x" + this.bitrateMultiplier);
            }

            if (this.keyFrameInterval != 0) {
                videoParams.put("keyFrameInterval", this.keyFrameInterval);
            }

            return videoParams;
        }

        public static class Builder {
            private List<VideoCodecParameters> codecs = new ArrayList();
            private int resolutionWidth = 0;
            private int resolutionHeight = 0;
            private int frameRate = 0;
            private int keyFrameInterval = 0;
            private double bitrateMultiplier = 0.0D;

            Builder() {
            }

            public SubscribeOptions.VideoSubscriptionConstraints.Builder setResolution(int width, int height) {
                this.resolutionWidth = width;
                this.resolutionHeight = height;
                return this;
            }

            public SubscribeOptions.VideoSubscriptionConstraints.Builder setFrameRate(int frameRate) {
                this.frameRate = frameRate;
                return this;
            }

            public SubscribeOptions.VideoSubscriptionConstraints.Builder setKeyFrameInterval(int keyFrameInterval) {
                this.keyFrameInterval = keyFrameInterval;
                return this;
            }

            public SubscribeOptions.VideoSubscriptionConstraints.Builder setBitrateMultiplier(double multiplier) {
                this.bitrateMultiplier = multiplier;
                return this;
            }

            public SubscribeOptions.VideoSubscriptionConstraints.Builder addCodec(VideoCodecParameters codec) {
                CheckCondition.RCHECK(codec);
                this.codecs.add(codec);
                return this;
            }

            public SubscribeOptions.VideoSubscriptionConstraints build() {
                CheckCondition.RCHECK(this.resolutionWidth != 0 && this.resolutionHeight != 0);
                SubscribeOptions.VideoSubscriptionConstraints constraints = new SubscribeOptions.VideoSubscriptionConstraints(this.codecs);
                constraints.resolutionWidth = this.resolutionWidth;
                constraints.resolutionHeight = this.resolutionHeight;
                constraints.frameRate = this.frameRate;
                constraints.keyFrameInterval = this.keyFrameInterval;
                constraints.bitrateMultiplier = this.bitrateMultiplier;
                return constraints;
            }
        }
    }

    public static class AudioSubscriptionConstraints {
        final List<AudioCodecParameters> codecs;

        public static SubscribeOptions.AudioSubscriptionConstraints.Builder builder() {
            return new SubscribeOptions.AudioSubscriptionConstraints.Builder();
        }

        private AudioSubscriptionConstraints(List<AudioCodecParameters> codecs) {
            this.codecs = codecs;
        }

        public static class Builder {
            private List<AudioCodecParameters> codecs = new ArrayList();

            Builder() {
            }

            public SubscribeOptions.AudioSubscriptionConstraints.Builder addCodec(AudioCodecParameters codec) {
                CheckCondition.RCHECK(codec);
                this.codecs.add(codec);
                return this;
            }

            public SubscribeOptions.AudioSubscriptionConstraints build() {
                return new SubscribeOptions.AudioSubscriptionConstraints(this.codecs);
            }
        }
    }
}
