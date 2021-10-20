package Fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.sensorsapp.BuildConfig;
import com.example.sensorsapp.R;
import com.example.sensorsapp.databinding.FragmentIndexBinding;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import Activities.Home;
import utils.SessionManagement;
import utils.VolleyListener;
import utils.VolleyOverride.JsonArrayRequest;

@SuppressWarnings("ALL")
public class Login extends Fragment implements VolleyListener {

    private FragmentIndexBinding binding;
    JsonArrayRequest jsonArrayRequest;

    RequestQueue requestQueue;
    SharedPreferences userSession;

    SharedPreferences.Editor editor;
    private int sumOfSleep = 0;
    SessionManagement session;

    @Override
    public View onCreateView (LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentIndexBinding.inflate(inflater, container, false);
        View view = binding.getRoot();

        requestQueue = Volley.newRequestQueue(getActivity());

        session = new SessionManagement(getActivity());

        binding.register.setOnClickListener(v -> {
            if(incorrectInput()) return;
            loading();
            registrationRequest();
        });

        binding.login.setOnClickListener(v -> {
            if(incorrectInput()) return;
            loading();
            loginRequest();
        });

        return view;
    }

    private void reset() {
        binding.loading.setVisibility(View.INVISIBLE);
        binding.username.setEnabled(true);
        binding.password.setEnabled(true);
        binding.login.setEnabled(true);
        binding.register.setEnabled(true);
    }

    private void loading() {
        binding.loading.setVisibility(View.VISIBLE);
        binding.username.setEnabled(false);
        binding.password.setEnabled(false);
        binding.login.setEnabled(false);
        binding.register.setEnabled(false);
    }


    private void getAverageSleepTime(JSONObject user, String jwtToken) throws JSONException {
        jsonArrayRequest = new JsonArrayRequest(Request.Method.POST,BuildConfig.SERVER_URL + "/get-week", user,
                response -> {
                    try {
                        int j = 0;
                        for (int i = 0; i < response.length(); i++) {
                            JSONObject obj = response.getJSONObject(i);
                            String duration = obj.get("duration").toString();
                            if (duration.equals("No records")) {
                                duration = "00:00:00";
                            } else {
                                duration = obj.get("duration").toString().split(":", 2)[1].trim();
                                j++;
                            }
                            String[] time = duration.split(":");
                            int hours =  Integer.parseInt(time[0]);
                            float minutes = Integer.parseInt(time[1]);
                            int seconds = Integer.parseInt(time[2])/3600;
                            float totalTime;

                            totalTime = hours + minutes/60 + seconds;
                            sumOfSleep += (int) totalTime;
                        }

                        onSuccess(sumOfSleep, user, j, jwtToken);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, (Response.ErrorListener) error -> {

        })
        {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String>  params = new HashMap<>();
                params.put("Authorization","Bearer " + jwtToken);

                return params;
            }
        }
        ;

        requestQueue.add(jsonArrayRequest);
        requestQueue = Volley.newRequestQueue(getActivity());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    public void loginRequest() {
        RequestQueue queue = Volley.newRequestQueue(getActivity());
        String url = BuildConfig.SERVER_URL + "/android-login";
        JSONObject user = constructJSONObject();
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, user,
                new Response.Listener<JSONObject>()
                {
                    @Override
                    public void onResponse(JSONObject response) {
                        System.out.println(response);
                        int HttpStatus = 0;
                        String jwtToken = null;
                        try {
                            HttpStatus = response.getInt("HttpStatus");
                            jwtToken = response.getString("jwtToken");
                        if (HttpStatus == 200){
                            getAverageSleepTime(user, jwtToken);
                            System.out.println(jwtToken);
                        } else {
                            reset();
                            binding.error.setText(R.string.wronginputs);
                            binding.error.setVisibility(View.VISIBLE);
                        }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        reset();
                        Log.d("ERROR","error => "+error.toString());
                    }
                }
        );
        queue.add(request);

    }

    public void registrationRequest(){
        JSONObject user = constructJSONObject();
        JsonObjectRequest registerRequest = new JsonObjectRequest(
                Request.Method.POST,
                BuildConfig.SERVER_URL + "/registration",
                user,
                response -> {
                    try {
                        if (response.getInt("HttpStatus") == 404) {
                            binding.error.setVisibility(View.VISIBLE);
                            binding.error.setText("Username already exists");
                            reset();
                            return;
                        }
                        session.createLoginSession(
                                user.getString("username"),
                                null,
                                response.getString("jwtToken"));
                        openHome();
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                },
                error -> {reset();});
        requestQueue.add(registerRequest);
    }

    @NotNull
    private JSONObject constructJSONObject() {
        String username = binding.username.getText().toString();
        String password = binding.password.getText().toString();

        JSONObject user =new JSONObject();
        try {
            user.put("username",username);
            user.put("password",password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return user;
    }

    @Override
    public void onSuccess(int result, JSONObject user, int j, String jwtToken) {
        try {
            String username = user.getString("username");
            if (j == 0) {
                session.createLoginSession(username, "No records yet.", jwtToken);
            } else {
                session.createLoginSession(username, "Last week's average: " + (result / 7) + " hours.", jwtToken);
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        openHome();

    }

    private void openHome() {
        Intent intent = new Intent(getActivity(), Home.class);
        startActivity(intent);
        getActivity().finish();
    }



    private boolean incorrectInput() {
        binding.error.setVisibility(View.INVISIBLE);

        if (binding.username.getText().toString().isEmpty()) {
            binding.error.setVisibility(View.VISIBLE);
            binding.error.setText(R.string.emptyError);
            return true;
        }

        if (binding.password.getText().toString().isEmpty()){
            binding.error.setVisibility(View.VISIBLE);
            binding.error.setText(R.string.emptyError);
            return true;
        }
        if (binding.username.getText().length() < 6) {
            binding.error.setVisibility(View.VISIBLE);
            binding.error.setText(R.string.username_length);
            return true;
        }

        if (binding.password.getText().length() < 6) {
            binding.error.setVisibility(View.VISIBLE);
            binding.error.setText(R.string.password_length);
            return true;
        }
        return false;
    }
}
