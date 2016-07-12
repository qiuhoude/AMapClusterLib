package com.houde.amapclusterlib;

import com.amap.api.maps.model.LatLng;


/**
 * 聚合点的元素
 */
public interface IClusterItem {

    /**
     * 返回聚合元素的地理位置
     *
     * @return
     */
    public LatLng getPosition();



    public  IconRes getIconRes();


}
