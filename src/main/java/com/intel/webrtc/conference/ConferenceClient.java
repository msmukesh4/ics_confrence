

package com.intel.webrtc.conference;

import android.util.Log;
import com.intel.webrtc.base.ActionCallback;
import com.intel.webrtc.base.CheckCondition;
import com.intel.webrtc.base.IcsError;
import com.intel.webrtc.base.LocalStream;
import com.intel.webrtc.base.MediaConstraints.TrackKind;
import com.intel.webrtc.base.PeerConnectionChannel.PeerConnectionChannelObserver;
import com.intel.webrtc.conference.SignalingChannel.SignalingChannelObserver;
import io.socket.client.Ack;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.IceCandidate;
import org.webrtc.RTCStatsCollectorCallback;
import org.webrtc.RTCStatsReport;
import org.webrtc.SessionDescription;

public final class ConferenceClient implements SignalingChannelObserver, PeerConnectionChannelObserver {
    private static final String TAG = "ICS";
    private final ConferenceClientConfiguration configuration;
    private SignalingChannel signalingChannel;
    private ConferenceInfo conferenceInfo;
    private ActionCallback<ConferenceInfo> joinCallback;
    private ExecutorService signalingExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    private ConcurrentHashMap<String, ConferencePeerConnectionChannel> pcChannels;
    private ConcurrentHashMap<String, ActionCallback<Subscription>> subCallbacks;
    private ConcurrentHashMap<String, ActionCallback<Publication>> pubCallbacks;
    private ConferenceClient.RoomStates roomStates;
    private final Object statusLock = new Object();
    private List<ConferenceClient.ConferenceClientObserver> observers;

    public ConferenceClient(ConferenceClientConfiguration configuration) {
        this.configuration = configuration;
        this.observers = Collections.synchronizedList(new ArrayList());
        this.pcChannels = new ConcurrentHashMap();
        this.subCallbacks = new ConcurrentHashMap();
        this.pubCallbacks = new ConcurrentHashMap();
        this.roomStates = ConferenceClient.RoomStates.DISCONNECTED;
    }

    public void addObserver(ConferenceClient.ConferenceClientObserver observer) {
        CheckCondition.RCHECK(observer);
        this.observers.add(observer);
    }

    public void removeObserver(ConferenceClient.ConferenceClientObserver observer) {
        CheckCondition.RCHECK(observer);
        this.observers.remove(observer);
    }

    public ConferenceInfo info() {
        return this.conferenceInfo;
    }

    public void join(String token, ActionCallback<ConferenceInfo> callback) {
        if (!this.checkRoomStatus(ConferenceClient.RoomStates.DISCONNECTED)) {
            callback.onFailure(new IcsError("Wrong room status."));
        } else {
            CheckCondition.DCHECK(this.signalingChannel == null);
            this.joinCallback = callback;
            this.signalingChannel = new SignalingChannel(token, this);
            this.changeRoomStatus(ConferenceClient.RoomStates.CONNECTING);
            this.signalingChannel.connect(this.configuration);
        }
    }

    public void leave() {
        if (this.checkRoomStatus(ConferenceClient.RoomStates.DISCONNECTED)) {
            Log.w("ICS", "Wrong room status when leave.");
        } else {
            CheckCondition.DCHECK(this.signalingChannel);
            this.sendSignalingMessage("logout", (JSONObject)null, new Ack() {
                public void call(Object... args) {
                    CheckCondition.DCHECK(ConferenceClient.this.extractMsg(0, args).equals("ok"));
                    ConferenceClient.this.signalingChannel.disconnect();
                }
            });
        }
    }

    public void publish(LocalStream localStream, ActionCallback<Publication> callback) {
        this.publish(localStream, (PublishOptions)null, callback);
    }

