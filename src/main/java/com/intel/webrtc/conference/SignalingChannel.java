//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intel.webrtc.conference;

import android.util.Base64;
import com.intel.webrtc.base.CheckCondition;
import com.intel.webrtc.base.IcsConst;
import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.client.IO.Options;
import io.socket.emitter.Emitter.Listener;
import java.net.URISyntaxException;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONException;
import org.json.JSONObject;

final class SignalingChannel {
    private final SignalingChannel.SignalingChannelObserver observer;
    private final String token;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private Socket socketClient;
    private String reconnectionTicket;
    private final int MAX_RECONNECT_ATTEMPTS = 5;
    private int reconnectAttempts = 0;
    private Timer refreshTimer;
    private final Object timerLock = new Object();
    private boolean loggedIn = false;
    private final Listener connectedCallback = new Listener() {
        public void call(Object... args) {
            try {
                SignalingChannel.this.login();
            } catch (JSONException var3) {
                SignalingChannel.this.observer.onRoomConnectFailed(var3.getMessage());
            }

        }
    };
    private final Listener connectErrorCallback = new Listener() {
        public void call(final Object... args) {
            SignalingChannel.this.executor.execute(new Runnable() {
                public void run() {
                    String msg = SignalingChannel.this.extractMsg(0, args);
                    if (SignalingChannel.this.reconnectAttempts >= 5) {
                        SignalingChannel.this.observer.onRoomConnectFailed("Socket.IO connected failed: " + msg);
                        if (SignalingChannel.this.loggedIn) {
                            SignalingChannel.this.triggerDisconnected();
                        }
                    }

                }
            });
        }
    };
    private final Listener reconnectingCallback = new Listener() {
        public void call(Object... args) {
            SignalingChannel.this.executor.execute(new Runnable() {
                public void run() {
                    SignalingChannel.this.reconnectAttempts++;
                    if (SignalingChannel.this.loggedIn && SignalingChannel.this.reconnectAttempts == 1) {
                        SignalingChannel.this.observer.onReconnecting();
                    }

                }
            });
        }
    };
    private final Listener disconnectCallback = new Listener() {
        public void call(Object... args) {
            SignalingChannel.this.triggerDisconnected();
        }
    };
    private final Listener progressCallback = new Listener() {
        public void call(final Object... args) {
            SignalingChannel.this.executor.execute(new Runnable() {
                public void run() {
                    JSONObject msg = (JSONObject)args[0];
                    SignalingChannel.this.observer.onProgressMessage(msg);
                }
            });
        }
    };
    private final Listener participantCallback = new Listener() {
        public void call(final Object... args) {
            SignalingChannel.this.executor.execute(new Runnable() {
                public void run() {
                    JSONObject msg = (JSONObject)args[0];

                    try {
                        String var2 = msg.getString("action");
                        byte var3 = -1;
                        switch(var2.hashCode()) {
                        case 3267882:
                            if (var2.equals("join")) {
                                var3 = 0;
                            }
                            break;
                        case 102846135:
                            if (var2.equals("leave")) {
                                var3 = 1;
                            }
                        }

                        switch(var3) {
                        case 0:
                            SignalingChannel.this.observer.onParticipantJoined(msg.getJSONObject("data"));
                            break;
                        case 1:
                            SignalingChannel.this.observer.onParticipantLeft(msg.getString("data"));
                            break;
                        default:
                            CheckCondition.DCHECK(false);
                        }
                    } catch (JSONException var4) {
                        CheckCondition.DCHECK(false);
                    }

                }
            });
        }
    };
    private final Listener streamCallback = new Listener() {
        public void call(final Object... args) {
            SignalingChannel.this.executor.execute(new Runnable() {
                public void run() {
                    try {
                        JSONObject msg = (JSONObject)args[0];
                        String status = msg.getString("status");
                        String streamId = msg.getString("id");
                        byte var5 = -1;
                        switch(status.hashCode()) {
                        case -934610812:
                            if (status.equals("remove")) {
                                var5 = 1;
                            }
                            break;
                        case -838846263:
                            if (status.equals("update")) {
                                var5 = 2;
                            }
                            break;
                        case 96417:
                            if (status.equals("add")) {
                                var5 = 0;
                            }
                        }

                        switch(var5) {
                        case 0:
                            JSONObject data = msg.getJSONObject("data");
                            RemoteStream remoteStream = new RemoteStream(data);
                            SignalingChannel.this.observer.onStreamAdded(remoteStream);
                            break;
                        case 1:
                            SignalingChannel.this.observer.onStreamRemoved(streamId);
                            break;
                        case 2:
                            SignalingChannel.this.observer.onStreamUpdated(streamId, msg.getJSONObject("data"));
                            break;
                        default:
                            CheckCondition.DCHECK(false);
                        }
                    } catch (JSONException var8) {
                        CheckCondition.DCHECK(var8);
                    }

                }
            });
        }
    };
    private final Listener textCallback = new Listener() {
        public void call(final Object... args) {
            SignalingChannel.this.executor.execute(new Runnable() {
                public void run() {
                    JSONObject data = (JSONObject)args[0];

                    try {
                        SignalingChannel.this.observer.onTextMessage(data.getString("from"), data.getString("message"));
                    } catch (JSONException var3) {
                        CheckCondition.DCHECK(false);
                    }

                }
            });
        }
    };
    private final Listener dropCallback = new Listener() {
        public void call(Object... args) {
        }
    };

