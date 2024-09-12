package com.xpmodder.spritpreise.shared;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.car.app.*;
import androidx.car.app.model.*;
import androidx.car.app.validation.HostValidator;
import androidx.core.graphics.drawable.IconCompat;
import androidx.lifecycle.DefaultLifecycleObserver;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationToken;
import com.google.android.gms.tasks.OnTokenCanceledListener;
import com.xpmodder.spritpreise.shared.html.StringUtils;

import java.util.List;


public final class MyCarAppService extends CarAppService{


    @SuppressLint("PrivateResource")
    @NonNull
    @Override
    public HostValidator createHostValidator() {
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            return HostValidator.ALLOW_ALL_HOSTS_VALIDATOR;
        } else {
            return new HostValidator.Builder(getApplicationContext())
                    .addAllowedHosts(androidx.car.app.R.array.hosts_allowlist_sample)
                    .build();
        }
    }

    @NonNull
    @Override
    public Session onCreateSession() {
        return new Session() {
            @NonNull
            @Override
            public Screen onCreateScreen(@NonNull Intent intent) {
                return new PlaceListScreen(getCarContext());
            }
        };
    }


    public static class PlaceListScreen extends Screen implements DefaultLifecycleObserver {

        private void refresh(){
            Data.tankstellen.clear();
            invalidate();
            Data.refresh(getCarContext(), this::invalidate);
        }

        private void getCurrentLocation(){

            Log.i("Debug", "Getting Current Location!");

            if(this.getCarContext().checkSelfPermission("android.permission.ACCESS_FINE_LOCATION") != PackageManager.PERMISSION_GRANTED || this.getCarContext().checkSelfPermission("android.permission.ACCESS_COARSE_LOCATION") != PackageManager.PERMISSION_GRANTED || this.getCarContext().checkSelfPermission("androidx.car.app.MAP_TEMPLATES") != PackageManager.PERMISSION_GRANTED){
                this.getCarContext().requestPermissions(List.of("android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "androidx.car.app.MAP_TEMPLATES"), (list, list1) -> {
                    Log.i("Debug", "Allowed perms: " + list);
                    if (list.contains("android.permission.ACCESS_FINE_LOCATION") && list.contains("android.permission.ACCESS_COARSE_LOCATION")){

                        FusedLocationProviderClient locationProviderClient;

                        locationProviderClient = LocationServices.getFusedLocationProviderClient(this.getCarContext());

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

                        locationProviderClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationToken).addOnSuccessListener( location -> {
                            Data.currentLocation = location;
                        });

                    }
                });
            }
            else{

                FusedLocationProviderClient locationProviderClient;

                locationProviderClient = LocationServices.getFusedLocationProviderClient(this.getCarContext());

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

                locationProviderClient.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cancellationToken).addOnSuccessListener( location -> {
                    Data.currentLocation = location;
                });

            }

        }

        private Place getLocation(Tankstelle tankstelle){

            CarIcon icon = new CarIcon.Builder(IconCompat.createWithResource(getCarContext(), R.drawable.marker_icon)).build();
            PlaceMarker marker = new PlaceMarker.Builder().setIcon(icon, PlaceMarker.TYPE_IMAGE).build();

            try {

                Geocoder geocoder = new Geocoder(getCarContext());

                List<Address> addresses = geocoder.getFromLocationName(tankstelle.name + " " + tankstelle.address + ", " + tankstelle.city, 1);

                double lat = addresses.get(0).getLatitude();
                double lon = addresses.get(0).getLongitude();

                CarLocation location = CarLocation.create(lat, lon);

                return new Place.Builder(location).setMarker(marker).build();

            }
            catch (Exception ex){
                Log.i("Debug", "Failed to get location for place!");
                CarLocation location = CarLocation.create(0, 0);
                return new Place.Builder(location).setMarker(marker).build();
            }

        }

        protected PlaceListScreen(@NonNull CarContext carContext) {
            super(carContext);
            getLifecycle().addObserver(this);
            Data.refresh(getCarContext(), this::invalidate);
            getCurrentLocation();
        }

        @NonNull
        @Override
        public Template onGetTemplate() {

            ItemList.Builder listBuilder = new ItemList.Builder();
            getCurrentLocation();

            for(Tankstelle tankstelle : Data.tankstellen){

                Place place = getLocation(tankstelle);

                Row.Builder builder = new Row.Builder();
                builder.setTitle(StringUtils.ScriptsHtmlToString(tankstelle.price));
                builder.addText(tankstelle.name);
                builder.addText(tankstelle.address + " " + tankstelle.city);
                builder.setMetadata(new Metadata.Builder().setPlace(place).build());
                builder.setBrowsable(true);
                builder.setOnClickListener(() -> {
                    Intent intent = new Intent(CarContext.ACTION_NAVIGATE, Uri.parse("geo:0,0?q=" + Uri.encode(tankstelle.name + " " + tankstelle.address + ", " + tankstelle.city)));
                    getCarContext().startCarApp(intent);
                });
                listBuilder.addItem(builder.build());

            }

            Row.Builder builder = new Row.Builder();
            builder.setTitle(getCarContext().getText(R.string.refresh_title));
            builder.addText(getCarContext().getText(R.string.refresh_line));
            builder.setBrowsable(true);
            builder.setOnClickListener(this::refresh);
            listBuilder.addItem(builder.build());


            ActionStrip.Builder actionStripBuilder = new ActionStrip.Builder();

            String[] strings = getCarContext().getResources().getStringArray(R.array.gasSelection);
            Action.Builder actionBuilder = new Action.Builder();
            actionBuilder.setTitle(strings[Data.gasIndex]);
            actionBuilder.setOnClickListener(() -> {
                Data.gasIndex++;
                if(Data.gasIndex >= strings.length){
                    Data.gasIndex = 0;
                }
                refresh();
            });

            actionStripBuilder.addAction(actionBuilder.build());


            String[] strings2 = getCarContext().getResources().getStringArray(R.array.radiusSelection);
            actionBuilder = new Action.Builder();
            actionBuilder.setTitle("Radius: " + strings2[Data.radiusIndex]);
            actionBuilder.setOnClickListener(() -> {
                Data.radiusIndex++;
                if(Data.radiusIndex >= strings2.length){
                    Data.radiusIndex = 0;
                }
                refresh();
            });

            actionStripBuilder.addAction(actionBuilder.build());


            PlaceListMapTemplate.Builder mapBuilder = new PlaceListMapTemplate.Builder();
            mapBuilder.setTitle(getCarContext().getText(R.string.stations_title));
            mapBuilder.setHeaderAction(Action.APP_ICON);
            mapBuilder.setCurrentLocationEnabled(true);
            if (Data.currentLocation != null) {
                mapBuilder.setAnchor(new Place.Builder(CarLocation.create(Data.currentLocation)).build());
            }
            mapBuilder.setActionStrip(actionStripBuilder.build());

            if(Data.tankstellen.isEmpty()){
                return mapBuilder.setLoading(true).build();
            }
            else{
                mapBuilder.setItemList( new ItemList.Builder().build());
                mapBuilder.setItemList(listBuilder.build());
                return mapBuilder.build();
            }

        }
    }

}
