//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intel.webrtc.conference;

import com.intel.webrtc.base.AudioCodecParameters;
import com.intel.webrtc.base.AudioEncodingParameters;
import com.intel.webrtc.base.CheckCondition;
import com.intel.webrtc.base.LocalStream;
import com.intel.webrtc.base.PeerConnectionChannel;
import com.intel.webrtc.base.Stream;
import com.intel.webrtc.base.VideoCodecParameters;
import com.intel.webrtc.base.VideoEncodingParameters;
import com.intel.webrtc.base.PeerConnectionChannel.PeerConnectionChannelObserver;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.webrtc.IceCandidate;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnection.IceConnectionState;
import org.webrtc.PeerConnection.RTCConfiguration;
import org.webrtc.PeerConnection.SignalingState;

final class ConferencePeerConnectionChannel extends PeerConnectionChannel {
    private boolean remoteSdpSet = false;
    private List<IceCandidate> queuedLocalCandidates = new LinkedList();
    Stream stream;
    MuteEventObserver muteEventObserver;

    ConferencePeerConnectionChannel(String key, RTCConfiguration configuration, boolean enableVideo, boolean enableAudio, PeerConnectionChannelObserver observer) {
        super(key, configuration, enableVideo, enableAudio, observer);
    }

    void publish(LocalStream localStream, PublishOptions options) {
        this.stream = localStream;
        Iterator var3;
        if (options != null && options.videoEncodingParameters != null && options.videoEncodingParameters.size() != 0) {
            this.videoCodecs = new ArrayList();
            var3 = options.videoEncodingParameters.iterator();

            while(var3.hasNext()) {
                VideoEncodingParameters param = (VideoEncodingParameters)var3.next();
                this.videoCodecs.add(param.codec.name);
            }

            this.videoMaxBitrate = VideoEncodingParameters.maxBitrate;
        }

        if (options != null && options.audioEncodingParameters != null && options.audioEncodingParameters.size() != 0) {
            this.audioCodecs = new ArrayList();
            var3 = options.audioEncodingParameters.iterator();

            while(var3.hasNext()) {
                AudioEncodingParameters param = (AudioEncodingParameters)var3.next();
                this.audioCodecs.add(param.codec.name);
            }

            this.audioMaxBitrate = AudioEncodingParameters.maxBitrate;
        }

        this.addStream(GetMediaStream(localStream));
        this.createOffer();
    }

    void subscribe(RemoteStream remoteStream, SubscribeOptions options) {
        this.stream = remoteStream;
        Iterator var3;
        if (options != null && options.videoOption != null && options.videoOption.codecs.size() != 0) {
            this.videoCodecs = new ArrayList();
            var3 = options.videoOption.codecs.iterator();

            while(var3.hasNext()) {
                VideoCodecParameters param = (VideoCodecParameters)var3.next();
                this.videoCodecs.add(param.name);
            }
        }

        if (options != null && options.audioOption != null && options.audioOption.codecs.size() != 0) {
            this.audioCodecs = new ArrayList();
            var3 = options.audioOption.codecs.iterator();

            while(var3.hasNext()) {
                AudioCodecParameters param = (AudioCodecParameters)var3.next();
                this.audioCodecs.add(param.name);
            }
        }

        this.createOffer();
    }

    protected synchronized void dispose() {
        if (this.stream instanceof LocalStream) {
            this.removeStream(GetMediaStream(this.stream));
        }

        super.dispose();
    }

    MediaStream getMediaStream() {
        return GetMediaStream(this.stream);
    }

    public void onSetSuccess() {
        if (this.signalingState == SignalingState.STABLE) {
            this.remoteSdpSet = true;
            Iterator var1 = this.queuedLocalCandidates.iterator();

            while(var1.hasNext()) {
                IceCandidate iceCandidate = (IceCandidate)var1.next();
                this.observer.onIceCandidate(this.key, iceCandidate);
            }

            this.queuedLocalCandidates.clear();
            if (this.stream instanceof LocalStream) {
                this.setMaxBitrate(GetMediaStream(this.stream));
            }
        }

    }

    public void onCreateFailure(final String error) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                ConferencePeerConnectionChannel.this.observer.onError(ConferencePeerConnectionChannel.this.key, error);
            }
        });
    }

    public void onSetFailure(final String error) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                ConferencePeerConnectionChannel.this.observer.onError(ConferencePeerConnectionChannel.this.key, error);
            }
        });
    }

    public void onSignalingChange(final SignalingState signalingState) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                ConferencePeerConnectionChannel.this.signalingState = signalingState;
            }
        });
    }

    public void onIceConnectionChange(final IceConnectionState iceConnectionState) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                if (iceConnectionState == IceConnectionState.CLOSED) {
                    ConferencePeerConnectionChannel.this.observer.onError(ConferencePeerConnectionChannel.this.key, "");
                }

            }
        });
    }

    public void onIceCandidate(final IceCandidate iceCandidate) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                if (ConferencePeerConnectionChannel.this.remoteSdpSet) {
                    ConferencePeerConnectionChannel.this.observer.onIceCandidate(ConferencePeerConnectionChannel.this.key, iceCandidate);
                } else {
                    ConferencePeerConnectionChannel.this.queuedLocalCandidates.add(iceCandidate);
                }

            }
        });
    }

    public void onAddStream(final MediaStream mediaStream) {
        CheckCondition.DCHECK(this.stream);
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                ((RemoteStream)ConferencePeerConnectionChannel.this.stream).setMediaStream(mediaStream);
                ConferencePeerConnectionChannel.this.observer.onAddStream(ConferencePeerConnectionChannel.this.key, (com.intel.webrtc.base.RemoteStream)ConferencePeerConnectionChannel.this.stream);
            }
        });
    }

    public void onRemoveStream(MediaStream mediaStream) {
        this.callbackExecutor.execute(new Runnable() {
            public void run() {
                ((RemoteStream)ConferencePeerConnectionChannel.this.stream).onEnded();
            }
        });
    }

    public void onRenegotiationNeeded() {
    }
}
