package com.harmoni.pos.order;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.harmoni.pos.order.util.LanguageManager;

/**
 * Activity base that pins its resources to the app's chosen language. Every activity
 * in the app should extend this, so attaching a different base context is enough to
 * make the whole screen use the language selected in Settings.
 */
public class LocalizedActivity extends AppCompatActivity {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LanguageManager.wrap(newBase));
    }

    /** Recreate the activity after the language changes, so all views pick it up. */
    public void applyLanguageChange() {
        recreate();
    }

    @NonNull
    public Context localizedContext() {
        return LanguageManager.wrap(this);
    }
}
