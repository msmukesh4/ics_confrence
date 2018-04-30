package com.intel.webrtc.conference;

/**
 * Hello world!
 *
 */
import org.json.JSONObject;

public class App 
{
    public static void main( String[] args )
    {
        System.out.println( "Hello World!" );
        JSONObject documentObj = null;
        try {
            documentObj = new JSONObject();
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON String document into a JSON Object.", e);
        }
    }
}
