package Fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.fragment.app.Fragment;

import com.example.sensorsapp.R;
import com.example.sensorsapp.databinding.FragmentHomeBinding;
import com.google.android.material.navigation.NavigationView;

public class Home extends Fragment {

    private FragmentHomeBinding binding;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);

        NavigationView navigationView = getActivity().findViewById(R.id.nav_view);
        FrameLayout fragmentPlaceholder = getActivity().findViewById(R.id.fragment_placeholder);

        binding.sensors.setOnClickListener(v -> {
            navigationView.getMenu().getItem(1).setChecked(true);
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(R.anim.enter_left_to_right,R.anim.out_right,0 ,0)
                        .replace(fragmentPlaceholder.getId(), new Sensors()).commit();
        });

        binding.statistics.setOnClickListener(v -> {
            navigationView.getMenu().getItem(2).setChecked(true);
            getActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(R.anim.enter_left_to_right,R.anim.out_right,0 ,0)
                    .replace(fragmentPlaceholder.getId(), new Statistics()).commit();
        });


        return binding.getRoot();
    }
}