//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intel.webrtc.conference;

import com.intel.webrtc.base.ClientConfiguration;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import org.webrtc.PeerConnection.RTCConfiguration;

public final class ConferenceClientConfiguration extends ClientConfiguration {
    SSLContext sslContext;
    HostnameVerifier hostnameVerifier;

    private ConferenceClientConfiguration(RTCConfiguration configuration) {
        super(configuration);
        this.sslContext = null;
        this.hostnameVerifier = null;
    }

    public static ConferenceClientConfiguration.Builder builder() {
        return new ConferenceClientConfiguration.Builder();
    }

    public static class Builder {
        private SSLContext sslContext = null;
        private HostnameVerifier hostnameVerifier = null;
        private RTCConfiguration rtcConfiguration = null;

        Builder() {
        }

        public ConferenceClientConfiguration.Builder setSSLContext(SSLContext sslContext) {
            this.sslContext = sslContext;
            return this;
        }

        public ConferenceClientConfiguration.Builder setHostnameVerifier(HostnameVerifier hostnameVerifier) {
            this.hostnameVerifier = hostnameVerifier;
            return this;
        }

        public ConferenceClientConfiguration.Builder setRTCConfiguration(RTCConfiguration rtcConfiguration) {
            this.rtcConfiguration = rtcConfiguration;
            return this;
        }

        public ConferenceClientConfiguration build() {
            ConferenceClientConfiguration configuration = new ConferenceClientConfiguration(this.rtcConfiguration);
            configuration.sslContext = this.sslContext;
            configuration.hostnameVerifier = this.hostnameVerifier;
            return configuration;
        }
    }
}
