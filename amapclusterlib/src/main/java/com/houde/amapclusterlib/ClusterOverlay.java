package com.houde.amapclusterlib;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.amap.api.maps.AMap;
import com.amap.api.maps.Projection;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.CameraPosition;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Administrator on 2016/7/9.
 */
public class ClusterOverlay {

    private Context mContext;
    private List<IClusterItem> mPoints; //所有点的点位信息
    private List<Cluster> mAllClusters; //所有的聚合点
    private List<Cluster> mShowClusters; //显示聚合点
    private int mClusterRadius;//聚合点半径,单位 dp
    private ExecutorService executor; //线程池
    private AMap mAMap;
    private Projection mProjection;
    private float level = 0f; //地图的当前的缩放级别

    //icon生成器
    private Map<Marker, MarkIconGenerator> iconGeneratorMap;
    private ClusterRender render;


    public interface ClusterRender {
        /**
         * @param num
         * @param drawable 可能为null
         * @return
         */
        public View getClusterView(int num, Drawable drawable);
    }


    static class UiHandler extends Handler {
        public UiHandler() {
        }

        public UiHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            final MsgResult msgResult = (MsgResult) msg.obj;
            switch (msg.what) {
                case ZOOM_INIT: //init初始化
                    msgResult.clusterOverlay.initClusterToMap();
                    break;
                case ZOOM_OUT: {
                    //合并
                    //1 隐藏
                    //2 重绘制icon
                    final Map<Cluster, LatLng> moveClusters = msgResult.moveClusters;
                    final Set<Cluster> redrawClusters = msgResult.redrawClusters;
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Iterator<Cluster> redrawIterator = redrawClusters.iterator();
                            while (redrawIterator.hasNext()) {
                                Cluster cluster = redrawIterator.next();
                                msgResult.clusterOverlay.addSingleClusterToMap(cluster);
                            }
                            for (Map.Entry<Cluster, LatLng> kv : moveClusters.entrySet()) {
                                Cluster cluster = kv.getKey();
//                                cluster.getMarker().setVisible(false);
                                msgResult.clusterOverlay
                                        .moveMarkerAnim(cluster.getMarker(),
                                                cluster.getPosition(), kv.getValue(), false);
                            }
                            redrawClusters.clear();
                            moveClusters.clear();
                        }
                    });
                    break;
                }
                case ZOOM_IN: {
                    //移出
                    final Map<Cluster, LatLng> moveClusters = msgResult.moveClusters;
                    final Set<Cluster> redrawClusters = msgResult.redrawClusters;
                    uiHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            Iterator<Cluster> redrawIterator = redrawClusters.iterator();
                            while (redrawIterator.hasNext()) {
                                Cluster cluster = redrawIterator.next();
                                msgResult.clusterOverlay.addSingleClusterToMap(cluster);
                            }
                            for (Map.Entry<Cluster, LatLng> kv : moveClusters.entrySet()) {
                                Cluster cluster = kv.getKey();
                                msgResult.clusterOverlay.addSingleClusterToMap(cluster);
//                                cluster.getMarker().setVisible(true);
                                msgResult.clusterOverlay
                                        .moveMarkerAnim(cluster.getMarker(),
                                                kv.getValue(), cluster.getPosition(), true);
                            }

                            redrawClusters.clear();
                            moveClusters.clear();
                        }
                    });
                    break;
                }
            }
        }
    }

    //防止handler内存泄露
    private static Handler uiHandler = new UiHandler(Looper.getMainLooper());


    public void moveMarkerAnim(final Marker marker, final LatLng start, final LatLng end, final boolean isVisible) {

//        marker.setVisible(true);
//        marker.setPosition(start);
//        final long startTime = SystemClock.uptimeMillis();
//
//        final long duration = 300;
//
//        final Interpolator interpolator = new LinearInterpolator();

//        uiHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                long elapsed = SystemClock.uptimeMillis() - startTime;
//                float percent = interpolator.getInterpolation((float) elapsed / duration);
//                //
//                double lng = percent * end.longitude + (1 - percent)
//                        * start.longitude;
//                double lat = percent * end.latitude + (1 - percent)
//                        * start.latitude;
//                marker.setPosition(new LatLng(lat, lng));
//                if (percent < 1.0) {
//                    uiHandler.postDelayed(this, 16);
//                } else {
//                    marker.setVisible(isVisible);
//                }
//            }
//        });


        //TODO 使用属性动画进行写动画
        ValueAnimator animator = new ValueAnimator();
        animator.setDuration(300);
        animator.setObjectValues(start, end);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.setEvaluator(new TypeEvaluator<LatLng>() {
            @Override
            public LatLng evaluate(float fraction, LatLng startValue, LatLng endValue) {
                double longitude = startValue.longitude + (fraction * (endValue.longitude - startValue.longitude));
                double latitude = startValue.latitude + (fraction * (endValue.latitude - startValue.latitude));
                return new LatLng(latitude, longitude);
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                marker.setVisible(isVisible);
                animation.removeListener(this);
            }
        });
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                LatLng latLng = (LatLng) animation.getAnimatedValue();
                marker.setPosition(latLng);
            }
        });
        animator.start();

    }


    public ClusterOverlay(Context context,
                          AMap amap,
                          int clusterSize,
                          List<IClusterItem> clusterItems,
                          ClusterRender render) {
        this.mContext = context;
        this.mAMap = amap;
        this.mClusterRadius = clusterSize;
        this.render = render;
        if (clusterItems != null) {
            mPoints = clusterItems;
        } else {
            mPoints = new ArrayList<IClusterItem>();
        }
        mAllClusters = Collections.synchronizedList(new ArrayList<Cluster>());
        //初始化聚合点集合
        mShowClusters = Collections.synchronizedList(new ArrayList<Cluster>());

        iconGeneratorMap = new HashMap<Marker, MarkIconGenerator>();
        mProjection = amap.getProjection();
        executor = Executors.newFixedThreadPool(2);

    }

    /**
     * 是否是第一次加载
     */
    private volatile boolean isInitLoad = true;


    class InitLoadTask implements Runnable {
        @Override
        public void run() {
            Log.e(TAG, " InitLoadTask");
            for (IClusterItem item : mPoints) {
                Cluster cluster = new Cluster(item.getPosition(), item.getIconRes());
                mAllClusters.add(cluster);
            }

            //用完清除掉
            mPoints.clear();
            Log.e(TAG, "mAllClusters size " + mAllClusters.size() + " 个 ");

            for (Cluster cluster : mAllClusters) {
                LatLng latlng = cluster.getPosition();
                Point point = mProjection.toScreenLocation(latlng);
                Cluster existCluster = getCluster(point);
                if (existCluster != null) {
                    existCluster.addClusterItem(cluster);
                } else {
                    mShowClusters.add(cluster);
                }
            }
            //首次加载marker需要发送送消息
            sendMessage(ZOOM_INIT, null, null);
            Log.e(TAG, "mAllClusters first :" + mAllClusters.get(0).get_id());
            Log.e(TAG, "mShowClusters first :" + mShowClusters.get(0).get_id());

            Log.e(TAG, "mShowClusters size " + mShowClusters.size() + " 个 ");
            int sum = 0;
            for (Cluster cluster : mShowClusters) {
                sum += cluster.getClusterCount();
            }
            Log.e(TAG, "mShowClusters 子 marker总合 " + sum + " 个");

        }
    }


    class ZoomOutLoadTask implements Runnable {
        @Override
        public void run() {
            //缩小 是合并动画
            Log.e(TAG, " ZoomOutLoadTask");
            if (mShowClusters.size() == 1) {
                Cluster cluster = mShowClusters.get(0);
                Log.e(TAG, cluster.get_id() + " 最终 cluster 有:" + cluster.getClusterCount() + " 个");
            }
            Set<Cluster> redrawClusters = new HashSet<Cluster>();
            Map<Cluster, LatLng> moveClusters = new HashMap<Cluster, LatLng>();
            int i = 0;
            List<Cluster> tempList = new ArrayList<Cluster>();
            tempList.addAll(mShowClusters);
            for (Cluster cluster : tempList) {
                LatLng latlng = cluster.getPosition();
                Point point = mProjection.toScreenLocation(latlng);
                Cluster existCluster = getCluster(point);
                if (existCluster != null
                        && !existCluster.equals(cluster)) {
                    existCluster.addAllClusterItem(cluster.getClusterItems());
                    cluster.clearClusterItem();

                    mShowClusters.remove(cluster);

                    moveClusters.put(cluster, existCluster.getPosition());
                    redrawClusters.add(existCluster);
                    i++;
                }
            }
            tempList.clear();

            sendMessage(ZOOM_OUT, moveClusters, redrawClusters);
            int sum = 0;
            for (Cluster cluster : mShowClusters) {
                sum += cluster.getClusterCount();
            }
            Log.e(TAG, "mShowClusters 子 marker总合 " + sum + " 个");
            Log.e(TAG, "删除 " + i + " 个, 还有 " + mShowClusters.size() + " 个");

        }
    }

    private Cluster findParent(Cluster cluster) {
        for (Cluster showParent : mShowClusters) {
            if (showParent.containsClusterItem(cluster)) {
                return showParent;
            }
        }
        return null;
    }

    class ZoomInLoadTask implements Runnable {

        @Override
        public void run() {
            //放大是移出动画
            Log.e(TAG, " ZoomInLoadTask");

            Set<Cluster> redrawClusters = new HashSet<Cluster>();
            Map<Cluster, LatLng> moveClusters = new HashMap<Cluster, LatLng>();
            //计算那些点需要移出
            int i = 0;

            for (Cluster cluster : mAllClusters) {
                LatLng latlng = cluster.getPosition();
                Point point = mProjection.toScreenLocation(latlng);
                Cluster existCluster = getCluster(point);
                if (existCluster == null) {
                    Cluster parent = findParent(cluster);
                    if (parent == null) {
                        continue;
                    }
                    parent.removeClusterItem(cluster);
                    //独立出一个聚合点
                    mShowClusters.add(cluster);

                    moveClusters.put(cluster, parent.getPosition());
                    redrawClusters.add(parent);
                    i++;
                } else {
                    //
                    if (moveClusters.containsKey(existCluster)) {
                        Cluster parent = findParent(cluster);
                        if (parent == null) {
                            continue;
                        }
                        parent.removeClusterItem(cluster);
                        existCluster.addClusterItem(cluster);
                    }
                }
            }
            sendMessage(ZOOM_IN, moveClusters, redrawClusters);
            int sum = 0;
            for (Cluster cluster : mShowClusters) {
                sum += cluster.getClusterCount();
            }
            Log.e(TAG, "mShowClusters 子 marker总合 " + sum + " 个");
            Log.e(TAG, "添加了 " + i + " 个, 现在有 " + mShowClusters.size() + " 个");

        }
    }


    private static final String TAG = "ClusterOverlay";

    /**
     * 重新分配聚合点
     */
    private void assignClusters(int zoomType) {
        //此处分3种情况
        //1. init 初始化
        //2. zoom out 缩小
        //3. zoom in 放大
        if (isInitLoad) {
            isInitLoad = false;
            executor.submit(new InitLoadTask());
        } else if (zoomType == ZOOM_IN) {
            executor.submit(new ZoomInLoadTask());
        } else if (zoomType == ZOOM_OUT) {
            executor.submit(new ZoomOutLoadTask());
        }

    }


    private void sendMessage(int what, Map<Cluster, LatLng> moveClusters, Set<Cluster> redrawClusters) {
        Message message = uiHandler.obtainMessage(what,
                new MsgResult(ClusterOverlay.this, moveClusters, redrawClusters));
        message.sendToTarget();
    }


    static class MsgResult {
        ClusterOverlay clusterOverlay;
        Map<Cluster, LatLng> moveClusters;
        Set<Cluster> redrawClusters;


        public MsgResult(ClusterOverlay clusterOverlay,
                         Map<Cluster, LatLng> moveClusters, Set<Cluster> redrawClusters) {
            this.clusterOverlay = clusterOverlay;
            this.moveClusters = moveClusters;
            this.redrawClusters = redrawClusters;
        }

    }

    /**
     * 将聚合元素添加至地图上
     */
    private void initClusterToMap() {
        for (Cluster cluster : mShowClusters) {
            addSingleClusterToMap(cluster);
        }
    }

    /**
     * 将单个聚合元素添加至地图显示
     *
     * @param cluster
     */
    private void addSingleClusterToMap(Cluster cluster) {
        if (render == null) {
            //加载默认的布局
            render = new ClusterRender() {
                @Override
                public View getClusterView(int num, Drawable drawable) {
                    return inflateViewByDrawable(mContext, drawable, String.valueOf(num));
                }
            };
        }
        int num = cluster.getClusterCount();
        if (cluster.getMarker() == null) {
            //为空时创建marker
            LatLng latlng = cluster.getPosition();
            MarkerOptions markerOptions = new MarkerOptions();
            BitmapDescriptor bitmapDescriptor = BitmapDescriptorFactory
                    .fromView(render.getClusterView(num, null));
            markerOptions.anchor(0.5f, 0.5f)
                    .icon(bitmapDescriptor)
                    .position(latlng);

            Marker marker = mAMap.addMarker(markerOptions);
            cluster.setMarker(marker);
            //加载icon图片
            iconGeneratorMap.put(marker, new MarkIconGenerator(mContext, marker, num, render));
            loadIcon(cluster);
        } else {
            //重新生成icon
            MarkIconGenerator markIconGenerator = iconGeneratorMap.get(cluster.getMarker());
            markIconGenerator.setNumAndRefresh(num);
        }
    }


    /**
     * 用Picasso 加载icon
     *
     * @param cluster
     */
    public void loadIcon(Cluster cluster) {
        int iconType = cluster.getIconRes().iconType;
        //加载图片,远程 本地 资源库
        if (iconType == IconRes.TYPE_RES_ICON) {
            Picasso.with(mContext)
                    .load(cluster.getIconRes().iconRes)
                    .resize(120, 120)
                    .into(iconGeneratorMap.get(cluster.getMarker()));
        } else if (iconType == IconRes.TYPE_LOCAL_ICON) {
            Picasso.with(mContext)
                    .load(cluster.getIconRes().iconFile)
                    .resize(120, 120)
                    .into(iconGeneratorMap.get(cluster.getMarker()));
        } else if (iconType == IconRes.TYPE_REMOTE_ICON) {
            Picasso.with(mContext)
                    .load(cluster.getIconRes().iconRemote)
                    .resize(120, 120)
                    .into(iconGeneratorMap.get(cluster.getMarker()));
        }
    }


    /**
     * 根据一个点获取是否可以依附的聚合点，没有则返回null
     *
     * @param point
     * @return
     */
    private Cluster getCluster(Point point) {
        for (Cluster cluster : mShowClusters) {
            if (calculateDistance(cluster, point)) {
                return cluster;
            }
        }
        return null;
    }


    private Cluster getClusterInList(List<Cluster> list, Point point) {
        for (Cluster cluster : list) {
            if (calculateDistance(cluster, point)) {
                return cluster;
            }
        }
        return null;
    }


    /**
     * @param cluster
     * @param point
     * @return true 表示这个点在这个范围内
     */
    private boolean calculateDistance(Cluster cluster, Point point) {
        Point poi = mProjection.toScreenLocation(cluster.getPosition());
        double distance = getDistanceBetweenTwoPoints(point.x, point.y,
                poi.x, poi.y);
        if (distance < mClusterRadius) {
            return true;
        }
        return false;
    }

    /**
     * 两点的距离
     *
     * @param x1
     * @param y1
     * @param x2
     * @param y2
     * @return
     */
    private double getDistanceBetweenTwoPoints(double x1, double y1, double x2,
                                               double y2) {
        double distance = Math.sqrt((x1 - x2) * (x1 - x2) + (y1 - y2)
                * (y1 - y2));
        return distance;
    }


    private static final int ZOOM_IN = 0x38; //放大
    private static final int ZOOM_OUT = 0x39; //缩小
    private static final int ZOOM_INIT = 0x37; //初始化

    /**
     * OnCameraChangeListener 的onCameraChangeFinish里面进行调用
     * 放大缩小完成后对聚合点进行重新计算
     *
     * @param cameraPosition
     */
    public void onCameraChangeFinish(CameraPosition cameraPosition) {
        float levelTemp = cameraPosition.zoom;
        if (levelTemp != level) {
            assignClusters(level > levelTemp ? ZOOM_OUT : ZOOM_IN);
            level = levelTemp;
        }
    }


    public void destroy() {

        if (mAllClusters != null) {
            mAllClusters.clear();
            mAllClusters = null;
        }
        if (mShowClusters != null) {
            mShowClusters.clear();
            mShowClusters = null;
        }
        //销毁生成器
        if (iconGeneratorMap != null) {
            for (Map.Entry<Marker, MarkIconGenerator> entrie : iconGeneratorMap.entrySet()) {
                MarkIconGenerator generator = entrie.getValue();
                generator.destoryMarker();
            }
        }
        iconGeneratorMap.clear();
    }

    public static View inflateViewByDrawable(Context context, Drawable drawable, String num) {
        FrameLayout frameLayout = new FrameLayout(context);
        frameLayout.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT));
//        frameLayout.setBackgroundResource(R.drawable.custom_info_bubble);

        ImageView iv_icon = new ImageView(context);
        iv_icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        iv_icon.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT
                , ViewGroup.LayoutParams.WRAP_CONTENT));

        BadgeView badgeView = new BadgeView(context);
        badgeView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT
                , ViewGroup.LayoutParams.WRAP_CONTENT));
        badgeView.setText(num);
        badgeView.show();

        if (drawable != null) {
            iv_icon.setImageDrawable(drawable);
        }

        frameLayout.addView(iv_icon);
        frameLayout.addView(badgeView);
        return frameLayout;
    }


}
