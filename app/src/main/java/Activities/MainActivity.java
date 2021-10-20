package Activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentTransaction;

import com.example.sensorsapp.databinding.ActivityMainBinding;

import java.util.Date;

import Fragments.Login;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    SharedPreferences userSession;

    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 123 ;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        View view = binding.getRoot();
        setContentView(view);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);
            requestPermissions(new String[]{ android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);
        }

        userSession = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE);
        String sessionUsername = userSession.getString("username", null);
        int sessionExpirationDate = userSession.getInt("expires_at", 0);
        int nowDate = (int) (new Date().getTime()/1000);
        boolean isExpired = sessionExpirationDate > 0 && (nowDate - sessionExpirationDate) > 60*60*24;

        if (sessionUsername != null && !isExpired ) {
            // TODO: 26/6/21 SERVER CHECK IF user EXISTS
            Intent intent = new Intent(this, Home.class);
            startActivity(intent);
            this.finish();
        } else {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(binding.fragmentPlaceholder.getId(), new Login());
            ft.commit();
        }

    }


}