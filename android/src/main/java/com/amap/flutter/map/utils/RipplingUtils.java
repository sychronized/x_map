package com.amap.flutter.map.utils;

import android.animation.ValueAnimator;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.Circle;
import com.amap.api.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class RipplingUtils {
    private List<Circle> circleList;//圆集合

    private ValueAnimator valueAnimator;//动画工具

    /**
     * 添加水波纹效果
     *
     * @param latLng 要展示扩散效果的点经纬度
     * AMap aMap：高德地图
     */
    public void addWaveAnimation(LatLng latLng, AMap aMap) {
        circleList = new ArrayList<>();
        int radius = 50;
        for (int i = 0; i < 4; i++) {
            radius = radius + 50 * i;
            circleList.add(RipplingCircleBuilder.addCircle(latLng, radius, aMap));
        }
        valueAnimator = RipplingAnimatorUtil.getValueAnimator(0, 50, new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                int value = (int) animation.getAnimatedValue();
                for (int i = 0; i < circleList.size(); i++) {
                    int nowradius = 50 + 50 * i;
                    Circle circle = circleList.get(i);
                    double radius1 = value + nowradius;
                    circle.setRadius(radius1);
                    int strokePercent = 200;
                    int fillPercent = 20;
                    if (value < 25) {
                        strokePercent = value * 8;
                        fillPercent = value * 20 / 50;
                    } else {
                        strokePercent = 200 - value * 4;
                        fillPercent = 20 - value * 20 / 50;
                    }
                    if (circle.getFillColor() != RipplingCircleBuilder.getStrokeColor(strokePercent)) {
                        circle.setStrokeColor(RipplingCircleBuilder.getStrokeColor(strokePercent));
                        circle.setFillColor(RipplingCircleBuilder.getFillColor(fillPercent));
                    }
                }
            }
        });
    }

    /**
     * 移除水波纹动画
     */
    public void removeCircleWave() {
        if (null != valueAnimator) {
            valueAnimator.cancel();
        }
        if (circleList != null) {
            for (Circle circle : circleList) {
                circle.remove();
            }
            circleList.clear();
        }
    }
}
