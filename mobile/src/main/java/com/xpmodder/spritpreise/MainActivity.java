package com.xpmodder.spritpreise;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.google.android.material.textfield.TextInputLayout;
import com.xpmodder.spritpreise.shared.Data;
import com.xpmodder.spritpreise.shared.Tankstelle;

import java.util.Objects;

import static android.text.Html.FROM_HTML_MODE_LEGACY;

/*
Spritsorten: (spritsorte)
    1: LPG
    2: LKW-Diesel
    3: Diesel
    4: Bioethanol
    5: Super E1ß
    6: SuperPlus
    7: Super E5
    8: CNG

    12: Premium Diesel
    13: AdBlue LKW

    246: Wasserstoff

    262: LNG

    264: GTL-Diesel

    266: AdBlue PKW

Radius: (r)
    1
    2
    5
    10
    15
    20
    25

Ort: (ort)
    PLZ+Name (26419+Schortens)
    PLZ (26419)
    Name (Schortens)

Koordinaten: (lat, long)

 */



public class MainActivity extends AppCompatActivity {

    SwipeRefreshLayout.OnRefreshListener refreshListener;

    private void closeMenu(Location location){
        LinearLayout menuLayout = findViewById(R.id.menuLayout);
        menuLayout.setVisibility(View.INVISIBLE);

        TextView settings = findViewById(R.id.settingsSummary);
        settings.setVisibility(View.VISIBLE);

        ConstraintLayout constraintLayout = findViewById(R.id.constraintLayout);
        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        set.connect(R.id.mainVerticalLayout, ConstraintSet.TOP, R.id.toolbar, ConstraintSet.BOTTOM);
        set.applyTo(constraintLayout);

        TextInputLayout cityInput = findViewById(R.id.cityInput);

        Spinner radiusSpinner = findViewById(R.id.radiusSpinner);

        Spinner gasSpinner = findViewById(R.id.gasSpinner);

        String cityString = Objects.requireNonNull(cityInput.getEditText()).getText().toString();

        if (location != null){
            cityString = location.getLatitude() + " N, " + location.getLongitude() + " O";
        }

        String radius = radiusSpinner.getSelectedItem().toString();

        String selected = gasSpinner.getSelectedItem().toString();

        Resources res = getResources();

        settings.setText(res.getString(R.string.settingsSummary, selected, cityString, radius));

    }

    private void openMenu(){
        LinearLayout menuLayout = findViewById(R.id.menuLayout);
        menuLayout.setVisibility(View.VISIBLE);

        TextView settings = findViewById(R.id.settingsSummary);
        settings.setVisibility(View.INVISIBLE);

        ConstraintLayout constraintLayout = findViewById(R.id.constraintLayout);
        ConstraintSet set = new ConstraintSet();
        set.clone(constraintLayout);
        set.connect(R.id.mainVerticalLayout, ConstraintSet.TOP, R.id.menuLayout, ConstraintSet.BOTTOM);
        set.applyTo(constraintLayout);
    }


    private void loadPreferences(){

        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        Resources res = getResources();

        TextInputLayout cityInput = findViewById(R.id.cityInput);
        Objects.requireNonNull(cityInput.getEditText()).setText(preferences.getString(res.getString(R.string.city_key), res.getString(R.string.city_default)));

        Spinner radiusSpinner = findViewById(R.id.radiusSpinner);
        radiusSpinner.setSelection(preferences.getInt(res.getString(R.string.radius_key), res.getInteger(R.integer.radius_default)));

        Spinner gasSpinner = findViewById(R.id.gasSpinner);
        gasSpinner.setSelection(preferences.getInt(res.getString(R.string.fuel_key), res.getInteger(R.integer.fuel_default)));

        Button searchButton = findViewById(R.id.confirmButton);
        searchButton.callOnClick();
    }


