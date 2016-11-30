package com.xiaoqi.redpackagehelper.job;

import android.view.accessibility.AccessibilityEvent;

import com.xiaoqi.redpackagehelper.IStatusBarNotification;
import com.xiaoqi.redpackagehelper.QiangHongBaoService;


public interface AccessbilityJob {
    String getTargetPackageName();
    void onCreateJob(QiangHongBaoService service);
    void onReceiveJob(AccessibilityEvent event);
    void onStopJob();
    void onNotificationPosted(IStatusBarNotification service);
    boolean isEnable();
}
