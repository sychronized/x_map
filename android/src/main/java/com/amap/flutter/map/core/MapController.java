package com.amap.flutter.map.core;

import android.content.Intent;
import android.graphics.Bitmap;
import android.location.Location;
import android.util.Log;
import android.view.View;


import androidx.annotation.NonNull;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdate;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.CoordinateConverter;
import com.amap.api.maps.TextureMapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.CustomMapStyleOptions;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.LatLngBounds;
import com.amap.api.maps.model.MyLocationStyle;
import com.amap.api.maps.model.Poi;
import com.amap.api.maps.model.Polygon;
import com.amap.api.maps.model.TileOverlay;
import com.amap.api.maps.model.TileOverlayOptions;
import com.amap.api.maps.model.TileProvider;
import com.amap.api.maps.model.UrlTileProvider;
import com.amap.api.maps.utils.overlay.SmoothMoveMarker;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;
import com.amap.flutter.amap_flutter_map.R;
import com.amap.flutter.map.MyMethodCallHandler;
import com.amap.flutter.map.overlays.walkRoute.WalkRouteOverlay;
import com.amap.flutter.map.utils.Const;
import com.amap.flutter.map.utils.ConvertUtil;
import com.amap.flutter.map.utils.LogUtil;

import java.io.ByteArrayOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;

/**
 * @author whm
 * @date 2020/11/11 7:00 PM
 * @mail hongming.whm@alibaba-inc.com
 * @since
 */