    public void publish(final LocalStream localStream, final PublishOptions options, final ActionCallback<Publication> callback) {
        if (!this.checkRoomStatus(ConferenceClient.RoomStates.CONNECTED)) {
            this.triggerCallback(callback, new IcsError("Wrong room status."));
        } else {
            CheckCondition.RCHECK(localStream);
            Ack publishAck = new Ack() {
                public void call(Object... args) {
                    if (ConferenceClient.this.extractMsg(0, args).equals("ok")) {
                        try {
                            JSONObject result = (JSONObject)args[1];
                            ConferencePeerConnectionChannel pcChannel = ConferenceClient.this.getPeerConnection(result.getString("id"), false, false);
                            if (callback != null) {
                                ConferenceClient.this.pubCallbacks.put(result.getString("id"), callback);
                            }

                            pcChannel.publish(localStream, options);
                        } catch (JSONException var4) {
                            ConferenceClient.this.triggerCallback(callback, new IcsError(var4.getMessage()));
                        }
                    } else {
                        ConferenceClient.this.triggerCallback(callback, new IcsError(ConferenceClient.this.extractMsg(1, args)));
                    }

                }
            };

            try {
                JSONObject mediaInfo = new JSONObject();
                JSONObject publishMsg;
                JSONObject attr;
                if (localStream.hasVideo()) {
                    publishMsg = new JSONObject();
                    publishMsg.put("width", localStream.resolutionWidth);
                    publishMsg.put("height", localStream.resolutionHeight);
                    attr = new JSONObject();
                    attr.put("resolution", publishMsg);
                    attr.put("framerate", localStream.frameRate);
                    JSONObject video = new JSONObject();
                    video.put("parameters", attr);
                    video.put("source", localStream.getStreamSourceInfo().videoSourceInfo.type);
                    mediaInfo.put("video", video);
                } else {
                    mediaInfo.put("video", false);
                }

                if (localStream.hasAudio()) {
                    publishMsg = new JSONObject();
                    publishMsg.put("source", localStream.getStreamSourceInfo().audioSourceInfo.type);
                    mediaInfo.put("audio", publishMsg);
                } else {
                    mediaInfo.put("audio", false);
                }

                publishMsg = new JSONObject();
                publishMsg.put("media", mediaInfo);
                if (localStream.getAttributes() != null) {
                    attr = new JSONObject(localStream.getAttributes());
                    publishMsg.put("attributes", attr);
                }

                this.sendSignalingMessage("publish", publishMsg, publishAck);
            } catch (JSONException var9) {
                CheckCondition.DCHECK(var9);
            }

        }
    }

    void unpublish(final String publicationId, final Publication publication) {
        if (!this.checkRoomStatus(ConferenceClient.RoomStates.CONNECTED)) {
            Log.w("ICS", "Wrong room status when unpublish.");
        } else {
            CheckCondition.RCHECK(publicationId);

            try {
                JSONObject unpubMsg = new JSONObject();
                unpubMsg.put("id", publicationId);
                this.sendSignalingMessage("unpublish", unpubMsg, new Ack() {
                    public void call(Object... args) {
                        CheckCondition.DCHECK(ConferenceClient.this.extractMsg(0, args).equals("ok"));
                        ConferencePeerConnectionChannel pcChannel = ConferenceClient.this.getPeerConnection(publicationId);
                        pcChannel.dispose();
                        ConferenceClient.this.pcChannels.remove(publicationId);
                        publication.onEnded();
                    }
                });
            } catch (JSONException var4) {
                CheckCondition.DCHECK(false);
            }

        }
    }

    public void subscribe(RemoteStream remoteStream, ActionCallback<Subscription> callback) {
        this.subscribe(remoteStream, (SubscribeOptions)null, callback);
    }

