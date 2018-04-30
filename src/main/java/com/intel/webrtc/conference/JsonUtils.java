//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.intel.webrtc.conference;

import com.intel.webrtc.base.CheckCondition;
import org.json.JSONException;
import org.json.JSONObject;

final class JsonUtils {
    JsonUtils() {
    }

    static String getString(JSONObject jsonObject, String key) {
        return getString(jsonObject, key, (String)null);
    }

    static String getString(JSONObject jsonObject, String key, String defaultVal) {
        if (jsonObject == null) {
            return defaultVal;
        } else {
            try {
                if (defaultVal == null) {
                    CheckCondition.DCHECK(jsonObject.has(key));
                }

                return jsonObject.has(key) ? jsonObject.getString(key) : defaultVal;
            } catch (JSONException var4) {
                return defaultVal;
            }
        }
    }

    static int getInt(JSONObject jsonObject, String key, int defaultVal) {
        if (jsonObject == null) {
            return defaultVal;
        } else {
            int result = defaultVal;

            try {
                result = jsonObject.has(key) ? jsonObject.getInt(key) : defaultVal;
            } catch (JSONException var5) {
                CheckCondition.DCHECK(var5);
            }

            return result;
        }
    }

    static JSONObject getObj(JSONObject jsonObject, String key) {
        return getObj(jsonObject, key, false);
    }

    static JSONObject getObj(JSONObject jsonObject, String key, boolean mandatory) {
        CheckCondition.DCHECK(jsonObject);
        CheckCondition.DCHECK(key);

        try {
            if (mandatory) {
                CheckCondition.DCHECK(jsonObject.has(key));
            }

            return jsonObject.getJSONObject(key);
        } catch (JSONException var4) {
            return null;
        }
    }
}
