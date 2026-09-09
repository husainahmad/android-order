package com.harmoni.pos.order;

import android.app.Application;
import android.content.Context;

import com.harmoni.pos.order.data.remote.ConfigManager;
import com.harmoni.pos.order.data.remote.TokenManager;
import com.harmoni.pos.order.print.PrinterManager;

public class PosApplication extends Application {

    // No static instance leak. Use context.getApplicationContext() via DI / AndroidViewModel.
    // Kept only for legacy callers that still use PosApplication.getAppContext() during migration.
    private static volatile PosApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        TokenManager.init(this);
        ConfigManager.init(this);
        PrinterManager.init(this);
    }

    /**
     * @deprecated Inject Context or pass Application instead. Kept for incremental migration.
     */
    @Deprecated
    public static Context getAppContext() {
        PosApplication i = instance;
        if (i != null) return i.getApplicationContext();
        throw new IllegalStateException("PosApplication not created yet");
    }
}
