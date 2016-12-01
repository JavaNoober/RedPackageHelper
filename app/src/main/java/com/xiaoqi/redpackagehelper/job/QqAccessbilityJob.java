package com.xiaoqi.redpackagehelper.job;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.xiaoqi.redpackagehelper.BuildConfig;
import com.xiaoqi.redpackagehelper.Config;
import com.xiaoqi.redpackagehelper.IStatusBarNotification;
import com.xiaoqi.redpackagehelper.QHBApplication;
import com.xiaoqi.redpackagehelper.QiangHongBaoService;
import com.xiaoqi.redpackagehelper.util.AccessibilityHelper;
import com.xiaoqi.redpackagehelper.util.NotifyHelper;

import java.util.List;

public class QqAccessbilityJob extends BaseAccessbilityJob {

    private static final String TAG = "QqAccessbilityJob";

    /** qq的包名*/
    public static final String QQ_PACKAGENAME = "com.tencent.mobileqq";

    /** 红包消息的关键字*/
    private static final String HONGBAO_TEXT_KEY = "[QQ红包]";


    private static final int WINDOW_NONE = 0;
    private static final int WINDOW_LUCKYMONEY_DETAIL = 2;
    private static final int WINDOW_LAUNCHER = 3;
    private static final int WINDOW_OTHER = -1;

    private int mCurrentWindow = WINDOW_NONE;

    private boolean isReceivingHongbao;
    private boolean needInput;
    private boolean needSend;

    private PackageInfo mWechatPackageInfo = null;
    private Handler mHandler = null;
    private boolean hasGetMoney = false;

    @Override
    public void onCreateJob(QiangHongBaoService service) {
        super.onCreateJob(service);

    }

    @Override
    public void onStopJob() {

    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void onNotificationPosted(IStatusBarNotification sbn) {
        Notification nf = sbn.getNotification();
        String text = String.valueOf(sbn.getNotification().tickerText);
        notificationEvent(text, nf);
    }

    @Override
    public boolean isEnable() {
        return getConfig().isEnableWechat();
    }

    @Override
    public String getTargetPackageName() {
        return QQ_PACKAGENAME;
    }

    @Override
    public void onReceiveJob(AccessibilityEvent event) {
        final int eventType = event.getEventType();
        //通知栏事件
        if(eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            Parcelable data = event.getParcelableData();
            if(data == null || !(data instanceof Notification)) {
                return;
            }
            if(QiangHongBaoService.isNotificationServiceRunning() && getConfig().isEnableNotificationService()) { //开启快速模式，不处理
                return;
            }
            List<CharSequence> texts = event.getText();
            if(!texts.isEmpty()) {
                String text = String.valueOf(texts.get(0));
                notificationEvent(text, (Notification) data);
            }
        } else if(eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            openHongBao(event);
        } else if(eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            if(mCurrentWindow != WINDOW_LAUNCHER) { //不在聊天界面或聊天列表，不处理
                return;
            }
            if(isReceivingHongbao) {
                handleChatListHongBao();
            }
        }
    }

    /** 通知栏事件*/
    private void notificationEvent(String ticker, Notification nf) {
        String text = ticker;
        int index = text.indexOf(":");
        if(index != -1) {
            text = text.substring(index + 1);
        }
        text = text.trim();
        if(text.contains(HONGBAO_TEXT_KEY)) { //红包消息
            newHongBaoNotification(nf);
        }
    }

    /** 打开通知栏消息*/
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void newHongBaoNotification(Notification notification) {
        isReceivingHongbao = true;
        //以下是精华，将微信的通知栏消息打开
        PendingIntent pendingIntent = notification.contentIntent;
        boolean lock = NotifyHelper.isLockScreen(getContext());

        if(!lock) {
            NotifyHelper.send(pendingIntent);
        } else {
            NotifyHelper.showNotify(getContext(), String.valueOf(notification.tickerText), pendingIntent);
        }

        if(lock || getConfig().getWechatMode() != Config.WX_MODE_0) {
            NotifyHelper.playEffect(getContext(), getConfig());
        }
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void openHongBao(AccessibilityEvent event) {
        if(hasGetMoney){
            hasGetMoney = false;
            AccessibilityHelper.performHome(getService());
        } else if("cooperation.qwallet.plugin.QWalletPluginProxyActivity".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LUCKYMONEY_DETAIL;
            if(!hasGetMoney) {
                hasGetMoney = true;
                AccessibilityHelper.performBack(getService());
            }
        }else if("com.tencent.mobileqq.activity.SplashActivity".equals(event.getClassName())) {
            mCurrentWindow = WINDOW_LAUNCHER;
            //在聊天界面,去点中红包
            handleChatListHongBao();
        } else {
            mCurrentWindow = WINDOW_OTHER;
        }
    }


    /**
     * 收到聊天里的红包
     * */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    private void handleChatListHongBao() {

        AccessibilityNodeInfo nodeInfo = getService().getRootInActiveWindow();
        if(nodeInfo == null) {
            Log.w(TAG, "rootWindow为空");
            return;
        }

        List<AccessibilityNodeInfo> list = nodeInfo.findAccessibilityNodeInfosByText("点击拆开");

        if(list != null && list.isEmpty()) {
            // 从消息列表查找红包
            AccessibilityNodeInfo node = AccessibilityHelper.findNodeInfosByText(nodeInfo, HONGBAO_TEXT_KEY);
            if(node != null) {
                if(BuildConfig.DEBUG) {
                    Log.i(TAG, "-->QQ红包:" + node);
                }
                isReceivingHongbao = true;
                AccessibilityHelper.performClick(nodeInfo);
            }else{
                AccessibilityNodeInfo messageNode = AccessibilityHelper.findNodeInfosByText(nodeInfo, "口令红包");
                if(needInput){
                    AccessibilityNodeInfo clickNode = AccessibilityHelper.findNodeInfosByText(nodeInfo, "点击输入口令");
                    if(clickNode != null){
                        AccessibilityHelper.performClick(clickNode);
                        needSend = true;
                    }else if(needSend){
                        needInput = false;
                        needSend = false;
                        AccessibilityNodeInfo sendNode = AccessibilityHelper.findNodeInfosByText(nodeInfo, "发送");
                        if(sendNode != null){
                            AccessibilityHelper.performClick(sendNode);
                            isReceivingHongbao = false;
                        }
                    }
                } else if(messageNode != null) {
                        if(BuildConfig.DEBUG) {
                            Log.i(TAG, "-->QQ红包:" + node);
                        }
                        needInput = true;
                        AccessibilityHelper.performClick(messageNode);
                    }
                }
        } else if(list != null) {
            if (isReceivingHongbao){
                //最新的红包领起
                AccessibilityNodeInfo node = list.get(list.size() - 1);
                AccessibilityHelper.performClick(node);
                isReceivingHongbao = false;
            }
        }


    }

    private Handler getHandler() {
        if(mHandler == null) {
            mHandler = new Handler(Looper.getMainLooper());
        }
        return mHandler;
    }


}
