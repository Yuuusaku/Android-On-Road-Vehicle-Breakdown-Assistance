package com.example.helpimstuck;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

public class HomeMap extends Fragment {

    private Button assist;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_home_map, container, false);



        // Create a new instance of Map_Fragment and replace the frame layout with it
        Fragment fragment = new Map_Fragment();  // Assuming you already have this fragment
        getChildFragmentManager().beginTransaction().replace(R.id.map_container, fragment).commit(); // Replace with the container view in fragment_home_map.xml

        return rootView;
    }

    // Method called when the "Call" button is clicked

}