    private void savePreferences(){

        SharedPreferences preferences = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Resources res = getResources();

        TextInputLayout cityInput = findViewById(R.id.cityInput);
        editor.putString(res.getString(R.string.city_key), Objects.requireNonNull(cityInput.getEditText()).getText().toString());

        Spinner radiusSpinner = findViewById(R.id.radiusSpinner);
        editor.putInt(res.getString(R.string.radius_key), radiusSpinner.getSelectedItemPosition());

        Spinner gasSpinner = findViewById(R.id.gasSpinner);
        editor.putInt(res.getString(R.string.fuel_key), gasSpinner.getSelectedItemPosition());

        editor.apply();

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityResultLauncher<String> permissionRequestLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if(!isGranted){
                AlertDialog.Builder alertBuilder = new AlertDialog.Builder(this);
                alertBuilder.setMessage(getText(R.string.permission_info));
                alertBuilder.setTitle(getTitle());
                alertBuilder.setPositiveButton("Ok", (dialog, which) -> finish());
                alertBuilder.create().show();
            }
        });

        if(checkSelfPermission("android.permission.INTERNET") != PackageManager.PERMISSION_GRANTED){
            permissionRequestLauncher.launch("android.permission.INTERNET");
        }

        Toolbar myToolbar = findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);

        myToolbar.setNavigationOnClickListener( view -> {
            LinearLayout menuLayout = findViewById(R.id.menuLayout);
            if(menuLayout.getVisibility() == View.INVISIBLE) {
                openMenu();
            }
            else{
                closeMenu(null);
            }
        });

        TextInputLayout cityInput = findViewById(R.id.cityInput);

        Spinner radiusSpinner = findViewById(R.id.radiusSpinner);

        Spinner gasSpinner = findViewById(R.id.gasSpinner);

        Button searchButton = findViewById(R.id.confirmButton);
        searchButton.setOnClickListener( view -> {

            Data.place = Objects.requireNonNull(cityInput.getEditText()).getText().toString().trim();
            Data.gasIndex = gasSpinner.getSelectedItemPosition();
            Data.radiusIndex = radiusSpinner.getSelectedItemPosition();
            Data.currentLocation = null;

            SwipeRefreshLayout refreshLayout = findViewById(R.id.swiperefresh);
            refreshLayout.post(() -> {refreshLayout.setRefreshing(true); refreshListener.onRefresh();});

            closeMenu(null);

        });


        SwipeRefreshLayout refreshLayout = findViewById(R.id.swiperefresh);

        refreshListener = () -> {
            LinearLayout layout = findViewById(R.id.scrollVerticalLayout);
            layout.removeAllViews();

            Data.refresh(this, ()->
                    runOnUiThread(() -> {
                        updateViews();
                        refreshLayout.setRefreshing(false);
                    }));
        };

        refreshLayout.setOnRefreshListener(refreshListener);

        loadPreferences();

        closeMenu(null);

        if(checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED){
            permissionRequestLauncher.launch("android.permission.ACCESS_FINE_LOCATION");
        }
        if(checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED){
            permissionRequestLauncher.launch("android.permission.ACCESS_COARSE_LOCATION");
        }
        if(checkSelfPermission("androidx.car.app.MAP_TEMPLATES") != PackageManager.PERMISSION_GRANTED){
            permissionRequestLauncher.launch("androidx.car.app.MAP_TEMPLATES");
        }

        FusedLocationProviderClient locationProviderClient;

        locationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        CancellationToken cancellationToken = new CancellationToken() {
            @NonNull
            @Override
            public CancellationToken onCanceledRequested(@NonNull OnTokenCanceledListener onTokenCanceledListener) {
                return null;
            }

            @Override
            public boolean isCancellationRequested() {
                return false;
            }
        };

        ImageButton locationButton = findViewById(R.id.locationButton);
        locationButton.setOnClickListener( view -> {

            locationProviderClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationToken).addOnSuccessListener( location -> {

                Data.gasIndex = gasSpinner.getSelectedItemPosition();
                Data.radiusIndex = radiusSpinner.getSelectedItemPosition();
                Data.currentLocation = location;

                refreshLayout.post(() -> {refreshLayout.setRefreshing(true); refreshListener.onRefresh();});

                closeMenu(location);

            });

        });


    }


    private void invokeMaps(String name, String address, String city){
        Uri gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(name + " " + address + ", " + city));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }


    private void updateViews(){

        LinearLayout scrollLayout = findViewById(R.id.scrollVerticalLayout);

        if(Data.tankstellen.size() < 1){
            return;
        }

        for(Tankstelle tankstelle : Data.tankstellen){

            LinearLayout horizontal = new LinearLayout(MainActivity.this);
            horizontal.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            horizontal.setOrientation(LinearLayout.HORIZONTAL);
            horizontal.setOnClickListener( view -> invokeMaps(tankstelle.name, tankstelle.address, tankstelle.city));

            Space space1 = new Space(MainActivity.this);
            space1.setLayoutParams(new LinearLayout.LayoutParams(10, LinearLayout.LayoutParams.MATCH_PARENT));

            horizontal.addView(space1);

            TextView price = new TextView(MainActivity.this);
            price.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            price.setText(Html.fromHtml(tankstelle.price, FROM_HTML_MODE_LEGACY), TextView.BufferType.SPANNABLE);
            price.setTextSize(40);
            price.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

            horizontal.addView(price);

            Space space2 = new Space(MainActivity.this);
            space2.setLayoutParams(new LinearLayout.LayoutParams(20, LinearLayout.LayoutParams.MATCH_PARENT));

            horizontal.addView(space2);

            LinearLayout vertical = new LinearLayout(MainActivity.this);
            vertical.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT, 2));
            vertical.setOrientation(LinearLayout.VERTICAL);

            horizontal.addView(vertical);

            TextView name = new TextView(MainActivity.this);
            name.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            name.setText(tankstelle.name);
            name.setTextSize(25);
            name.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);

            vertical.addView(name);

            LinearLayout horizontal2 = new LinearLayout(MainActivity.this);
            horizontal2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            horizontal2.setOrientation(LinearLayout.HORIZONTAL);

            vertical.addView(horizontal2);

            LinearLayout vertical2 = new LinearLayout(MainActivity.this);
            vertical2.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            vertical2.setOrientation(LinearLayout.VERTICAL);

            horizontal2.addView(vertical2);

            TextView distance = new TextView(MainActivity.this);
            distance.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            distance.setText(tankstelle.distance);
            distance.setTextSize(20);
            distance.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_END);

            horizontal2.addView(distance);

            TextView address = new TextView(MainActivity.this);
            address.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            address.setText(tankstelle.address);
            address.setTextSize(15);
            address.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

            vertical2.addView(address);

            TextView city = new TextView(MainActivity.this);
            city.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            city.setText(tankstelle.city);
            city.setTextSize(15);
            city.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);

            vertical2.addView(city);

            scrollLayout.addView(horizontal);

            View divider = new View(MainActivity.this);
            divider.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 4));
            divider.setBackgroundColor(ContextCompat.getColor(this, R.color.purple_200));

            scrollLayout.addView(divider);

        }

    }


    @Override
    protected void onStop() {
        savePreferences();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        savePreferences();
        super.onDestroy();
    }

}