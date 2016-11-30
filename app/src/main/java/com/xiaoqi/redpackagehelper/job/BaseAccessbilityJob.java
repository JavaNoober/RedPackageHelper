package com.xiaoqi.redpackagehelper.job;

import android.content.Context;

import com.xiaoqi.redpackagehelper.Config;
import com.xiaoqi.redpackagehelper.QiangHongBaoService;

public abstract class BaseAccessbilityJob implements AccessbilityJob {

    private QiangHongBaoService service;

    @Override
    public void onCreateJob(QiangHongBaoService service) {
        this.service = service;
    }

    public Context getContext() {
        return service.getApplicationContext();
    }

    public Config getConfig() {
        return service.getConfig();
    }

    public QiangHongBaoService getService() {
        return service;
    }
}
