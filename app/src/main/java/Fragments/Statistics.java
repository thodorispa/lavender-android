package Fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.example.sensorsapp.BuildConfig;
import com.example.sensorsapp.R;
import com.example.sensorsapp.databinding.FragmentStatisticsBinding;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.LimitLine;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.google.android.material.navigation.NavigationView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import utils.SessionManagement;
import utils.VolleyOverride.JsonArrayRequest;


public class Statistics extends Fragment{

    private FragmentStatisticsBinding binding;
    String username;

    RequestQueue requestQueue;
    private static final String SET_LABEL = "Sleeping Time";
    private static final String[] DAYS = new String[8];

    BarChart barChart;
    BarData data = new BarData();

    FragmentActivity _this;
    Activities.Home home;

    String jwtToken;
    SessionManagement session;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentStatisticsBinding.inflate(inflater, container, false);
        _this = getActivity();
        home = (Activities.Home) getActivity();

        Calendar cal = Calendar.getInstance();
        int currentDay = cal.get(Calendar.DAY_OF_MONTH);
        int currentMonth = cal.get(Calendar.MONTH);

        binding.sDays.setSelection(currentDay-1);
        binding.sMonths.setSelection(currentMonth);
        binding.sYears.setSelection(2);

        session = new SessionManagement(_this);

        HashMap<String, String> userData = session.getUserDetails();
        jwtToken = userData.get("jwtToken");

        NavigationView navigationView =_this.findViewById(R.id.nav_view);

        View headerView = navigationView.getHeaderView(0);
        TextView headerUsername = headerView.findViewById(R.id.headerEmail);

        configureChartAppearance();

        username = (String) headerUsername.getText();

        binding.clearResults.setOnClickListener(v -> {
            binding.results.setVisibility(View.GONE);
            binding.barChart.setVisibility(View.GONE);
            binding.clearResults.setVisibility(View.INVISIBLE);

        });

