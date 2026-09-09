package com.harmoni.pos.order.data.remote;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Centralized 401/403 session-expiration bus.
 *
 * <p>ApiClient's SessionExpiredInterceptor posts here from OkHttp threads.
 * MainActivity observes and navigates to login exactly once per expiration burst
 * (throttled to avoid N dialogs when N parallel requests get 403).
 */
public final class SessionManager {

    private static final MutableLiveData<String> EXPIRED_EVENT = new MutableLiveData<>();
    // Throttle: only one dialog per burst; reset after UI handles it or after timeout
    private static final AtomicBoolean PENDING = new AtomicBoolean(false);
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static final long RESET_DELAY_MS = 2000L;

    private SessionManager() {}

    public static LiveData<String> getSessionExpiredEvent() {
        return EXPIRED_EVENT;
    }

    /**
     * Called from OkHttp interceptor (background thread safe via postValue).
     *
     * @param debugMessage optional debug, e.g. "Forbidden (403)" – shown to user in a friendly form
     */
    public static void notifySessionExpired(@Nullable String debugMessage) {
        if (!PENDING.compareAndSet(false, true)) {
            return; // already notifying – suppress duplicate dialogs in same burst
        }
        String msg = debugMessage != null ? debugMessage : "Session expired";
        // postValue is safe from any thread
        EXPIRED_EVENT.postValue(msg);
        // Auto-reset throttle so a *new* expiration later can still be shown
        MAIN.postDelayed(() -> PENDING.set(false), RESET_DELAY_MS);
    }

    public static void notifyForbidden() {
        notifySessionExpired("Access denied (403)");
    }

    /** Call after UI has handled the event to allow next burst immediately. */
    public static void onHandled() {
        EXPIRED_EVENT.setValue(null);
        // Keep PENDING throttled for RESET_DELAY_MS to absorb parallel 403s,
        // but allow immediate reset if UI explicitly wants it:
        // PENDING will auto-reset via delayed post; we don't clear it eagerly.
    }

    /** Force reset throttle (e.g. after successful login). */
    public static void resetThrottle() {
        PENDING.set(false);
        MAIN.removeCallbacksAndMessages(null);
        EXPIRED_EVENT.setValue(null);
    }

    /** Convenience: post on main thread with explicit message. */
    public static void notifyOnMain(@NonNull String message) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            notifySessionExpired(message);
        } else {
            MAIN.post(() -> notifySessionExpired(message));
        }
    }
}