    public void subscribe(final RemoteStream remoteStream, final SubscribeOptions options, final ActionCallback<Subscription> callback) {
        if (!this.checkRoomStatus(ConferenceClient.RoomStates.CONNECTED)) {
            this.triggerCallback(callback, new IcsError("Wrong room status."));
        } else {
            CheckCondition.RCHECK(remoteStream);
            final boolean subVideo = options == null || options.videoOption != null;
            final boolean subAudio = options == null || options.audioOption != null;
            Ack subscribeAck = new Ack() {
                public void call(Object... args) {
                    if (ConferenceClient.this.extractMsg(0, args).equals("ok")) {
                        Iterator var2 = ConferenceClient.this.pcChannels.values().iterator();

                        ConferencePeerConnectionChannel pcChannel;
                        while(var2.hasNext()) {
                            pcChannel = (ConferencePeerConnectionChannel)var2.next();
                            if (pcChannel.stream.id().equals(remoteStream.id())) {
                                ConferenceClient.this.triggerCallback(callback, new IcsError("Remote stream has been subscribed."));
                                return;
                            }
                        }

                        JSONObject result = (JSONObject)args[1];

                        try {
                            pcChannel = ConferenceClient.this.getPeerConnection(result.getString("id"), subVideo, subAudio);
                            if (callback != null) {
                                ConferenceClient.this.subCallbacks.put(result.getString("id"), callback);
                            }

                            pcChannel.subscribe(remoteStream, options);
                        } catch (JSONException var4) {
                            ConferenceClient.this.triggerCallback(callback, new IcsError(var4.getMessage()));
                        }
                    } else {
                        ConferenceClient.this.triggerCallback(callback, new IcsError(ConferenceClient.this.extractMsg(1, args)));
                    }

                }
            };

            try {
                JSONObject media = new JSONObject();
                JSONObject subscribeMsg;
                if (subVideo) {
                    subscribeMsg = new JSONObject();
                    subscribeMsg.put("from", remoteStream.id());
                    if (options != null) {
                        subscribeMsg.put("parameters", options.videoOption.generateOptionsMsg());
                    }

                    media.put("video", subscribeMsg);
                } else {
                    media.put("video", false);
                }

                if (subAudio) {
                    subscribeMsg = new JSONObject();
                    subscribeMsg.put("from", remoteStream.id());
                    media.put("audio", subscribeMsg);
                } else {
                    media.put("audio", false);
                }

                subscribeMsg = new JSONObject();
                subscribeMsg.put("media", media);
                this.sendSignalingMessage("subscribe", subscribeMsg, subscribeAck);
            } catch (JSONException var9) {
                CheckCondition.DCHECK(var9);
            }

        }
    }

    void unsubscribe(final String subscriptionId, final Subscription subscription) {
        if (!this.checkRoomStatus(ConferenceClient.RoomStates.CONNECTED)) {
            Log.w("ICS", "Wrong room status when unsubscribe.");
        } else {
            CheckCondition.RCHECK(subscriptionId);

            try {
                JSONObject unpubMsg = new JSONObject();
                unpubMsg.put("id", subscriptionId);
                this.sendSignalingMessage("unsubscribe", unpubMsg, new Ack() {
                    public void call(Object... args) {
                        if (ConferenceClient.this.pcChannels.containsKey(subscriptionId)) {
                            ConferencePeerConnectionChannel pcChannel = ConferenceClient.this.getPeerConnection(subscriptionId);
                            pcChannel.dispose();
                            ConferenceClient.this.pcChannels.remove(subscriptionId);
                            subscription.onEnded();
                        }

                    }
                });
            } catch (JSONException var4) {
                CheckCondition.DCHECK(false);
            }

        }
    }

    public void send(String message, ActionCallback<Void> callback) {
        this.send((String)null, message, callback);
    }

    public void send(String participantId, String message, final ActionCallback<Void> callback) {
        if (!this.checkRoomStatus(ConferenceClient.RoomStates.CONNECTED)) {
            this.triggerCallback(callback, new IcsError(0, "Wrong status"));
        } else {
            CheckCondition.RCHECK(message);

            try {
                JSONObject sendMsg = new JSONObject();
                sendMsg.put("to", participantId == null ? "all" : participantId);
                sendMsg.put("message", message);
                this.sendSignalingMessage("text", sendMsg, new Ack() {
                    public void call(Object... args) {
                        if (ConferenceClient.this.extractMsg(0, args).equals("ok")) {
                            ConferenceClient.this.callbackExecutor.execute(new Runnable() {
                                public void run() {
                                    if (callback != null) {
                                        callback.onSuccess((Void)null);
                                    }

                                }
                            });
                        } else {
                            ConferenceClient.this.triggerCallback(callback, new IcsError(ConferenceClient.this.extractMsg(1, args)));
                        }

                    }
                });
            } catch (JSONException var5) {
                CheckCondition.DCHECK(false);
            }

        }
    }

