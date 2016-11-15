package study.lastwarmth.me.socketdemo;

import android.app.Application;

/**
 * Created by Jaceli on 2016-11-04.
 */

public class MyApplication extends Application {

    private static MyApplication mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static MyApplication getApplication() {
        return mContext;
    }
}
