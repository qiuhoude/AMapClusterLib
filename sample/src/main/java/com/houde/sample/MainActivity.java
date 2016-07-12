package com.houde.sample;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.amap.api.maps.AMap;
import com.amap.api.maps.CameraUpdateFactory;
import com.amap.api.maps.MapView;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.houde.amapclusterlib.ClusterOverlay;
import com.houde.amapclusterlib.IClusterItem;
import com.houde.amapclusterlib.IconRes;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends Activity implements
        AMap.OnMarkerClickListener,
        AMap.OnMapLoadedListener,
        AMap.OnCameraChangeListener {


    MapView mapView;
    AMap aMap;
    Context mContext;
    private ClusterOverlay clusterOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = getApplicationContext();
        initMap(savedInstanceState);
        addMarkersToMap();// 往地图上添加marker
    }

    private void initMap(Bundle savedInstanceState) {
        mapView = (MapView) findViewById(R.id.map);
        mapView.onCreate(savedInstanceState); // 此方法必须重写
        if (aMap == null) {
            aMap = mapView.getMap();
            setUpMap();
        }
    }

    private void setUpMap() {
        aMap.setOnMapLoadedListener(this);// 设置amap加载成功事件监听器
        aMap.setOnMarkerClickListener(this);// 设置点击marker事件监听器
        aMap.setOnCameraChangeListener(this);// 设置自定义InfoWindow样式
    }

    private static final String TAG = "ClusterMarkerActivity";


    private void addMarkersToMap() {

        List<IClusterItem> clusterItems = new ArrayList<IClusterItem>();
        for (int i = 0; i < Images.imageUrls.length; i++) {
            Random r = new Random();
            double lat = (290000 + r.nextInt(30000)) / 10000.0D;
            double lng = (1120000 + r.nextInt(30000)) / 10000.0D;
            LatLng latLng = new LatLng(lat, lng);
            clusterItems.add(new ImgData(latLng, Images.imageUrls[i]));

        }
        Log.e(TAG, "list " + clusterItems);
        clusterOverlay = new ClusterOverlay(mContext, aMap, dp2px(80), clusterItems, null);
    }

    @Override
    public void onMapLoaded() {
        aMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(29, 112), 8));
    }


    /**
     * 根据手机的分辨率从 dp 的单位 转成为 px(像素)
     */
    public int dp2px(float dpValue) {
        final float scale = mContext.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    static class ImgData implements IClusterItem {
        LatLng latLng;
        String imgStr;

        public ImgData(LatLng latLng, String imgStr) {
            this.latLng = latLng;
            this.imgStr = imgStr;
        }

        @Override
        public LatLng getPosition() {
            return latLng;
        }

        @Override
        public IconRes getIconRes() {
            return new IconRes(imgStr);
        }

        @Override
        public String toString() {
            return "ImgData{" +
                    "latLng=" + latLng +
                    ", imgStr='" + imgStr + '\'' +
                    '}';
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clusterOverlay!=null){
            clusterOverlay.destroy();
        }
    }

    @Override
    public void onCameraChange(CameraPosition cameraPosition) {

    }

    @Override
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        clusterOverlay.onCameraChangeFinish(cameraPosition);
    }


    @Override
    public boolean onMarkerClick(Marker marker) {
        return false;
    }


}
