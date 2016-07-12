package com.houde.amapclusterlib;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.Marker;
import com.houde.amapclusterlib.utils.BitmapUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

/**
 * Created by Administrator on 2016/7/9.
 */
public class MarkIconGenerator implements Target {
    public Marker marker;
    private Context context;
    public Drawable circleDrawable;
    private ClusterOverlay.ClusterRender render;
    private int num = 1;

    public MarkIconGenerator(Context context, Marker marker,
                             int number,
                             ClusterOverlay.ClusterRender render) {
        this.marker = marker;
        this.context = context;
        this.render = render;
        this.num = number;
    }


    public synchronized void setNumAndRefresh(int num) {
        this.num = num;
        refreshIcon(circleDrawable);
    }

    public void destoryMarker() {
        //销毁应用的逻辑
        marker.destroy();
        marker = null;
        circleDrawable = null;
    }

    @Override
    public void onBitmapLoaded(Bitmap netBitmap, Picasso.LoadedFrom from) {
//        Bitmap smallBitmap = Bitmap.createScaledBitmap(netBitmap, 120, 120, true);
        Bitmap circleBitmap = BitmapUtils.toRoundBitmap(netBitmap);
        circleDrawable = new BitmapDrawable(context.getResources(), circleBitmap);
        refreshIcon(circleDrawable);
    }

    private void refreshIcon(Drawable drawable) {
        BitmapDescriptor bitmapDescriptor =
                BitmapDescriptorFactory
                        .fromView(render.getClusterView(num, drawable));

        if (marker != null && bitmapDescriptor != null
                && bitmapDescriptor.getWidth() > 0
                && bitmapDescriptor.getHeight() > 0) {
            marker.setIcon(bitmapDescriptor);
        }
    }

    @Override
    public void onBitmapFailed(Drawable errorDrawable) {
        //加载失败重新加载....
    }

    @Override
    public void onPrepareLoad(Drawable placeHolderDrawable) {

    }
}
