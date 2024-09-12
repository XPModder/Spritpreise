package com.xpmodder.spritpreise.shared.html;

import android.util.Log;
import com.xpmodder.spritpreise.shared.Data;
import com.xpmodder.spritpreise.shared.Tankstelle;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HTMLParser {


    public static void getAndParse(String url){

        try{

            Log.i("HTMLParser", "Attempting to get Data...");

            URLConnection connection = new URL(url).openConnection();
            connection.setRequestProperty("Accept-Charset", "UTF-8");
            InputStream response = connection.getInputStream();
            parseHTMLStream(response);

        }catch(Exception ex){
            Log.e("HTMLParser", ex.fillInStackTrace().toString());
        }

    }


    public static void parseHTMLStream(InputStream stream){

        try{

            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));

            ArrayList<String> tags = new ArrayList<>();
            StringBuilder tag;

            String line = reader.readLine();

            while(line != null){

                if(line.contains("<a href=\"/tankstelle_details/")){
                    tag = new StringBuilder();

                    while(true){
                        line = reader.readLine();

                        if(line.contains("</a>")){
                            tags.add(tag.toString());
                            break;
                        }

                        tag.append(line);
                    }

                }

                line = reader.readLine();

            }

            for( String station : tags){

                String price="", name="", distance="", address="", city="";

                Matcher matcher = Pattern.compile("price-text.*\"> *(.*\\d*\\.\\d*<sup>\\d*)(?=</sup>)").matcher(station);

                if(matcher.find()) {
                    price = matcher.group(1);
                }

                matcher = Pattern.compile("(?<=fuel-station-location-name\">)[^<]*").matcher(station);

                if(matcher.find()){
                    name = matcher.group();
                }

                matcher = Pattern.compile("(?<=fuel-station-location-distance).*(\\d+\\.\\d+ km)</span>").matcher(station);

                if(matcher.find()){
                    distance = matcher.group(1);
                }

                matcher = Pattern.compile("(?<=fuel-station-location-street\">)[^<]*").matcher(station);

                if(matcher.find()){
                    address = matcher.group();
                }

                matcher = Pattern.compile("(?<=fuel-station-location-city\">)[^<]*").matcher(station);

                if(matcher.find()){
                    city = matcher.group();
                }

                assert price != null;
                if(price.equals("")){
                    price = "-.--";
                }

                assert distance != null;
                Tankstelle tankstelle = new Tankstelle(name.trim(), address.trim(), city.trim(), price.trim(), distance.trim());

                Data.tankstellen.add(tankstelle);

            }

            reader.close();

            Log.i("HTMLParser", "Data collection done!");

        }catch (Exception ex){
            Log.e("HTMLParser", ex.fillInStackTrace().toString());
        }

    }


}
