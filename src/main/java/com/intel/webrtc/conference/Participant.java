//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intel.webrtc.conference;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;

public final class Participant {
    public final String id;
    public final String role;
    public final String userId;
    private List<Participant.ParticipantObserver> observers;

    Participant(JSONObject participantInfo) throws JSONException {
        this.id = participantInfo.getString("id");
        this.role = participantInfo.getString("role");
        this.userId = participantInfo.getString("user");
    }

    public void addObserver(Participant.ParticipantObserver observer) {
        if (this.observers == null) {
            this.observers = new ArrayList();
        }

        this.observers.add(observer);
    }

    public void removeObserver(Participant.ParticipantObserver observer) {
        if (this.observers != null) {
            this.observers.remove(observer);
        }

    }

    void onLeft() {
        if (this.observers != null) {
            Iterator var1 = this.observers.iterator();

            while(var1.hasNext()) {
                Participant.ParticipantObserver observer = (Participant.ParticipantObserver)var1.next();
                observer.onLeft();
            }
        }

    }

    public interface ParticipantObserver {
        void onLeft();
    }
}
