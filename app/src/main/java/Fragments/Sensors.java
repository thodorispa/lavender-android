package Fragments;

import android.annotation.SuppressLint;
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
import android.text.Html;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.example.sensorsapp.R;
import com.example.sensorsapp.databinding.FragmentSensorsBinding;
import com.google.android.gms.maps.model.LatLng;

import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Timer;
import java.util.TimerTask;

import utils.LocationUtils;

import static android.content.Context.SENSOR_SERVICE;

@SuppressWarnings("ALL")
public class    Sensors extends Fragment {

    private FragmentSensorsBinding binding;
    private FragmentActivity _this;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 123;

    public static SensorManager sensorManager;
    public static Sensor lightSensor;
    private Sensor accelerometer;
    public static SensorEventListener lightEventListener;
    public SensorEventListener accelerationEventListener;

    private float[] mGravity;
    private float mAccel;
    private float mAccelCurrent;
    private float mAccelLast;
    private float Luminosity;
    boolean isCharging;
    String location;
    float value;

    RequestQueue requestQueue;

    static boolean active = false;
    boolean SLEEPING;
    LocalDateTime fellAsleep,awakened;
    LocalDate fellAsleepDate;
    int  counter;

    double lat, longi;
    LatLng latLng;
    Activities.Home home;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        binding = FragmentSensorsBinding.inflate(inflater, container, false);
        _this = this.getActivity();

        requestQueue = Volley.newRequestQueue(_this);

        home = (Activities.Home) getActivity();

        sensorManager = (SensorManager) _this.getSystemService(SENSOR_SERVICE);
        lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);


        location = LocationUtils.getLocation(_this);
        if (location!=null) {
            binding.location.setText(LocationUtils.getLocation(_this));
        }

        if (lightSensor == null) {
            Toast.makeText(_this, "The device has no light sensor!", Toast.LENGTH_SHORT).show();
            _this.finish();
        }

        binding.sleep.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                if (Luminosity > 50){
                    return;
                }
                //Woke up!
                if(Integer.parseInt(String.valueOf(s)) == 190388 && SLEEPING) {
                    awakened = LocalDateTime.now();
                    SLEEPING = false;
                    binding.sleeping.setVisibility(View.INVISIBLE);
                } else if (Integer.parseInt(String.valueOf(s)) == 1) { // Started sleeping
                    fellAsleep = LocalDateTime.now();
                    fellAsleepDate =  LocalDate.now();
                }
            }
            @Override
            public void beforeTextChanged(CharSequence s, int start,
                                          int count, int after) { }
            @Override
            public void onTextChanged(CharSequence s, int start,
                                      int before, int count) { }
        });

        Timer timer = new Timer();
        TimerTask task = new TimerTask() {
            @SuppressLint("SetTextI18n")
            @Override
            public void run() {
                _this.runOnUiThread(() -> {
                    if (value > 50) {
                        return;
                    }
                    if(Math.abs(mAccel) < 3.0){
                        String sleep = binding.sleep.getText().toString();
                        counter = Integer.parseInt(sleep);
                        if (counter == 190388) {
                            counter = 1;
                        }else {
                            counter++;
                        }

                        SLEEPING = counter >= 10;
                        if (SLEEPING){
                            binding.sleeping.bringToFront();
                            binding.sleeping.setVisibility(View.VISIBLE);
                        } else {
                            binding.sleeping.setVisibility(View.INVISIBLE);
                        }
                        binding.sleep.setText(String.valueOf(counter));
                        String accel = new DecimalFormat("##.##").format(Math.abs(mAccel));
                        String ms2 = String.format(_this.getResources().getString(R.string.ms), accel);
                        binding.acceleration.setText(Html.fromHtml(ms2));
                    }else {
                        binding.sleep.setText("190388");
                    }
                });

            }
        };
        /** 1 second interval for listening **/
        timer.schedule(task, 0, 1000);

        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mAccel = 0.00f;
        mAccelCurrent = SensorManager.GRAVITY_EARTH;
        mAccelLast = SensorManager.GRAVITY_EARTH;


        /** Max value for luminosity sensor **/
        float maxValue = lightSensor.getMaximumRange();

        /** Luminosity event listener **/
        lightEventListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                if (location == "Unknown Location") {
                    location = LocationUtils.getLocation(_this);
                    binding.location.setText(LocationUtils.getLocation(_this));
                }
                IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
                Intent batteryStatus = _this.registerReceiver(null, intentFilter);
                int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);

                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
                    binding.charging.setText("Charging");
                } else if (status == BatteryManager.BATTERY_STATUS_FULL){
                    binding.charging.setText("Fully Charged");
                } else {
                    binding.charging.setText("Not Charging");
                }

                if(!active){
                    return;
                }
                value = sensorEvent.values[0];
                String formattedLum = String.format("%.02f  ", value);
                binding.luminosity.setText(formattedLum + " lux");

                // Dark Values
                if (value < 50.0) {
                    sensorManager.registerListener(accelerationEventListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
                    Luminosity = value;
                    counter = 0;
                } else {
                    sensorManager.unregisterListener(accelerationEventListener);
                    binding.sleep.setText("190388");
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) { }
        };
        /** Acceleration event listener **/
        accelerationEventListener = new SensorEventListener() {
            @RequiresApi(api = Build.VERSION_CODES.O)
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
                    mGravity = event.values.clone();
                    // Shake detection
                    float x = mGravity[0];
                    float y = mGravity[1];
                    float z = mGravity[2];
                    mAccelLast = mAccelCurrent;
                    mAccelCurrent = (float)Math.sqrt(x*x + y*y + z*z);
                    float delta = mAccelCurrent - mAccelLast;
                    mAccel = mAccel * 0.9f + delta;
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };


        return binding.getRoot();
    }


    @Override
    public void onResume() {
        super.onResume();
        sensorManager.registerListener(lightEventListener, lightSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }


    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStart() {
        super.onStart();
        active = true;
    }




}