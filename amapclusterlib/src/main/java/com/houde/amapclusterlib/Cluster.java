package com.houde.amapclusterlib;


import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class Cluster implements IClusterItem {

    private String _id; //唯一表示
    private LatLng mLatLng;
    private List<Cluster> mClusterItems;
    private Marker mMarker;

    private IconRes iconRes;

    Cluster(LatLng latLng, IconRes iconRes) {
        mLatLng = latLng;
        this.iconRes = iconRes;
        mClusterItems = Collections.synchronizedList(new ArrayList<Cluster>());
        mClusterItems.add(this);
        this._id = UUID.randomUUID().toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Cluster cluster = (Cluster) o;
        return _id.equals(cluster._id);
    }

    public String get_id() {
        return _id;
    }

    @Override
    public int hashCode() {
        return _id.hashCode();
    }

    @Override
    public LatLng getPosition() {
        return mLatLng;
    }

    public Marker getMarker() {
        return mMarker;
    }

    @Override
    public IconRes getIconRes() {
        return iconRes;
    }


    void addClusterItem(Cluster clusterItem) {
        if (!mClusterItems.contains(clusterItem)) {
            //去重
            mClusterItems.add(clusterItem);
        }
    }

    boolean removeClusterItem(Cluster clusterItem) {
        if (clusterItem.equals(this)) {
            return false;
        }
        return mClusterItems.remove(clusterItem);
    }

    void clearClusterItem() {
        mClusterItems.clear();
        mClusterItems.add(this);
    }

    boolean containsClusterItem(Cluster clusterItem) {
        return mClusterItems.contains(clusterItem);
    }

    //去重的addAll
    void addAllClusterItem(List<Cluster> items) {
        for (Cluster item : items) {
            addClusterItem(item);
        }
    }

    int getClusterCount() {
        return mClusterItems.size();
    }


    void setMarker(Marker marker) {
        mMarker = marker;
    }

    List<Cluster> getClusterItems() {
        return mClusterItems;
    }


}
