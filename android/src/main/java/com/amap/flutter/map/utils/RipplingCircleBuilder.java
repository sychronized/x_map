package com.amap.flutter.map.utils;

import android.graphics.Color;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.CircleOptions;
import com.amap.api.maps.model.LatLng;

public class RipplingCircleBuilder {

    //180, 3, 145, 255
    //10, 0, 0, 180
    public static final int STROKE_COLOR = Color.parseColor("#FF0000");
    public static final int FILL_COLOR = Color.parseColor("#1AFF0000");

    //224,236,237
    //233,241,242


    public static Circle addCircle(LatLng latlng, double radius, AMap aMap) {
        CircleOptions options = new CircleOptions();
        options.strokeWidth(1f);
        options.fillColor(FILL_COLOR);
        options.strokeColor(STROKE_COLOR);
        options.center(latlng);
        options.radius(radius);
        return aMap.addCircle(options);
    }

    public static int getStrokeColor(int alpha) {

//        return Color.argb(alpha, 3, 145, 200);
        return STROKE_COLOR;
    }


    public static int getFillColor(int alpha) {

//        return Color.argb(alpha, 0, 0, 180);
        return FILL_COLOR;

    }
}