    SignalingChannel(String token, SignalingChannel.SignalingChannelObserver observer) {
        this.token = token;
        this.observer = observer;
    }

    void connect(final ConferenceClientConfiguration configuration) {
        CheckCondition.DCHECK(this.executor);
        this.executor.execute(new Runnable() {
            public void run() {
                try {
                    CheckCondition.DCHECK(SignalingChannel.this.token);
                    JSONObject jsonToken = new JSONObject(new String(Base64.decode(SignalingChannel.this.token, 0)));
                    boolean isSecure = jsonToken.getBoolean("secure");
                    String host = jsonToken.getString("host");
                    String url = (isSecure ? "https" : "http") + "://" + host;
                    Options opt = new Options();
                    opt.forceNew = true;
                    opt.reconnection = true;
                    opt.reconnectionAttempts = 5;
                    opt.secure = isSecure;
                    if (configuration.sslContext != null) {
                        opt.sslContext = configuration.sslContext;
                    }

                    if (configuration.hostnameVerifier != null) {
                        opt.hostnameVerifier = configuration.hostnameVerifier;
                    }

                    SignalingChannel.this.socketClient = IO.socket(url, opt);
                    SignalingChannel.this.socketClient.on("connect", SignalingChannel.this.connectedCallback).on("connect_error", SignalingChannel.this.connectErrorCallback).on("reconnecting", SignalingChannel.this.reconnectingCallback).on("progress", SignalingChannel.this.progressCallback).on("participant", SignalingChannel.this.participantCallback).on("stream", SignalingChannel.this.streamCallback).on("text", SignalingChannel.this.textCallback).on("drop", SignalingChannel.this.dropCallback);
                    SignalingChannel.this.socketClient.connect();
                } catch (JSONException var6) {
                    SignalingChannel.this.observer.onRoomConnectFailed(var6.getMessage());
                } catch (URISyntaxException var7) {
                    SignalingChannel.this.observer.onRoomConnectFailed(var7.getMessage());
                }

            }
        });
    }

    private void login() throws JSONException {
        JSONObject loginInfo = new JSONObject();
        loginInfo.put("token", this.token);
        loginInfo.put("userAgent", new JSONObject(IcsConst.userAgent));
        loginInfo.put("protocol", "1.0");
        this.socketClient.emit("login", new Object[]{loginInfo, new Ack() {
            public void call(final Object... args) {
                SignalingChannel.this.executor.execute(new Runnable() {
                    public void run() {
                        if (SignalingChannel.this.extractMsg(0, args).equals("ok")) {
                            SignalingChannel.this.observer.onRoomConnected((JSONObject)args[1]);
                        } else {
                            SignalingChannel.this.observer.onRoomConnectFailed(SignalingChannel.this.extractMsg(1, args));
                        }

                    }
                });
            }
        }});
    }

    void disconnect() {
        if (this.socketClient != null) {
            this.socketClient.on("disconnect", this.disconnectCallback);
            this.socketClient.disconnect();
        }

    }

    private void triggerDisconnected() {
        this.loggedIn = false;
        this.reconnectAttempts = 0;
        Object var1 = this.timerLock;
        synchronized(this.timerLock) {
            if (this.refreshTimer != null) {
                this.refreshTimer.cancel();
                this.refreshTimer = null;
            }
        }

        this.observer.onRoomDisconnected();
    }

    void sendMsg(String name, JSONObject args, Ack acknowledge) {
        if (args != null) {
            this.socketClient.emit(name, new Object[]{args, acknowledge});
        } else {
            this.socketClient.emit(name, new Object[]{acknowledge});
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

    interface SignalingChannelObserver {
        void onRoomConnected(JSONObject var1);

        void onRoomConnectFailed(String var1);

        void onReconnecting();

        void onRoomDisconnected();

        void onProgressMessage(JSONObject var1);

        void onTextMessage(String var1, String var2);

        void onStreamAdded(RemoteStream var1);

        void onStreamRemoved(String var1);

        void onStreamUpdated(String var1, JSONObject var2);

        void onParticipantJoined(JSONObject var1);

        void onParticipantLeft(String var1);
    }
}
