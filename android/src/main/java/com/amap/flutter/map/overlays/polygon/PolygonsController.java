package com.amap.flutter.map.overlays.polygon;

import android.graphics.Color;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.amap.api.maps.AMap;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.PolygonOptions;
import com.amap.api.maps.model.Polyline;
import com.amap.api.maps.model.PolylineOptions;
import com.amap.flutter.map.MyMethodCallHandler;
import com.amap.flutter.map.overlays.AbstractOverlayController;
import com.amap.flutter.map.utils.Const;
import com.amap.flutter.map.utils.ConvertUtil;
import com.amap.flutter.map.utils.LogUtil;

import java.util.ArrayList;
import java.util.List;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/**
 * @author whm
 * @date 2020/11/12 9:53 AM
 * @mail hongming.whm@alibaba-inc.com
 * @since
 */
public class PolygonsController
        extends AbstractOverlayController<PolygonController>
        implements MyMethodCallHandler {

    private static final String CLASS_NAME = "PolygonsController";
    private ArrayList<Polygon> dutyPolygon = new ArrayList<>();

    public PolygonsController(MethodChannel methodChannel, AMap amap) {
        super(methodChannel, amap);
    }

    @Override
    public void doMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        String methodId = call.method;
        LogUtil.i(CLASS_NAME, "doMethodCall===>" +methodId);
        switch (methodId) {
            case Const.METHOD_POLYGON_UPDATE:
                invokePolylineOptions(call, result);
                break;
            case Const.METHOD_POLYGON_IS_IN_DUTY:
                //计算点是否在责任区域内
                if (null != amap) {
                    final double lat = (Double) call.argument("lat");
                    final double lng = (Double) call.argument("lng");
                    LatLng point = new LatLng(lat, lng);
                    for (Polygon item : dutyPolygon) {
                        if(item.contains(point)){
                            result.success(true);
                            return;
                        }
                    }
                    result.success(false);
                }
                break;
        }
    }

    @Override
    public String[] getRegisterMethodIdArray() {
        return Const.METHOD_ID_LIST_FOR_POLYGON;
    }

    /**
     *
     * @param methodCall
     * @param result
     */
    public void invokePolylineOptions(MethodCall methodCall, MethodChannel.Result result) {
        if (null == methodCall) {
            return;
        }
        Object listToAdd = methodCall.argument("polygonsToAdd");
        addByList((List<Object>) listToAdd);
        Object listToChange = methodCall.argument("polygonsToChange");
        updateByList((List<Object>) listToChange);
        Object listIdToRemove = methodCall.argument("polygonIdsToRemove");
        removeByIdList((List<Object>) listIdToRemove);
        result.success(null);
    }

    public void addByList(List<Object> polygonsToAdd) {
        if (polygonsToAdd != null) {
            for (Object polygonToAdd : polygonsToAdd) {
                add(polygonToAdd);
            }
        }
    }

    private void add(Object polylineObj) {
        if (null != amap) {
            PolygonOptionsBuilder builder = new PolygonOptionsBuilder();
            String dartId = PolygonUtil.interpretOptions(polylineObj, builder);
            if (!TextUtils.isEmpty(dartId)) {
                PolygonOptions options = builder.build();
                final Polygon polygon = amap.addPolygon(options);
                if(polygon.getStrokeColor() == -65536){
                    //如果颜色是红色的话代表责任区 添加到责任区数据中
                    polygon.setFillColor(Color.parseColor("#4D7CB6F2"));
                    polygon.setStrokeColor(Color.parseColor("#007CB6F2"));
                    //添加边界图的虚线
                    PolylineOptions polylineOptions = new PolylineOptions();
                    polylineOptions.setPoints(polygon.getPoints());
                    polylineOptions.color(Color.parseColor("#85C1FF"));
                    polylineOptions.setDottedLine(true);
                    polylineOptions.width(15);
                    amap.addPolyline(polylineOptions);
                    dutyPolygon.add(polygon);
                }
                PolygonController polygonController = new PolygonController(polygon);
                controllerMapByDartId.put(dartId, polygonController);
                idMapByOverlyId.put(polygon.getId(), dartId);
            }
        }

    }

    private void updateByList(List<Object> overlaysToChange) {
        if (overlaysToChange != null) {
            for (Object overlayToChange : overlaysToChange) {
                update(overlayToChange);
            }
        }
    }

    private void update(Object toUpdate) {
        Object dartId = ConvertUtil.getKeyValueFromMapObject(toUpdate, "id");
        if (null != dartId) {
            PolygonController controller = controllerMapByDartId.get(dartId);
            if (null != controller) {
                PolygonUtil.interpretOptions(toUpdate, controller);
            }
        }
    }

    private void removeByIdList(List<Object> toRemoveIdList) {
        if (toRemoveIdList == null) {
            return;
        }
        for (Object toRemoveId : toRemoveIdList) {
            if (toRemoveId == null) {
                continue;
            }
            String dartId = (String) toRemoveId;
            final PolygonController controller = controllerMapByDartId.remove(dartId);
            if (controller != null) {

                idMapByOverlyId.remove(controller.getId());
                controller.remove();
            }
        }
    }
}
