package utils;

import org.json.JSONObject;

public interface VolleyListener{

    void onSuccess(int result, JSONObject user, int j, String jwtToken);

}