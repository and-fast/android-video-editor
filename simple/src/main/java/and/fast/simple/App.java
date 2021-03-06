package and.fast.simple;

import android.app.Application;
import android.content.Context;

import and.fast.video.eidtor.utils.UIUtils;
import androidx.multidex.MultiDex;

/**
 * @author LLhon
 * @Project Android-Video-Editor
 * @Package com.marvhong.videoeditor
 * @Date 2018/8/21 16:00
 * @description
 */
public class App extends Application {

    public static Context sApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        sApplication = getApplicationContext();
        UIUtils.register(this);
        MultiDex.install(this);
    }
}
