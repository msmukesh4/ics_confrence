//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intel.webrtc.conference;

import com.intel.webrtc.base.ActionCallback;
import com.intel.webrtc.base.IcsError;
import com.intel.webrtc.base.MediaConstraints.TrackKind;
import io.socket.client.Ack;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.RTCStatsReport;

public final class Publication extends com.intel.webrtc.base.Publication implements MuteEventObserver {
    private final ConferenceClient client;
    private List<Publication.PublicationObserver> observers;

    Publication(String id, MediaStream mediaStream, ConferenceClient client) {
        super(id, mediaStream);
        this.client = client;
    }

    public void addObserver(Publication.PublicationObserver observer) {
        if (this.observers == null) {
            this.observers = new ArrayList();
        }

        this.observers.add(observer);
    }

    public void removeObserver(Publication.PublicationObserver observer) {
        if (this.observers != null) {
            this.observers.remove(observer);
        }

    }

    public void mute(TrackKind trackKind, final ActionCallback<Void> callback) {
        if (this.ended) {
            this.client.triggerCallback(callback, new IcsError(0, "Wrong state"));
        } else {
            Ack ack = new Ack() {
                public void call(Object... args) {
                    if (args[0].equals("ok")) {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    } else {
                        Publication.this.client.triggerCallback(callback, new IcsError(0, ""));
                    }

                }
            };

            try {
                this.client.sendSignalingMessage("stream-control", this.generateMsg(trackKind, true), ack);
            } catch (JSONException var5) {
                callback.onFailure(new IcsError(0, var5.getMessage()));
            }

        }
    }

    public void unmute(TrackKind trackKind, final ActionCallback<Void> callback) {
        if (this.ended) {
            this.client.triggerCallback(callback, new IcsError(0, "Wrong state"));
        } else {
            Ack ack = new Ack() {
                public void call(Object... args) {
                    if (args[0].equals("ok")) {
                        if (callback != null) {
                            callback.onSuccess(null);
                        }
                    } else {
                        Publication.this.client.triggerCallback(callback, new IcsError(0, ""));
                    }

                }
            };

            try {
                this.client.sendSignalingMessage("stream-control", this.generateMsg(trackKind, false), ack);
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

    public void getStats(ActionCallback<RTCStatsReport> callback) {
        if (this.ended) {
            this.client.triggerCallback(callback, new IcsError("Publication has stopped."));
        } else {
            this.client.getStats(this.id, callback);
        }
    }

    public void stop() {
        if (!this.ended) {
            this.client.unpublish(this.id, this);
        }

    }

    void onEnded() {
        this.ended = true;
        if (this.observers != null) {
            Iterator var1 = this.observers.iterator();

            while(var1.hasNext()) {
                Publication.PublicationObserver observer = (Publication.PublicationObserver)var1.next();
                observer.onEnded();
            }
        }

    }

    public void onStatusUpdated(TrackKind trackKind, boolean active) {
        if (this.observers != null) {
            Iterator var3 = this.observers.iterator();

            while(var3.hasNext()) {
                Publication.PublicationObserver observer = (Publication.PublicationObserver)var3.next();
                if (active) {
                    observer.onUnmute(trackKind);
                } else {
                    observer.onMute(trackKind);
                }
            }
        }

    }

    public interface PublicationObserver {
        void onEnded();

        void onMute(TrackKind var1);

        void onUnmute(TrackKind var1);
    }
}
