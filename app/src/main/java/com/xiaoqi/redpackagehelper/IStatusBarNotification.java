package com.xiaoqi.redpackagehelper;

import android.app.Notification;

/**
 */
public interface IStatusBarNotification {

    String getPackageName();
    Notification getNotification();
}
