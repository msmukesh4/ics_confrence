//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intel.webrtc.conference;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class ConferenceInfo {
    private String id;
    private Participant self;
    List<Participant> participants = Collections.synchronizedList(new ArrayList());
    List<RemoteStream> remoteStreams = Collections.synchronizedList(new ArrayList());

    ConferenceInfo(JSONObject conferenceInfo) throws JSONException {
        this.updateInfo(conferenceInfo);
    }

    private void updateInfo(JSONObject conferenceInfo) throws JSONException {
        JSONObject room = conferenceInfo.getJSONObject("room");
        this.id = room.getString("id");
        JSONArray participantsInfo = room.getJSONArray("participants");

        for(int i = 0; i < participantsInfo.length(); ++i) {
            JSONObject participantInfo = participantsInfo.getJSONObject(i);
            Participant participant = new Participant(participantInfo);
            this.participants.add(participant);
            if (participant.id.equals(conferenceInfo.getString("id"))) {
                this.self = participant;
            }
        }

        JSONArray streamsInfo = room.getJSONArray("streams");

        for(int i = 0; i < streamsInfo.length(); ++i) {
            JSONObject streamInfo = streamsInfo.getJSONObject(i);
            Object remoteStream;
            if (streamInfo.getString("type").equals("mixed")) {
                remoteStream = new RemoteMixedStream(streamInfo);
            } else {
                remoteStream = new RemoteStream(streamInfo);
            }

            this.remoteStreams.add((RemoteStream) remoteStream);
        }

    }

    public String id() {
        return this.id;
    }

    public Participant self() {
        return this.self;
    }

    public List<Participant> getParticipants() {
        return Collections.unmodifiableList(this.participants);
    }

    public List<RemoteStream> getRemoteStreams() {
        return Collections.unmodifiableList(this.remoteStreams);
    }
}
