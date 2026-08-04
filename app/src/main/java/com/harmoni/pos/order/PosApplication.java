package com.harmoni.pos.order;

import android.app.Application;
import android.content.Context;

import com.harmoni.pos.order.data.remote.ConfigManager;
import com.harmoni.pos.order.data.remote.TokenManager;
import com.harmoni.pos.order.print.PrinterManager;

public class PosApplication extends Application {

    private static PosApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        TokenManager.init(this);
        ConfigManager.init(this);
        PrinterManager.init(this);
    }

    public static Context getAppContext() {
        return instance;
    }
}
