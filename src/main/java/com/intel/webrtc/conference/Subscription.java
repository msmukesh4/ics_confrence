//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intel.webrtc.conference;

import com.intel.webrtc.base.ActionCallback;
import com.intel.webrtc.base.CheckCondition;
import com.intel.webrtc.base.IcsError;
import com.intel.webrtc.base.MediaConstraints.TrackKind;
import io.socket.client.Ack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.RTCStatsReport;

public final class Subscription implements MuteEventObserver {
    public final String id;
    private final ConferenceClient client;
    private List<Subscription.SubscriptionObserver> observers;
    private boolean ended = false;

    Subscription(String id, ConferenceClient client) {
        this.id = id;
        this.client = client;
    }

    public void mute(final TrackKind trackKind, final ActionCallback<Void> callback) {
        if (this.ended) {
            this.client.triggerCallback(callback, new IcsError(0, "Wrong state"));
        } else {
            Ack ack = new Ack() {
                public void call(Object... args) {
                    if (args[0].equals("ok")) {
                        Subscription.this.onStatusUpdated(trackKind, false);
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    } else {
                        Subscription.this.client.triggerCallback(callback, new IcsError(0, ""));
                    }

                }
            };

            try {
                this.client.sendSignalingMessage("subscription-control", this.generateMsg(trackKind, true), ack);
            } catch (JSONException var5) {
                callback.onFailure(new IcsError(0, var5.getMessage()));
            }

        }
    }

    public void unmute(final TrackKind trackKind, final ActionCallback<Void> callback) {
        if (this.ended) {
            this.client.triggerCallback(callback, new IcsError(0, "Wrong state"));
        } else {
            Ack ack = new Ack() {
                public void call(Object... args) {
                    if (args[0].equals("ok")) {
                        Subscription.this.onStatusUpdated(trackKind, true);
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    } else {
                        Subscription.this.client.triggerCallback(callback, new IcsError(0, ""));
                    }

                }
            };

            try {
                this.client.sendSignalingMessage("subscription-control", this.generateMsg(trackKind, false), ack);
            } catch (JSONException var5) {
                callback.onFailure(new IcsError(0, var5.getMessage()));
            }

        }
    }

    private JSONObject generateMsg(TrackKind trackKind, boolean mute) throws JSONException {
        JSONObject msg = new JSONObject();
        msg.put("id", this.id);
        msg.put("operation", mute ? "pause" : "play");
        msg.put("data", trackKind.kind);
        return msg;
    }

    public void applyOptions(Subscription.VideoUpdateOptions updateOptions, final ActionCallback<Void> callback) {
        if (this.ended) {
            this.client.triggerCallback(callback, new IcsError(0, "Wrong state"));
        } else {
            CheckCondition.RCHECK(updateOptions);
            JSONObject msg = new JSONObject();

            try {
                msg.put("id", this.id);
                msg.put("operation", "update");
                msg.put("data", updateOptions.generateOptionMsg());
            } catch (JSONException var5) {
                this.client.triggerCallback(callback, new IcsError(0, var5.getMessage()));
            }

            this.client.sendSignalingMessage("subscription-control", msg, new Ack() {
                public void call(Object... args) {
                    if (args[0].equals("ok")) {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    } else {
                        Subscription.this.client.triggerCallback(callback, new IcsError(0, ""));
                    }

                }
            });
        }
    }

    public void getStats(ActionCallback<RTCStatsReport> callback) {
        if (this.ended) {
            this.client.triggerCallback(callback, new IcsError(0, "Wrong state"));
        } else {
            this.client.getStats(this.id, callback);
        }
    }

    public void stop() {
        if (!this.ended) {
            this.client.unsubscribe(this.id, this);
        }

    }

    public void addObserver(Subscription.SubscriptionObserver observer) {
        if (this.observers == null) {
            this.observers = new ArrayList();
        }

        this.observers.add(observer);
    }

    public void removeObserver(Subscription.SubscriptionObserver observer) {
        if (this.observers != null) {
            this.observers.remove(observer);
        }

    }

    void onEnded() {
        this.ended = true;
        if (this.observers != null) {
            Iterator var1 = this.observers.iterator();

            while(var1.hasNext()) {
                Subscription.SubscriptionObserver observer = (Subscription.SubscriptionObserver)var1.next();
                observer.onEnded();
            }
        }

    }

    public void onStatusUpdated(TrackKind trackKind, boolean active) {
        if (this.observers != null) {
            Iterator var3 = this.observers.iterator();

            while(var3.hasNext()) {
                Subscription.SubscriptionObserver observer = (Subscription.SubscriptionObserver)var3.next();
                if (active) {
                    observer.onUnmute(trackKind);
                } else {
                    observer.onMute(trackKind);
                }
            }
        }

    }

    public static final class VideoUpdateOptions {
        public int resolutionHeight = 0;
        public int resolutionWidth = 0;
        public int fps = 0;
        public int keyframeInterval = 0;
        public double bitrateMultiplier = 0.0D;

        public VideoUpdateOptions() {
        }

        JSONObject generateOptionMsg() throws JSONException {
            JSONObject optionMsg = new JSONObject();
            JSONObject video = new JSONObject();
            JSONObject parameters = new JSONObject();
            if (this.resolutionWidth != 0 && this.resolutionHeight != 0) {
                JSONObject reso = new JSONObject();
                reso.put("width", this.resolutionWidth);
                reso.put("height", this.resolutionHeight);
                parameters.put("resolution", reso);
            }

            if (this.fps != 0) {
                parameters.put("framerate", this.fps);
            }

            if (this.keyframeInterval != 0) {
                parameters.put("keyFrameInterval", this.keyframeInterval);
            }

            if (this.bitrateMultiplier != 0.0D) {
                parameters.put("bitrate", "x" + this.bitrateMultiplier);
            }

            video.put("parameters", parameters);
            optionMsg.put("video", video);
            return optionMsg;
        }
    }

    public interface SubscriptionObserver {
        void onEnded();

        void onMute(TrackKind var1);

        void onUnmute(TrackKind var1);
    }
}
