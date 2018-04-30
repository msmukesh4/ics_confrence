package com.intel.webrtc.conference;

import com.intel.webrtc.base.AudioCodecParameters;
import com.intel.webrtc.base.CheckCondition;
import com.intel.webrtc.base.VideoCodecParameters;

import android.util.Log;

import com.intel.webrtc.base.MediaCodecs.AudioCodec;
import com.intel.webrtc.base.MediaCodecs.VideoCodec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SubscriptionCapabilities {
    public final SubscriptionCapabilities.AudioSubscriptionCapabilities audioSubscriptionCapabilities;
    public final SubscriptionCapabilities.VideoSubscriptionCapabilities videoSubscriptionCapabilities;

    SubscriptionCapabilities(JSONObject mediaInfo) throws JSONException {
        CheckCondition.DCHECK(mediaInfo);
        JSONObject audio = JsonUtils.getObj(mediaInfo, "audio");
        this.audioSubscriptionCapabilities = audio == null ? null : new SubscriptionCapabilities.AudioSubscriptionCapabilities(audio);
        JSONObject video = JsonUtils.getObj(mediaInfo, "video");
        this.videoSubscriptionCapabilities = video == null ? null : new SubscriptionCapabilities.VideoSubscriptionCapabilities(video);
    }

    public static class VideoSubscriptionCapabilities {
        public final List<VideoCodecParameters> videoCodecs = new ArrayList();
        public final List<HashMap<String, Integer>> resolutions = new ArrayList();
        public final List<Integer> frameRates = new ArrayList();
        public final List<Double> bitrateMultipliers = new ArrayList();
        public final List<Integer> keyFrameIntervals = new ArrayList();

        VideoSubscriptionCapabilities(JSONObject videoObj) throws JSONException {
        	Log.d("VideoSubscriptionCapabilities",videoObj.toString());
            JSONObject formatObj = JsonUtils.getObj(videoObj, "format");
            String codec = JsonUtils.getString(formatObj, "codec", "");
            this.videoCodecs.add(new VideoCodecParameters(VideoCodec.get(codec)));
            JSONObject parametersObj = JsonUtils.getObj(videoObj, "parameters");
            HashMap<String, Integer> resolutionItem = new HashMap();
            try {
            	JSONObject reslutionObj = JsonUtils.getObj(parametersObj, "resolution");
	            resolutionItem.put("width", JsonUtils.getInt(reslutionObj, "width", 0));
	            resolutionItem.put("height", JsonUtils.getInt(reslutionObj, "height", 0));
	            this.resolutions.add(resolutionItem);
            } catch(Exception e) {
            	Log.e("VideoSubscriptionCapabilities",e.getMessage());
            	 resolutionItem.put("width", 0);
 	             resolutionItem.put("height", 0);
            } finally {
            	this.resolutions.add(resolutionItem);
            }
            this.frameRates.add(JsonUtils.getInt(parametersObj, "framerate", 0));
            this.keyFrameIntervals.add(JsonUtils.getInt(parametersObj, "keyFrameInterval", 0));
            JSONObject optionalObj = JsonUtils.getObj(videoObj, "optional");
            if (optionalObj != null && optionalObj.has("format")) {
                JSONArray videoFormats = optionalObj.getJSONArray("format");

                for(int i = 0; i < videoFormats.length(); ++i) {
                    JSONObject codecObj = videoFormats.getJSONObject(i);
                    VideoCodec videoCodec = VideoCodec.get(JsonUtils.getString(codecObj, "codec", ""));
                    this.videoCodecs.add(new VideoCodecParameters(videoCodec));
                }
            }

            if (optionalObj != null && optionalObj.has("parameters")) {
                JSONObject optParamObj = JsonUtils.getObj(optionalObj, "parameters");
                JSONArray resolutionsArray = optParamObj.getJSONArray("resolution");

                for(int i = 0; i < resolutionsArray.length(); ++i) {
                    JSONObject resolution = resolutionsArray.getJSONObject(i);
                    HashMap<String, Integer> res = new HashMap();
                    res.put("width", JsonUtils.getInt(resolution, "width", 0));
                    res.put("height", JsonUtils.getInt(resolution, "height", 0));
                    this.resolutions.add(res);
                }

                JSONArray frameRatesArray = optParamObj.getJSONArray("framerate");

                for(int i = 0; i < frameRatesArray.length(); ++i) {
                    this.frameRates.add(frameRatesArray.getInt(i));
                }

                JSONArray bitratesArray = optParamObj.getJSONArray("bitrate");

                for(int i = 0; i < bitratesArray.length(); ++i) {
                    String bitrateString = bitratesArray.getString(i).substring(1);
                    this.bitrateMultipliers.add(Double.parseDouble(bitrateString));
                }

                JSONArray keyFrameIntervalsArray = optParamObj.getJSONArray("keyFrameInterval");

                for(int i = 0; i < keyFrameIntervalsArray.length(); ++i) {
                    this.keyFrameIntervals.add(keyFrameIntervalsArray.getInt(i));
                }
            }

        }
    }

    public static class AudioSubscriptionCapabilities {
        public final List<AudioCodecParameters> audioCodecs = new ArrayList();

        AudioSubscriptionCapabilities(JSONObject audioObj) throws JSONException {
            JSONObject format = JsonUtils.getObj(audioObj, "format");
            this.audioCodecs.add(new AudioCodecParameters(AudioCodec.get(JsonUtils.getString(format, "codec", "")), JsonUtils.getInt(format, "channelNum", 0), JsonUtils.getInt(format, "sampleRate", 0)));
            JSONObject audioOpt = JsonUtils.getObj(audioObj, "optional");
            if (audioOpt != null && audioOpt.has("format")) {
                JSONArray audioFormats = audioOpt.getJSONArray("format");

                for(int i = 0; i < audioFormats.length(); ++i) {
                    JSONObject codecObj = audioFormats.getJSONObject(i);
                    AudioCodec codec = AudioCodec.get(JsonUtils.getString(codecObj, "codec", ""));
                    int channelNum = JsonUtils.getInt(codecObj, "channelNum", 0);
                    int sampleRate = JsonUtils.getInt(codecObj, "sampleRate", 0);
                    this.audioCodecs.add(new AudioCodecParameters(codec, channelNum, sampleRate));
                }
            }

        }
    }
}
