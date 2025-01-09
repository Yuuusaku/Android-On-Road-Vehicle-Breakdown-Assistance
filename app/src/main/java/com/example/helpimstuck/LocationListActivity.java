package com.example.helpimstuck;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.helpimstuck.R;

public class LocationListActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearme);

        // Sample locations
        String[] locations = {
                "Taguig City Justice Hall  (General Santos Ave, Lower Bicutan, Taguig, 1632 Metro Manila)",
                "Camp Bagong Diwa  (F3Q3+HQ8, Camp bagong diwa, Lower Bicutan)",
                "Central Bicutan Barangay Hall  (F3R3+RJ4, Taguig, Metro Manila)",
                "Upper Bicutan National High School  (F3R3+7H7, General Santos Ave, Taguig, Metro Manila)",
                "Department of Public Works and Highways  (Daisy, Lower Bicutan, Taguig, Metro Manila)",
                "Food and Nutrition Research Institute  (DOST Compound Saliksik St, General Santos Ave, Taguig, 1630 Metro Manila)",
                "CSC BANAWE  (F3R3+2G8, Taguig, Metro Manila)",
                "Environment and Biotechnology Division  (DOST Compound, Tuklas St, Taguig, Kalakhang Maynila)",
                "Kuya Odjie's School Supplies  (F3V3+3V8, Cristobal St, Taguig, 1630 Metro Manila)"
        };

        // Set up the adapter
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                android.R.id.text1,
                locations
        );

        // Set the adapter to the ListView
        ListView listView = findViewById(R.id.listViewLocations);
        listView.setAdapter(adapter);
    }
}
