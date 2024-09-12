package com.xpmodder.spritpreise.shared;

import android.content.Context;
import android.location.Location;
import com.xpmodder.spritpreise.shared.html.HTMLParser;

import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

public final class Data {

    public static String place = "Schortens";

    public static int gasIndex = 6;
    public static int radiusIndex = 2;

    public static ArrayList<Tankstelle> tankstellen = new ArrayList<>();

    public static Location currentLocation = null;

    public static void refresh(Context context, Runnable toDoAfter){
        Data.tankstellen.clear();

        CompletableFuture.runAsync(()-> HTMLParser.getAndParse(getRequestURL(context))).thenRun(toDoAfter);
    }

    public static String getRequestURL(Context context){

        int[] gasTypes = context.getResources().getIntArray(R.array.gasTypes);
        String[] radii = context.getResources().getStringArray(R.array.radiusSelection);
        String baseURL = context.getResources().getString(R.string.base_request_url);

        int radius = Integer.parseInt(radii[radiusIndex].replace("km", "").trim());

        if (currentLocation != null) {
            return baseURL + "?lat=" + currentLocation.getLatitude() + "&lon=" + currentLocation.getLongitude() + "&spritsorte=" + gasTypes[gasIndex] + "&r=" + radius;
        }
        else{
            return baseURL + "?ort=" + place + "&spritsorte=" + gasTypes[gasIndex] + "&r=" + radius;
        }
    }

}
