//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intel.webrtc.conference;

import com.intel.webrtc.base.AudioEncodingParameters;
import com.intel.webrtc.base.CheckCondition;
import com.intel.webrtc.base.VideoEncodingParameters;
import java.util.ArrayList;
import java.util.List;

public final class PublishOptions {
    final List<AudioEncodingParameters> audioEncodingParameters;
    final List<VideoEncodingParameters> videoEncodingParameters;

    public static PublishOptions.Builder builder() {
        return new PublishOptions.Builder();
    }

    private PublishOptions(List<AudioEncodingParameters> audioParameters, List<VideoEncodingParameters> videoParameters) {
        this.audioEncodingParameters = audioParameters;
        this.videoEncodingParameters = videoParameters;
    }

    public static class Builder {
        List<AudioEncodingParameters> audioEncodingParameters = new ArrayList();
        List<VideoEncodingParameters> videoEncodingParameters = new ArrayList();

        Builder() {
        }

        public PublishOptions.Builder addVideoParameter(VideoEncodingParameters parameter) {
            CheckCondition.RCHECK(parameter);
            this.videoEncodingParameters.add(parameter);
            return this;
        }

        public PublishOptions.Builder addAudioParameter(AudioEncodingParameters parameter) {
            CheckCondition.RCHECK(parameter);
            this.audioEncodingParameters.add(parameter);
            return this;
        }

        public PublishOptions build() {
            return new PublishOptions(this.audioEncodingParameters, this.videoEncodingParameters);
        }
    }
}
