package com.xpmodder.spritpreise.shared;

public class Tankstelle {

    public String name, address, city, price, distance;

    public Tankstelle(String name, String address, String city, String price, String distance){
        this.name = name;
        this.address = address;
        this.city = city;
        this.price = price;
        this.distance = distance;
    }


    @Override
    public String toString(){

        String out = this.name;
        out += ":\n\t";
        out += price;
        out += " €\n\t";
        out += address;
        out += "\n\t";
        out += city;
        out += "\n\t";
        out += distance;
        out += " km";

        return out;

    }

}