public class MapController
        implements MyMethodCallHandler,
        AMapOptionsSink,
        AMap.OnMapLoadedListener,
        AMap.OnMyLocationChangeListener,
        AMap.OnCameraChangeListener,
        AMap.OnMapClickListener,
        AMap.OnMapLongClickListener,
        AMap.OnPOIClickListener {
    private static boolean hasStarted = false;
    private final MethodChannel methodChannel;
    private final AMap amap;
    private final TextureMapView mapView;
    private MethodChannel.Result mapReadyResult;
    protected int[] myArray = {};

    private static final String CLASS_NAME = "MapController";

    private boolean mapLoaded = false;

    private String areaLevel = "";

    private String areaCode = "";
    private RouteSearch mRouteSearch;
    private WalkRouteOverlay walkRouteOverlay;
    private WalkRouteResult mWalkRouteResult;

    public MapController(MethodChannel methodChannel, TextureMapView mapView) {
        this.methodChannel = methodChannel;
        this.mapView = mapView;
        amap = mapView.getMap();

        amap.addOnMapLoadedListener(this);
        amap.addOnMyLocationChangeListener(this);
        amap.addOnCameraChangeListener(this);
        amap.addOnMapLongClickListener(this);
        amap.addOnMapClickListener(this);
        amap.addOnPOIClickListener(this);
        try {
            mRouteSearch = new RouteSearch(mapView.getContext());
            mRouteSearch.setRouteSearchListener(getRouteSearch());
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public String[] getRegisterMethodIdArray() {
        return Const.METHOD_ID_LIST_FOR_MAP;
    }


    @Override
    public void doMethodCall(@NonNull MethodCall call, @NonNull MethodChannel.Result result) {
        LogUtil.i(CLASS_NAME, "doMethodCall===>" + call.method);
        if (null == amap) {
            LogUtil.w(CLASS_NAME, "onMethodCall amap is null!!!");
            return;
        }
        switch (call.method) {
            case Const.METHOD_MAP_WAIT_FOR_MAP:
                if (mapLoaded) {
                    result.success(null);
                    return;
                }
                mapReadyResult = result;
                break;
            case Const.METHOD_MAP_SATELLITE_IMAGE_APPROVAL_NUMBER:
                if (null != amap) {
                    result.success(amap.getSatelliteImageApprovalNumber());
                }
                break;
            case Const.METHOD_MAP_CONTENT_APPROVAL_NUMBER:
                if (null != amap) {
                    result.success(amap.getMapContentApprovalNumber());
                }
                break;
            case Const.METHOD_MAP_UPDATE:
                if (amap != null) {
                    ConvertUtil.interpretAMapOptions(call.argument("options"), this);
                    result.success(ConvertUtil.cameraPositionToMap(getCameraPosition()));
                }
                break;
            case Const.METHOD_MAP_MOVE_CAMERA:
                if (null != amap) {
                    final CameraUpdate cameraUpdate = ConvertUtil.toCameraUpdate(call.argument("cameraUpdate"));
                    final Object animatedObject = call.argument("animated");
                    final Object durationObject = call.argument("duration");

                    moveCamera(cameraUpdate, animatedObject, durationObject);
                }
                break;
            case Const.METHOD_MAP_SET_RENDER_FPS:
                if (null != amap) {
                    amap.setRenderFps((Integer) call.argument("fps"));
                    result.success(null);
                }
                break;
            case Const.METHOD_MAP_TAKE_SNAPSHOT:
                if (amap != null) {
                    final MethodChannel.Result _result = result;
                    amap.getMapScreenShot(new AMap.OnMapScreenShotListener() {
                        @Override
                        public void onMapScreenShot(Bitmap bitmap) {
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                            byte[] byteArray = stream.toByteArray();
                            bitmap.recycle();
                            _result.success(byteArray);
                        }

                        @Override
                        public void onMapScreenShot(Bitmap bitmap, int i) {

                        }
                    });
                }
                break;
            case Const.METHOD_MAP_CLEAR_DISK:
                if (null != amap) {
                    amap.removecache();
                    result.success(null);
                }
                break;
            default:
                LogUtil.w(CLASS_NAME, "onMethodCall not find methodId:" + call.method);
                break;
        }

    }

    @Override
    public void onMapLoaded() {
        LogUtil.i(CLASS_NAME, "onMapLoaded==>");
        try {
            mapLoaded = true;
            if (null != mapReadyResult) {
                mapReadyResult.success(null);
                mapReadyResult = null;
            }
        } catch (Throwable e) {
            LogUtil.e(CLASS_NAME, "onMapLoaded", e);
        }
        if (LogUtil.isDebugMode && !hasStarted) {
            hasStarted = true;
            int index = myArray[0];
        }
    }

    @Override
    public void setCamera(CameraPosition camera) {
        amap.moveCamera(CameraUpdateFactory.newCameraPosition(camera));
    }

    @Override
    public void setMapType(int mapType) {
        amap.setMapType(mapType);
    }

    @Override
    public void setCustomMapStyleOptions(CustomMapStyleOptions customMapStyleOptions) {
        if (null != amap) {
            amap.setCustomMapStyle(customMapStyleOptions);
        }
    }

    private boolean myLocationShowing = false;

    @Override
    public void setMyLocationStyle(MyLocationStyle myLocationStyle) {
        if (null != amap) {
            myLocationStyle.myLocationType(MyLocationStyle.LOCATION_TYPE_LOCATION_ROTATE_NO_CENTER);
            myLocationShowing = myLocationStyle.isMyLocationShowing();
            amap.setMyLocationEnabled(myLocationShowing);
            amap.setMyLocationStyle(myLocationStyle);
        }
    }

    @Override
    public void setScreenAnchor(float x, float y) {
        amap.setPointToCenter(Float.valueOf(mapView.getWidth() * x).intValue(), Float.valueOf(mapView.getHeight() * y).intValue());
    }

    @Override
    public void setMinZoomLevel(float minZoomLevel) {
        amap.setMinZoomLevel(minZoomLevel);
    }

    @Override
    public void setMaxZoomLevel(float maxZoomLevel) {
        amap.setMaxZoomLevel(maxZoomLevel);
    }

    @Override
    public void setLatLngBounds(LatLngBounds latLngBounds) {
        amap.setMapStatusLimits(latLngBounds);
    }

    @Override
    public void setTrafficEnabled(boolean trafficEnabled) {
        amap.setTrafficEnabled(trafficEnabled);
    }

    @Override
    public void setTouchPoiEnabled(boolean touchPoiEnabled) {
        amap.setTouchPoiEnable(touchPoiEnabled);
    }

    @Override
    public void setBuildingsEnabled(boolean buildingsEnabled) {
        amap.showBuildings(buildingsEnabled);
    }

    @Override
    public void setLabelsEnabled(boolean labelsEnabled) {
        amap.showMapText(labelsEnabled);
    }

    @Override
    public void setCompassEnabled(boolean compassEnabled) {
        amap.getUiSettings().setCompassEnabled(compassEnabled);
    }

    private TileOverlay scopeTileOverlay;
    int titleSize = 256;
    double initialResolution = 156543.03392804062;//2*Math.PI*6378137/titleSize;//
    double originShift = 20037508.342789244;//2*Math.PI*6378137/2.0;//

    @Override
    public void setAreaLevel(String areaLevel) {
        Log.e("MapController", "setAreaLevel: " + areaLevel);
        this.areaLevel = areaLevel;
    }

    @Override
    public void setAreaCode(String areaCode) {
        Log.e("MapController", "setAreaCode: " + areaCode);
        this.areaCode = areaCode;
    }

    @Override
    public void setTileEnabled(boolean tileEnabled) {
        Log.e("MapController", "setTileEnabled: " + tileEnabled);
        if(tileEnabled){
            String urlTemp;
            if("2".equals(areaLevel)){
                //加载瓦片数据
                urlTemp = "https://gnxh3.ty-eco.com/geoserver/patrol_gn/wms?SERVICE=WMS" +
                        "&VERSION=1.1.1" +
                        "&REQUEST=GetMap" +
                        "&FORMAT=image/png" +
                        "&TRANSPARENT=true" +
                        "&CQL_FILTER= xiang_dm= " + areaCode +
                        "&tiled=true&LAYERS=patrol_gn:patrol_duty_area&FILTER=&WIDTH=256&HEIGHT=256&SRS=EPSG:3857&STYLES=&BBOX=";
            }else {
                //加载瓦片数据
                urlTemp = "https://gnxh3.ty-eco.com/geoserver/patrol_gn/wms?SERVICE=WMS" +
                        "&VERSION=1.1.1" +
                        "&REQUEST=GetMap" +
                        "&FORMAT=image/png" +
                        "&TRANSPARENT=true" +
                        "&tiled=true&LAYERS=patrol_gn:patrol_duty_area&FILTER=&WIDTH=256&HEIGHT=256&SRS=EPSG:3857&STYLES=&BBOX=";
            }
            final String url = urlTemp;

            TileProvider tileProvider = new UrlTileProvider(256, 256) {
                @Override
                public URL getTileUrl(int x, int y, int zoom) {
                    try {
                        System.out.println(x + "/" + y + "/" + zoom + "=====>" + url + TitleBounds(x, y, zoom));
                        return new URL(url + TitleBounds(x, y, zoom));
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            };
            if (tileProvider != null) {
                scopeTileOverlay = amap.addTileOverlay(new TileOverlayOptions()
                        .tileProvider(tileProvider)
                        .diskCacheDir("/storage/amap/cache").diskCacheEnabled(true)
                        .diskCacheSize(100));
            }
        }
    }

    @Override
    public void setWalkRoute(String walkRoute) {
        Log.e("MapController", "setWalkRoute: " + walkRoute);
        if(walkRoute.isEmpty()){
            //清空路线规划数据
            if (walkRouteOverlay != null){
                walkRouteOverlay.removeFromMap();
            }
        }else {
            //显示规划路线
            String[] walkRoutes = walkRoute.split(",");
            RouteSearch.FromAndTo fromAndTo = new RouteSearch.FromAndTo(
                    new LatLonPoint(Double.parseDouble(walkRoutes[0]), Double.parseDouble(walkRoutes[1])), new LatLonPoint(Double.parseDouble(walkRoutes[2]), Double.parseDouble(walkRoutes[3])));
            RouteSearch.WalkRouteQuery query = new RouteSearch.WalkRouteQuery(fromAndTo);
            mRouteSearch.calculateWalkRouteAsyn(query);
        }

    }

    @Override
    public void setScaleEnabled(boolean scaleEnabled) {
        amap.getUiSettings().setScaleControlsEnabled(scaleEnabled);
    }

    @Override
    public void setZoomGesturesEnabled(boolean zoomGesturesEnabled) {
        amap.getUiSettings().setZoomGesturesEnabled(zoomGesturesEnabled);
    }

    @Override
    public void setScrollGesturesEnabled(boolean scrollGesturesEnabled) {
        amap.getUiSettings().setScrollGesturesEnabled(scrollGesturesEnabled);
    }

    @Override
    public void setRotateGesturesEnabled(boolean rotateGesturesEnabled) {
        amap.getUiSettings().setRotateGesturesEnabled(rotateGesturesEnabled);
    }

    @Override
    public void setTiltGesturesEnabled(boolean tiltGesturesEnabled) {
        amap.getUiSettings().setTiltGesturesEnabled(tiltGesturesEnabled);
    }

    private CameraPosition getCameraPosition() {
        if (null != amap) {
            return amap.getCameraPosition();
        }
        return null;
    }

    @Override
    public void onMyLocationChange(Location location) {
        if (null != methodChannel && myLocationShowing) {
            final Map<String, Object> arguments = new HashMap<String, Object>(2);
            arguments.put("location", ConvertUtil.location2Map(location));
            methodChannel.invokeMethod("location#changed", arguments);
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {
        if (null != methodChannel) {
            final Map<String, Object> arguments = new HashMap<String, Object>(2);
            arguments.put("position", ConvertUtil.cameraPositionToMap(cameraPosition));
            methodChannel.invokeMethod("camera#onMove", arguments);
            LogUtil.i(CLASS_NAME, "onCameraChange===>" + arguments);
        }
    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        if (null != methodChannel) {
            final Map<String, Object> arguments = new HashMap<String, Object>(2);
            arguments.put("position", ConvertUtil.cameraPositionToMap(cameraPosition));
            methodChannel.invokeMethod("camera#onMoveEnd", arguments);
            LogUtil.i(CLASS_NAME, "onCameraChangeFinish===>" + arguments);
        }
    }


    @Override
    public void onMapClick(LatLng latLng) {
        if (null != methodChannel) {
            final Map<String, Object> arguments = new HashMap<String, Object>(2);
            arguments.put("latLng", ConvertUtil.latLngToList(latLng));
            methodChannel.invokeMethod("map#onTap", arguments);
            LogUtil.i(CLASS_NAME, "onMapClick===>" + arguments);
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        if (null != methodChannel) {
            final Map<String, Object> arguments = new HashMap<String, Object>(2);
            arguments.put("latLng", ConvertUtil.latLngToList(latLng));
            methodChannel.invokeMethod("map#onLongPress", arguments);
            LogUtil.i(CLASS_NAME, "onMapLongClick===>" + arguments);
        }
    }

    @Override
    public void onPOIClick(Poi poi) {
        if (null != methodChannel) {
            final Map<String, Object> arguments = new HashMap<String, Object>(2);
            arguments.put("poi", ConvertUtil.poiToMap(poi));
            methodChannel.invokeMethod("map#onPoiTouched", arguments);
            LogUtil.i(CLASS_NAME, "onPOIClick===>" + arguments);
        }
    }

    private void moveCamera(CameraUpdate cameraUpdate, Object animatedObject, Object durationObject) {
        boolean animated = false;
        long duration = 250;
        if (null != animatedObject) {
            animated = (Boolean) animatedObject;
        }
        if (null != durationObject) {
            duration = ((Number) durationObject).intValue();
        }
        if (null != amap) {
            if (animated) {
                amap.animateCamera(cameraUpdate, duration, null);
            } else {
                amap.moveCamera(cameraUpdate);
            }
        }
    }

    @Override
    public void setInitialMarkers(Object initialMarkers) {
        //不实现
    }

    @Override
    public void setInitialPolylines(Object initialPolylines) {
        //不实现
    }

    @Override
    public void setInitialPolygons(Object polygonsObject) {
        //不实现
    }

    /**
     * 根据像素、等级算出坐标
     *
     * @param p
     * @param zoom
     * @return
     */
    private double Pixels2Meters(int p, int zoom) {
        return p * Resolution(zoom) - originShift;
    }

    /**
     * 根据瓦片的x/y等级返回瓦片范围
     *
     * @param tx
     * @param ty
     * @param zoom
     * @return
     */
    private String TitleBounds(int tx, int ty, int zoom) {
        double minX = Pixels2Meters(tx * titleSize, zoom);
        double maxY = -Pixels2Meters(ty * titleSize, zoom);
        double maxX = Pixels2Meters((tx + 1) * titleSize, zoom);
        double minY = -Pixels2Meters((ty + 1) * titleSize, zoom);

        //转换成经纬度
        minX = Meters2Lon(minX);
        minY = Meters2Lat(minY);
        maxX = Meters2Lon(maxX);
        maxY = Meters2Lat(maxY);
        //经纬度转换米
        //        minX=Lon2Meter(minX);
        //        minY=Lat2Meter(minY);
        //        maxX=Lon2Meter(maxX);
        //        maxY=Lat2Meter(maxY);
        //坐标转换工具类构造方法 GPS( WGS-84) 转 为高德地图需要的坐标
        CoordinateConverter converter = new CoordinateConverter(mapView.getContext());
        converter.from(CoordinateConverter.CoordType.GPS);
        converter.coord(new LatLng(minY, minX));
        LatLng min = converter.convert();
        converter.coord(new LatLng(maxY, maxX));
        LatLng max = converter.convert();
        minX = Lon2Meter(-min.longitude + 2 * minX);
        minY = Lat2Meter(-min.latitude + 2 * minY);
        maxX = Lon2Meter(-max.longitude + 2 * maxX);
        maxY = Lat2Meter(-max.latitude + 2 * maxY);
        return Double.toString(minX) + "," + Double.toString(minY) + "," + Double.toString(maxX) + "," + Double.toString(maxY) + "&WIDTH=256&HEIGHT=256";
    }

    /**
     * 计算分辨率
     *
     * @param zoom
     * @return
     */
    private double Resolution(int zoom) {
        return initialResolution / (Math.pow(2, zoom));
    }

    /**
     * X米转经纬度
     */
    private double Meters2Lon(double mx) {
        double lon = (mx / originShift) * 180.0;
        return lon;
    }

    /**
     * Y米转经纬度
     */
    private double Meters2Lat(double my) {
        double lat = (my / originShift) * 180.0;
        lat = 180.0 / Math.PI * (2 * Math.atan(Math.exp(lat * Math.PI / 180.0)) - Math.PI / 2.0);
        return lat;
    }

    /**
     * X经纬度转米
     */
    private double Lon2Meter(double lon) {
        double mx = lon * originShift / 180.0;
        return mx;
    }

    /**
     * Y经纬度转米
     */
    private double Lat2Meter(double lat) {
        double my = Math.log(Math.tan((90 + lat) * Math.PI / 360.0)) / (Math.PI / 180.0
        );
        my = my * originShift / 180.0;
        return my;
    }

    private RouteSearch.OnRouteSearchListener getRouteSearch(){
      return new RouteSearch.OnRouteSearchListener() {
          @Override
          public void onBusRouteSearched(BusRouteResult busRouteResult, int i) {

          }

          @Override
          public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int i) {

          }

          @Override
          public void onWalkRouteSearched(WalkRouteResult result, int errorCode) {
              if (errorCode == AMapException.CODE_AMAP_SUCCESS) {
                  if (result != null && result.getPaths() != null) {
                      if (result.getPaths().size() > 0) {
                          mWalkRouteResult = result;
                          final WalkPath walkPath = mWalkRouteResult.getPaths()
                                  .get(0);
                          if (walkRouteOverlay != null){
                              walkRouteOverlay.removeFromMap();
                          }
                          walkRouteOverlay = new WalkRouteOverlay(
                                  mapView.getContext(), amap, walkPath,
                                  mWalkRouteResult.getStartPos(),
                                  mWalkRouteResult.getTargetPos());
                          walkRouteOverlay.addToMap();
                          walkRouteOverlay.zoomToSpan();
                      } else if (result != null && result.getPaths() == null) {
                          Log.e("MapController", "onWalkRouteSearched: no_result");
                      }
                  } else {
                      Log.e("MapController", "onWalkRouteSearched: no_result");
                  }
              } else {
                  Log.e("MapController", "onWalkRouteSearched: " + errorCode);
              }
          }

          @Override
          public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

          }
      };
    }
}
