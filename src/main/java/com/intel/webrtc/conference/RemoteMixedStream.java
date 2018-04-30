//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intel.webrtc.conference;

import com.intel.webrtc.base.CheckCondition;
import com.intel.webrtc.base.RemoteStream.StreamObserver;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class RemoteMixedStream extends RemoteStream {
    public final String view;
    private String activeAudioInput;
    private List<RemoteMixedStream.Region> regions;

    RemoteMixedStream(JSONObject streamInfo) throws JSONException {
        super(streamInfo);
        JSONObject info = JsonUtils.getObj(streamInfo, "info", true);
        this.view = JsonUtils.getString(info, "label", "");
        this.activeAudioInput = JsonUtils.getString(info, "activeInput", "");
        this.regions = new ArrayList();
        this.updateRegions(info.getJSONArray("layout"));
    }

    public List<RemoteMixedStream.Region> regions() {
        return Collections.unmodifiableList(this.regions);
    }

    public String activeAudioInput() {
        return this.activeAudioInput;
    }

    void updateRegions(JSONArray regionsInfo) {
        this.regions.clear();

        try {
            for(int i = 0; i < regionsInfo.length(); ++i) {
                JSONObject region = regionsInfo.getJSONObject(i);
                this.regions.add(new RemoteMixedStream.Region(region));
            }
        } catch (JSONException var4) {
            CheckCondition.DCHECK(var4);
        }

        this.triggerLayoutChange();
    }

    private void triggerLayoutChange() {
        if (this.observers != null) {
            Iterator var1 = this.observers.iterator();

            while(var1.hasNext()) {
                StreamObserver observer = (StreamObserver)var1.next();
                if (observer instanceof RemoteMixedStream.RemoteMixedStreamObserver) {
                    ((RemoteMixedStream.RemoteMixedStreamObserver)observer).onLayoutChange(this.regions);
                }
            }
        }

    }

    void updateActiveInput(String activeInput) {
        this.activeAudioInput = activeInput;
        this.triggerActiveInputChange(activeInput);
    }

    private void triggerActiveInputChange(String activeInput) {
        if (this.observers != null) {
            Iterator var2 = this.observers.iterator();

            while(var2.hasNext()) {
                StreamObserver observer = (StreamObserver)var2.next();
                if (observer instanceof RemoteMixedStream.RemoteMixedStreamObserver) {
                    ((RemoteMixedStream.RemoteMixedStreamObserver)observer).onActiveAudioInputChange(activeInput);
                }
            }
        }

    }

    public interface RemoteMixedStreamObserver extends StreamObserver {
        void onLayoutChange(List<RemoteMixedStream.Region> var1);

        void onActiveAudioInputChange(String var1);
    }

    public static class Region {
        public final String regionId;
        public final String streamId;
        public final String shape;
        public final HashMap<String, String> parameters = new HashMap();

        Region(JSONObject regionObj) throws JSONException {
            this.streamId = JsonUtils.getString(regionObj, "stream");
            JSONObject region = JsonUtils.getObj(regionObj, "region");
            if (region != null) {
                this.regionId = JsonUtils.getString(region, "id");
                this.shape = JsonUtils.getString(region, "shape");
                JSONObject area = JsonUtils.getObj(region, "area");
                Iterator it = area.keys();

                while(it.hasNext()) {
                    String key = (String)it.next();
                    String value = JsonUtils.getString(area, key);
                    this.parameters.put(key, value);
                }
            } else {
                this.regionId = null;
                this.shape = null;
            }

        }
    }
}
