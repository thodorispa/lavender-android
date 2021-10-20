package Activities;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentTransaction;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.sensorsapp.BuildConfig;
import com.example.sensorsapp.R;
import com.example.sensorsapp.databinding.ActivityHomeBinding;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import Fragments.Sensors;
import Fragments.Statistics;
import utils.LocationUtils;
import utils.SessionManagement;

public class Home extends AppCompatActivity {

    private ActivityHomeBinding binding;

    private String username, avgSleep;
    private String jwtToken;

    SessionManagement session;

    DrawerLayout drawer;

    public static SensorManager sensorManager;
    public static Sensor lightSensor;
    private Sensor accelerometer;
    public static SensorEventListener lightEventListener;
    public SensorEventListener accelerationEventListener;

    private float[] mGravity;
    private float mAccel ,mAccelCurrent, mAccelLast, Luminosity;
    float value;
    boolean isCharging;
    RequestQueue requestQueue;

    static boolean active = false;
    boolean SLEEPING;
    LocalDateTime fellAsleep, awakened;
    LocalDate fellAsleepDate;
    int counter;

    @SuppressLint("NonConstantResourceId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHomeBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);
        session = new SessionManagement(this);
        session.checkLogin();

        requestQueue = Volley.newRequestQueue(this);


        HashMap<String, String> userData = session.getUserDetails();
        username = userData.get("username");
        avgSleep = userData.get("avgSleep");
        jwtToken = userData.get("jwtToken");

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        NavigationView navigationView = findViewById(R.id.nav_view);

        View headerView = navigationView.getHeaderView(0);
        TextView avgSleepText = headerView.findViewById(R.id.avgSleep);
        TextView headerEmail = headerView.findViewById(R.id.headerEmail);
        avgSleepText.setText(avgSleep);
        headerEmail.setText(username);

        navigationView.setNavigationItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.nav_home:
                    navigationView.getMenu().getItem(0).setChecked(true);
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.enter_left_to_right, R.anim.out_right, 0, 0)
                            .replace(binding.fragmentPlaceholder.getId(), new Fragments.Home()).commit();
                    break;
                case R.id.nav_sensors:
                    navigationView.getMenu().getItem(1).setChecked(true);
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.enter_left_to_right, R.anim.out_right, 0, 0)
                            .replace(binding.fragmentPlaceholder.getId(), new Sensors()).commit();
                    break;
                case R.id.nav_data:
                    navigationView.getMenu().getItem(2).setChecked(true);
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setCustomAnimations(R.anim.enter_left_to_right, R.anim.out_right, 0, 0)
                            .replace(binding.fragmentPlaceholder.getId(), new Statistics()).commit();
                    break;
                case R.id.nav_logout:
                    logoutDialog();
                    break;
            }
            drawer.closeDrawer(GravityCompat.START);
            return true;
        });
        navigationView.getMenu().getItem(0).setChecked(true);

        // Set up Drawer Menu
        drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);

        if (lightSensor == null) {
            Toast.makeText(this, "The device has no light sensor!", Toast.LENGTH_SHORT).show();
            this.finish();
        }

        binding.sleep.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
//                if (Luminosity > 50.0) {
//                    return;
//                }
                if (Integer.parseInt(String.valueOf(s)) == 190388 && SLEEPING) { //Woke up!
                    awakened = LocalDateTime.now();
                    recordData(mAccel, Luminosity, fellAsleepDate, fellAsleep, awakened);
                    SLEEPING = false;
                } else if (Integer.parseInt(String.valueOf(s)) == 1) { // Started sleeping
                    fellAsleep = LocalDateTime.now();
                    fellAsleepDate = LocalDate.now();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) {
            }
        });

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                runOnUiThread(() -> {
                    if (value > 50) {
                        return;
                    }
                    if (Math.abs(mAccel) < 3.0) {
                        String sleep = binding.sleep.getText().toString();
                        counter = Integer.parseInt(sleep);
                        if (counter == 190388) {
                            counter = 1;
                        } else {
                            counter++;
                        }

                        // Sleeping for 10 seconds
                        SLEEPING = counter >= 10;
                        binding.sleep.setText(String.valueOf(counter));
                    } else {
                        binding.sleep.setText("190388");
                    }
                });

            }
        };
        /* 1 second interval for listening **/
        timer.schedule(task, 0, 1000);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;

        /* Luminosity event listener **/
        lightEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                value = sensorEvent.values[0];
                /* Get battery state **/
                IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = getApplicationContext().registerReceiver(null, intentFilter);
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL;
                // Dark Surroundings
                if (value < 50) {
                    sensorManager.registerListener(accelerationEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
                    Luminosity = value;
                    counter = 0;
                } else {
                    sensorManager.unregisterListener(accelerationEventListener);
                    // Το TextView αυτό δεν είναι ορατό στον χρήστη
                    // Χρησιμοποιείται σαν counter και εδώ δίνεται μια "άκυρη τιμή"
                    binding.sleep.setText("190388");
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {
            }
        };
        /* Acceleration event listener **/
        accelerationEventListener = new SensorEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                    mGravity = event.values.clone();
                    // Shake detection
                    float x = mGravity[0];
                    float y = mGravity[1];
                    float z = mGravity[2];
                    mAccelLast = mAccelCurrent;
                    mAccelCurrent = (float) Math.sqrt(x * x + y * y + z * z);
                    float delta = mAccelCurrent - mAccelLast;
                    mAccel = mAccel * 0.9f + delta;
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        ft.replace(binding.fragmentPlaceholder.getId(), new Fragments.Home());
        ft.commit();
    }

    private void recordData(float mAccel, float luminosity, LocalDate now, LocalDateTime fellAsleep, LocalDateTime awakened) {
        /* Get current or last known location **/
        String location = LocationUtils.getLocation(this);
        JSONObject sleepSession = new JSONObject();
        try {
            sleepSession.put("username", username);
            sleepSession.put("luminosity", luminosity);
            sleepSession.put("acceleration", mAccel);
            sleepSession.put("date", now);
            sleepSession.put("fellAsleep", fellAsleep);
            sleepSession.put("awakened", awakened);
            sleepSession.put("isCharging", isCharging);
            sleepSession.put("location", location);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        JsonObjectRequest jObject = new JsonObjectRequest(Request.Method.POST, BuildConfig.SERVER_URL + "/upload", sleepSession,
                jsonObject ->
                        Toast.makeText(this, "Recorded Successfully", Toast.LENGTH_SHORT).show(),
                System.out::println) {
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Authorization", "Bearer " + jwtToken);

                return params;
            }
        };
        requestQueue.add(jObject);
    }

    public void logoutDialog() {
        DialogInterface.OnClickListener dialogClickListener = (dialog, which) -> {
            switch (which) {
                case DialogInterface.BUTTON_POSITIVE:
                    session.logoutUser();
                    sensorManager.unregisterListener(lightEventListener);
                    Intent intent = new Intent(this, MainActivity.class);

                    startActivity(intent);
                    this.finish();
                    break;
                case DialogInterface.BUTTON_NEGATIVE:
                    //"No" button clicked
                    break;
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this, R.style.DialogTheme);
        builder.setMessage("Are you sure you want to logout?").setPositiveButton("Yes", dialogClickListener)
                .setNegativeButton("No", dialogClickListener).show();
    }

    @Override
    public void onStart() {
        super.onStart();
        sensorManager.registerListener(lightEventListener, lightSensor, SensorManager.SENSOR_DELAY_FASTEST);
        active = true;
    }
}