    void getStats(String id, final ActionCallback<RTCStatsReport> callback) {
        if (!this.pcChannels.containsKey(id)) {
            this.triggerCallback(callback, new IcsError(0, "Wrong state"));
        } else {
            ConferencePeerConnectionChannel pcChannel = this.getPeerConnection(id);
            pcChannel.getConnectionStats(new RTCStatsCollectorCallback() {
                public void onStatsDelivered(RTCStatsReport rtcStatsReport) {
                    if (callback != null) {
                        callback.onSuccess(rtcStatsReport);
                    }

                }
            });
        }
    }

    private void closeInternal() {
//        Iterator var1 = this.pcChannels.keySet().iterator();
        Iterator var1 = Collections.list(this.pcChannels.keys()).iterator();
        while(var1.hasNext()) {
            String key = (String)var1.next();
            ((ConferencePeerConnectionChannel)this.pcChannels.get(key)).dispose();
        }

        this.pcChannels.clear();
        this.subCallbacks.clear();
        this.pubCallbacks.clear();
        this.signalingChannel = null;
        this.conferenceInfo = null;
        this.joinCallback = null;
    }

    private boolean checkRoomStatus(ConferenceClient.RoomStates roomStates) {
        Object var2 = this.statusLock;
        synchronized(this.statusLock) {
            return this.roomStates == roomStates;
        }
    }

    private void changeRoomStatus(ConferenceClient.RoomStates roomStates) {
        Object var2 = this.statusLock;
        synchronized(this.statusLock) {
            this.roomStates = roomStates;
        }
    }

    private ConferencePeerConnectionChannel getPeerConnection(String id) {
        CheckCondition.DCHECK(this.pcChannels.containsKey(id));
        return this.getPeerConnection(id, true, true);
    }

    private ConferencePeerConnectionChannel getPeerConnection(String id, boolean enableVideo, boolean enableAudio) {
        if (this.pcChannels.containsKey(id)) {
            return (ConferencePeerConnectionChannel)this.pcChannels.get(id);
        } else {
            ConferencePeerConnectionChannel pcChannel = new ConferencePeerConnectionChannel(id, this.configuration.rtcConfiguration, enableVideo, enableAudio, this);
            this.pcChannels.put(id, pcChannel);
            return pcChannel;
        }
    }

    <T> void triggerCallback(final ActionCallback<T> callback, final IcsError error) {
        CheckCondition.DCHECK(this.callbackExecutor);
        if (callback != null) {
            this.callbackExecutor.execute(new Runnable() {
                public void run() {
                    callback.onFailure(error);
                }
            });
        }
    }

    private String extractMsg(int position, Object... args) {
        if (position >= 0 && args != null && args.length >= position + 1 && args[position] != null) {
            return args[position].toString();
        } else {
            CheckCondition.DCHECK(false);
            return "";
        }
    }

    void sendSignalingMessage(final String type, final JSONObject message, final Ack ack) {
        CheckCondition.DCHECK(this.signalingExecutor);
        CheckCondition.DCHECK(this.signalingChannel);
        this.signalingExecutor.execute(new Runnable() {
            public void run() {
                ConferenceClient.this.signalingChannel.sendMsg(type, message, ack);
            }
        });
    }

