//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intel.webrtc.conference;

import com.intel.webrtc.base.Stream.StreamSourceInfo;
import com.intel.webrtc.base.Stream.StreamSourceInfo.AudioSourceInfo;
import com.intel.webrtc.base.Stream.StreamSourceInfo.VideoSourceInfo;
import java.util.HashMap;
import java.util.Iterator;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;

public class RemoteStream extends com.intel.webrtc.base.RemoteStream {
    public final SubscriptionCapabilities subscriptionCapability;
    public final PublicationSettings publicationSettings;

    RemoteStream(JSONObject streamInfo) throws JSONException {
        super(JsonUtils.getString(streamInfo, "id"), JsonUtils.getString(streamInfo.getJSONObject("info"), "owner", "mixer"));
        JSONObject mediaInfo = JsonUtils.getObj(streamInfo, "media", true);
        this.publicationSettings = new PublicationSettings(mediaInfo);
        this.subscriptionCapability = new SubscriptionCapabilities(mediaInfo);
        JSONObject video = JsonUtils.getObj(mediaInfo, "video");
        VideoSourceInfo videoSourceInfo = null;
        if (video != null) {
            videoSourceInfo = VideoSourceInfo.get(JsonUtils.getString(video, "source", "mixed"));
        }

        JSONObject audio = JsonUtils.getObj(mediaInfo, "audio");
        AudioSourceInfo audioSourceInfo = null;
        if (audio != null) {
            audioSourceInfo = AudioSourceInfo.get(JsonUtils.getString(audio, "source", "mixed"));
        }

        this.setStreamSourceInfo(new StreamSourceInfo(videoSourceInfo, audioSourceInfo));
        this.setAttributes(JsonUtils.getObj(JsonUtils.getObj(streamInfo, "info"), "attributes"));
    }

    void setMediaStream(MediaStream mediaStream) {
        this.mediaStream = mediaStream;
    }

    MediaStream getMediaStream() {
        return this.mediaStream;
    }

    private void setAttributes(JSONObject attributes) throws JSONException {
        if (attributes != null) {
            HashMap<String, String> attr = new HashMap();
            Iterator keyset = attributes.keys();

            while(keyset.hasNext()) {
                String key = (String)keyset.next();
                String value = attributes.getString(key);
                attr.put(key, value);
            }

            this.setAttributes(attr);
        }
    }

    void onEnded() {
        this.triggerEndedEvent();
    }
}
