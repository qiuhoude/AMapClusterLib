package com.houde.amapclusterlib;

import java.io.File;

/**
 * Created by Administrator on 2016/7/9.
 */
public class IconRes {

    public final static int TYPE_RES_ICON = 0x55;

    public final static int TYPE_REMOTE_ICON = 0x66;

    public final static int TYPE_LOCAL_ICON = 0x77;

    public File iconFile;
    public int iconRes;
    public String iconRemote;
    public int iconType;

    public IconRes(File iconFile) {
        this.iconFile = iconFile;
        iconType = TYPE_LOCAL_ICON;
    }

    public IconRes(int iconRes) {
        this.iconRes = iconRes;
        iconType = TYPE_RES_ICON;
    }

    public IconRes(String iconRemote) {
        this.iconRemote = iconRemote;
        iconType = TYPE_REMOTE_ICON;
    }


}