    private void processAck(final String id) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                ActionCallback callback;
                if (ConferenceClient.this.pubCallbacks.containsKey(id)) {
                    callback = (ActionCallback)ConferenceClient.this.pubCallbacks.get(id);
                    ConferencePeerConnectionChannel pcChannel = ConferenceClient.this.getPeerConnection(id);
                    Publication publication = new Publication(id, pcChannel.getMediaStream(), ConferenceClient.this);
                    ConferenceClient.this.getPeerConnection(id).muteEventObserver = publication;
                    callback.onSuccess(publication);
                    ConferenceClient.this.pubCallbacks.remove(id);
                } else {
                    if (ConferenceClient.this.subCallbacks.containsKey(id)) {
                        callback = (ActionCallback)ConferenceClient.this.subCallbacks.get(id);
                        Subscription subscription = new Subscription(id, ConferenceClient.this);
                        ConferenceClient.this.getPeerConnection(id).muteEventObserver = subscription;
                        callback.onSuccess(subscription);
                        ConferenceClient.this.subCallbacks.remove(id);
                    }

                }
            }
        });
    }

    private void processError(final String id, final String errorMsg) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                ActionCallback callback;
                if (ConferenceClient.this.pubCallbacks.containsKey(id)) {
                    callback = (ActionCallback)ConferenceClient.this.pubCallbacks.get(id);
                    ConferenceClient.this.triggerCallback(callback, new IcsError(errorMsg));
                    ConferenceClient.this.pubCallbacks.remove(id);
                } else {
                    callback = (ActionCallback)ConferenceClient.this.subCallbacks.get(id);
                    ConferenceClient.this.triggerCallback(callback, new IcsError(errorMsg));
                    ConferenceClient.this.subCallbacks.remove(id);
                }

            }
        });
    }

    public void onRoomConnected(final JSONObject info) {
        CheckCondition.DCHECK(this.callbackExecutor);
        this.changeRoomStatus(ConferenceClient.RoomStates.CONNECTED);
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                try {
                    if (ConferenceClient.this.joinCallback != null) {
                        ConferenceInfo conferenceInfo = new ConferenceInfo(info);
                        ConferenceClient.this.conferenceInfo = conferenceInfo;
                        ConferenceClient.this.joinCallback.onSuccess(conferenceInfo);
                    }
                } catch (JSONException var3) {
                    ConferenceClient.this.triggerCallback(ConferenceClient.this.joinCallback, new IcsError(var3.getMessage()));
                }

                ConferenceClient.this.joinCallback = null;
            }
        });
    }

    public void onRoomConnectFailed(String errorMsg) {
        CheckCondition.DCHECK(this.callbackExecutor);
        this.changeRoomStatus(ConferenceClient.RoomStates.DISCONNECTED);
        this.signalingChannel = null;
        this.triggerCallback(this.joinCallback, new IcsError(errorMsg));
    }

    public void onReconnecting() {
    }

    public void onRoomDisconnected() {
        CheckCondition.DCHECK(this.callbackExecutor);
        this.changeRoomStatus(ConferenceClient.RoomStates.DISCONNECTED);
        this.closeInternal();
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                Iterator var1 = ConferenceClient.this.observers.iterator();

                while(var1.hasNext()) {
                    ConferenceClient.ConferenceClientObserver observer = (ConferenceClient.ConferenceClientObserver)var1.next();
                    observer.onServerDisconnected();
                }

            }
        });
    }

    public void onProgressMessage(JSONObject msg) {
        CheckCondition.DCHECK(msg);

        try {
            ConferencePeerConnectionChannel pcChannel = this.getPeerConnection(msg.getString("id"));
            String var3 = msg.getString("status");
            byte var4 = -1;
            switch(var3.hashCode()) {
            case 3535742:
                if (var3.equals("soac")) {
                    var4 = 0;
                }
                break;
            case 96784904:
                if (var3.equals("error")) {
                    var4 = 2;
                }
                break;
            case 108386723:
                if (var3.equals("ready")) {
                    var4 = 1;
                }
            }

            switch(var4) {
            case 0:
                pcChannel.processSignalingMessage(msg.getJSONObject("data"));
                break;
            case 1:
                this.processAck(msg.getString("id"));
                break;
            case 2:
                this.processError(msg.getString("id"), msg.getString("data"));
                break;
            default:
                CheckCondition.DCHECK(false);
            }
        } catch (JSONException var5) {
            CheckCondition.DCHECK(false);
        }

    }

    public void onTextMessage(final String participantId, final String message) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                Iterator var1 = ConferenceClient.this.observers.iterator();

                while(var1.hasNext()) {
                    ConferenceClient.ConferenceClientObserver observer = (ConferenceClient.ConferenceClientObserver)var1.next();
                    observer.onMessageReceived(participantId, message);
                }

            }
        });
    }

    public void onStreamAdded(final RemoteStream remoteStream) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                ConferenceClient.this.conferenceInfo.remoteStreams.add(remoteStream);
                Iterator var1 = ConferenceClient.this.observers.iterator();

                while(var1.hasNext()) {
                    ConferenceClient.ConferenceClientObserver observer = (ConferenceClient.ConferenceClientObserver)var1.next();
                    observer.onStreamAdded(remoteStream);
                }

            }
        });
    }

    public void onStreamRemoved(final String streamId) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                Iterator var1 = ConferenceClient.this.conferenceInfo.remoteStreams.iterator();

                while(var1.hasNext()) {
                    RemoteStream remoteStream = (RemoteStream)var1.next();
                    if (remoteStream.id().equals(streamId)) {
                        remoteStream.onEnded();
                        ConferenceClient.this.conferenceInfo.remoteStreams.remove(remoteStream);
                        break;
                    }
                }

            }
        });
    }

    public void onStreamUpdated(final String id, final JSONObject updateInfo) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                try {
                    String field = updateInfo.getString("field");
                    Iterator var2;
                    RemoteStream remoteStream;
                    if (field.equals("video.layout")) {
                        var2 = ConferenceClient.this.conferenceInfo.remoteStreams.iterator();

                        while(var2.hasNext()) {
                            remoteStream = (RemoteStream)var2.next();
                            if (remoteStream.id().equals(id)) {
                                ((RemoteMixedStream)remoteStream).updateRegions(updateInfo.getJSONArray("value"));
                            }
                        }
                    } else if (!field.equals("audio.status") && !field.equals("video.status")) {
                        if (field.equals("activeInput")) {
                            var2 = ConferenceClient.this.conferenceInfo.remoteStreams.iterator();

                            while(var2.hasNext()) {
                                remoteStream = (RemoteStream)var2.next();
                                if (remoteStream.id().equals(id)) {
                                    ((RemoteMixedStream)remoteStream).updateActiveInput(updateInfo.getString("value"));
                                }
                            }
                        }
                    } else {
                        var2 = ConferenceClient.this.pcChannels.values().iterator();

                        while(true) {
                            ConferencePeerConnectionChannel pcChannel;
                            do {
                                if (!var2.hasNext()) {
                                    return;
                                }

                                pcChannel = (ConferencePeerConnectionChannel)var2.next();
                            } while(!pcChannel.stream.id().equals(id) && !pcChannel.key.equals(id));

                            if (pcChannel.muteEventObserver != null) {
                                TrackKind trackKind = field.equals("audio.status") ? TrackKind.AUDIO : TrackKind.VIDEO;
                                boolean active = updateInfo.getString("value").equals("active");
                                pcChannel.muteEventObserver.onStatusUpdated(trackKind, active);
                            }
                        }
                    }
                } catch (JSONException var6) {
                    CheckCondition.DCHECK(var6);
                }

            }
        });
    }

    public void onParticipantJoined(final JSONObject participantInfo) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                try {
                    Participant participant = new Participant(participantInfo);
                    ConferenceClient.this.conferenceInfo.participants.add(participant);
                    Iterator var2 = ConferenceClient.this.observers.iterator();

                    while(var2.hasNext()) {
                        ConferenceClient.ConferenceClientObserver observer = (ConferenceClient.ConferenceClientObserver)var2.next();
                        observer.onParticipantJoined(participant);
                    }
                } catch (JSONException var4) {
                    CheckCondition.DCHECK(false);
                }

            }
        });
    }

    public void onParticipantLeft(final String participantId) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                Iterator var1 = ConferenceClient.this.conferenceInfo.participants.iterator();

                while(var1.hasNext()) {
                    Participant participant = (Participant)var1.next();
                    if (participant.id.equals(participantId)) {
                        participant.onLeft();
                        ConferenceClient.this.conferenceInfo.participants.remove(participant);
                        break;
                    }
                }

            }
        });
    }

    public void onIceCandidate(final String id, final IceCandidate candidate) {
        this.signalingExecutor.execute(new Runnable() {
            public void run() {
                try {
                    JSONObject candidateObj = new JSONObject();
                    candidateObj.put("sdpMLineIndex", candidate.sdpMLineIndex);
                    candidateObj.put("sdpMid", candidate.sdpMid);
                    candidateObj.put("candidate", candidate.sdp.indexOf("a=") == 0 ? candidate.sdp : "a=" + candidate.sdp);
                    JSONObject candidateMsg = new JSONObject();
                    candidateMsg.put("type", "candidate");
                    candidateMsg.put("candidate", candidateObj);
                    JSONObject msg = new JSONObject();
                    msg.put("id", id);
                    msg.put("signaling", candidateMsg);
                    ConferenceClient.this.sendSignalingMessage("soac", msg, (Ack)null);
                } catch (JSONException var4) {
                    CheckCondition.DCHECK(var4);
                }

            }
        });
    }

    public void onLocalDescription(final String id, final SessionDescription localSdp) {
        this.signalingExecutor.execute(new Runnable() {
            public void run() {
                try {
                    SessionDescription sdp = new SessionDescription(localSdp.type, localSdp.description.replaceAll("a=ice-options:google-ice\r\n", ""));
                    JSONObject sdpObj = new JSONObject();
                    sdpObj.put("type", sdp.type.toString().toLowerCase(Locale.US));
                    sdpObj.put("sdp", sdp.description);
                    JSONObject msg = new JSONObject();
                    msg.put("id", id);
                    msg.put("signaling", sdpObj);
                    ConferenceClient.this.sendSignalingMessage("soac", msg, (Ack)null);
                } catch (JSONException var4) {
                    CheckCondition.DCHECK(var4);
                }

            }
        });
    }

    public void onError(final String id, final String errorMsg) {
        if (this.pcChannels.containsKey(id)) {
            ((ConferencePeerConnectionChannel)this.pcChannels.get(id)).dispose();
            this.pcChannels.remove(id);
        }

        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                if (ConferenceClient.this.pubCallbacks.containsKey(id)) {
                    ConferenceClient.this.triggerCallback((ActionCallback)ConferenceClient.this.pubCallbacks.get(id), new IcsError(0, errorMsg));
                    ConferenceClient.this.pubCallbacks.remove(id);
                }

                if (ConferenceClient.this.subCallbacks.containsKey(id)) {
                    ConferenceClient.this.triggerCallback((ActionCallback)ConferenceClient.this.subCallbacks.get(id), new IcsError(0, errorMsg));
                    ConferenceClient.this.subCallbacks.remove(id);
                }

            }
        });
    }

    public void onAddStream(String key, com.intel.webrtc.base.RemoteStream remoteStream) {
    }

    public void onDataChannelMessage(String key, String message) {
    }

    public void onRenegotiationRequest(String key) {
    }

    private static enum RoomStates {
        DISCONNECTED,
        CONNECTING,
        CONNECTED;

        private RoomStates() {
        }
    }

    public interface ConferenceClientObserver {
        void onStreamAdded(RemoteStream var1);

        void onParticipantJoined(Participant var1);

        void onMessageReceived(String var1, String var2);

        void onServerDisconnected();
    }
}