        binding.search.setOnClickListener(v -> {
            binding.clearResults.setVisibility(View.VISIBLE);
            binding.results.setVisibility(View.VISIBLE);
            binding.barChart.setVisibility(View.GONE);
            JsonObjectRequest jsonObjectRequest;
            StringBuilder sb = new StringBuilder();
            String dd = binding.sDays.getSelectedItem().toString();
            String mm = binding.sMonths.getSelectedItem().toString();
            String YYYY = binding.sYears.getSelectedItem().toString();
            String date = YYYY+"-"+mm+"-"+dd;

            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("username", username);
                requestBody.put("date",date);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            jsonObjectRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    BuildConfig.SERVER_URL + "/get",
                    requestBody ,
                    response -> {
                        if(response.length() < 2) {
                            binding.results.setText(R.string.not_found);
                            return;
                        }
                        for (int i = 1; i <= (response.length())/2 ; i++) {
                            try {
                                sb.append("Session: " + response.get("Session"+ i) +
                                          "---> Duration: " + response.get("Duration" + i)+"\n");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        binding.results.setText(sb);
                    },
                    error -> {
                        System.out.println(error.getMessage());
                    })
            {
                @Override
                public Map<String, String> getHeaders() {
                    Map<String, String>  params = new HashMap<>();
                    params.put("Authorization","Bearer " + jwtToken);

                    return params;
                }
            };
            requestQueue.add(jsonObjectRequest);
        });

        binding.searchLastWeek.setOnClickListener(v -> {
            binding.clearResults.setVisibility(View.VISIBLE);
            binding.results.setVisibility(View.GONE);
            binding.barChart.setVisibility(View.VISIBLE);
            JSONObject requestBody = new JSONObject();
            try {
                requestBody.put("username", username);
            } catch (JSONException e) {
                e.printStackTrace();
            }

          JsonArrayRequest jsonArrayRequest = new JsonArrayRequest(Request.Method.POST,BuildConfig.SERVER_URL + "/get-week", requestBody ,
                    response -> {
                        try {
                            ArrayList<BarEntry> values = new ArrayList<>();
                            for (int i = 0; i < response.length(); i++) {
                                JSONObject obj = response.getJSONObject(i);
                                String day = (String) obj.get("title");
                                String duration = obj.get("duration").toString();
                                if (duration.equals("No records")) {
                                    duration = "00:00:00";
                                } else {
                                    duration = obj.get("duration").toString().split(":", 2)[1].trim();
                                }
                                String[] time = duration.split(":");
                                int hours =  Integer.parseInt(time[0]);
                                float minutes = Integer.parseInt(time[1]);
                                int seconds = Integer.parseInt(time[2])/3600;
                                float totalTime;

                                totalTime = hours + minutes/60 + seconds;

                                DAYS[i] = day;

                                int barIndex = Arrays.asList(DAYS).indexOf(day);
                                values.add(new BarEntry(barIndex, totalTime));
                            }

                            BarDataSet dataset = new BarDataSet(values, "First");
                            dataset.setColor(Color.WHITE);

                            data = createChartData(values);
                            prepareChartData(data);

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }, error -> {
              System.out.println(error.getMessage());
          })
          {
              @Override
              public Map<String, String> getHeaders() {
                  Map<String, String>  params = new HashMap<String, String>();
                  params.put("Authorization","Bearer " + jwtToken);
                  return params;
              }
          };

            requestQueue.add(jsonArrayRequest);

            data.setValueTextSize(12f);
            barChart.setData(data);
            barChart.invalidate();
        });

        requestQueue = Volley.newRequestQueue(_this);

        return binding.getRoot();
    }

    private void prepareChartData(BarData data) {
        data.setValueTextSize(12f);
        barChart.setData(data);
        barChart.setScaleEnabled(false);
        barChart.invalidate();
    }

    private void configureChartAppearance() {
        barChart = binding.barChart;
        barChart.getDescription().setEnabled(false);
        barChart.setDrawValueAboveBar(true);
        barChart.getLegend().setTextColor(Color.WHITE);
        XAxis xAxis = barChart.getXAxis();
        xAxis.setTextColor(Color.WHITE);
        xAxis.setGridColor(Color.WHITE);
        xAxis.setAxisLineColor(Color.WHITE);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return DAYS[(int) value];
            }
        });

        YAxis axisLeft = barChart.getAxisLeft();
        axisLeft.setGranularity(10f);
        axisLeft.setAxisMinimum(0);
        axisLeft.setAxisMaximum(24);
        axisLeft.setLabelCount(24);
        axisLeft.setGridColor(Color.WHITE);
        axisLeft.setTextColor(Color.WHITE);

        YAxis axisRight = barChart.getAxisRight();
        axisRight.setGranularity(10f);
        axisRight.setAxisMinimum(0);
        axisRight.setAxisMaximum(24);
        axisRight.setGridColor(Color.WHITE);
        axisRight.setTextColor(Color.WHITE);
        barChart.getAxisLeft().setDrawGridLines(false);
        barChart.getXAxis().setDrawGridLines(false);

    }

    private BarData createChartData(ArrayList<BarEntry> values) {
        BarEntry entry;
        int sum = 0;
        for (int i = 0; i < values.size(); i++) {
            entry = values.get(i);
            sum += entry.getY();
        }

        int avg = sum/values.size();
        barChart.getAxisLeft().removeAllLimitLines();
        LimitLine avgLine = new LimitLine(avg, "Avg. Sleeping Time");
        avgLine.setLineColor(Color.WHITE);
        avgLine.setTextColor(Color.WHITE);
        barChart.getAxisLeft().addLimitLine(avgLine);
        barChart.setBorderColor(Color.WHITE);


        BarDataSet set1 = new BarDataSet(values, SET_LABEL);
        int purple = Color.rgb(131, 88, 207);
        set1.setColor(purple);

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();
        dataSets.add(set1);

        BarData data = new BarData(dataSets);
        data.setValueTextColor(Color.WHITE);
        return data;
    }

}