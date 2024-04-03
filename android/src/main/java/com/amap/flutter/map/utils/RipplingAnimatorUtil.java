package com.amap.flutter.map.utils;

import android.animation.ValueAnimator;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;

public class RipplingAnimatorUtil {
    private static RipplingAnimatorUtil animatorUtil;

    public static RipplingAnimatorUtil getInstance() {
        if (animatorUtil == null) {
            animatorUtil = new RipplingAnimatorUtil();
        }
        return animatorUtil;
    }

    /**
     * View缩放动画
     *
     * @param view
     * @param time
     * @param count
     */
    public void scaleViewAnimator(View view, int time, int count) {
        ScaleAnimation scaleAnimation = new ScaleAnimation(0, 1, 0, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(time);
        scaleAnimation.setRepeatCount(count);
        scaleAnimation.setFillAfter(true);
        view.startAnimation(scaleAnimation);
    }

    public static ValueAnimator getValueAnimator(int min, int max, ValueAnimator.AnimatorUpdateListener animatorUpdateListener) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(min, max);
        valueAnimator.setDuration(3000);
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.addUpdateListener(animatorUpdateListener);
        //无限循环
        valueAnimator.setRepeatCount(ValueAnimator.INFINITE);
        //从头开始动画
        valueAnimator.setRepeatMode(ValueAnimator.RESTART);
        valueAnimator.start();
        return valueAnimator;
    }
}
