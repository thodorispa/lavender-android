package utils;

import androidx.annotation.Nullable;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
public class VolleyOverride {

    /** Nested JsonArrayRequest class in order to override JsonArrayRequest method
     *  to send a JSONObject but receive a JSONArray instead of a JSONObject. */
    public static class JsonArrayRequest extends JsonRequest<JSONArray> {

        public JsonArrayRequest(
                String url, Listener<JSONArray> listener, @Nullable Response.ErrorListener errorListener) {
            super(Method.GET, url, null, listener, errorListener);
        }

        public JsonArrayRequest(int method, String url, JSONObject jsonRequest,
                                Listener<JSONArray> listener, Response.ErrorListener errorListener) {
            super(method, url, (jsonRequest == null) ? null : jsonRequest.toString(),
                    listener, errorListener);
        }

        @Override
        protected Response<JSONArray> parseNetworkResponse(NetworkResponse response) {
            try {
                String jsonString =
                        new String(
                                response.data,
                                HttpHeaderParser.parseCharset(response.headers, PROTOCOL_CHARSET));
                return Response.success(
                        new JSONArray(jsonString), HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException | JSONException e) {
                return Response.error(new ParseError(e));
            }
        }
    }

}
