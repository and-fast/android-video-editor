package and.fast.video.eidtor.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.File;

import and.fast.video.editor.R;
import and.fast.video.eidtor.Constant;
import and.fast.video.eidtor.model.TrimVideoConfigModel;
import and.fast.video.eidtor.utils.VideoUtil;
import and.fast.video.record.JCameraView;
import and.fast.video.record.listener.ErrorListener;
import and.fast.video.record.listener.JCameraListener;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class VideoCameraActivity extends AppCompatActivity {

    public static Intent newIntent(Context context, TrimVideoConfigModel trimModel) {
        return new Intent(context, VideoCameraActivity.class)
                .putExtra(Constant.EXT_TRIM_CONFIG_MODEL, trimModel);
    }

    private JCameraView mJCameraView;

    private int mMinDuration = 3, mMaxDuration = 15;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 参数
        TrimVideoConfigModel model = getIntent().getParcelableExtra(Constant.EXT_TRIM_CONFIG_MODEL);
        mMinDuration = model.getMinDuration();
        mMaxDuration = model.getMaxDuration();

        // 方向设置
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        // 设置视图
        setContentView(R.layout.activity_video_camera);

        // 初始化视图
        initViews();
    }

    protected void initViews() {
        mJCameraView = findViewById(R.id.jcameraview);

        // 设置视频保存路径
        mJCameraView.setSaveVideoPath(
                Environment.getExternalStorageDirectory().getPath()
                        + File.separator
                        + "videoeditor"
                        + File.separator
                        + Constant.COMPRESS_VIDEO_DIR_NAME
        );

        mJCameraView.setMinDuration(mMinDuration * 1000); //设置最短录制时长
        mJCameraView.setDuration(mMaxDuration * 1000); //设置最长录制时长
        mJCameraView.setFeatures(JCameraView.BUTTON_STATE_ONLY_RECORDER);
        mJCameraView.setTip(String.format("长按拍摄, %d~%d秒", mMinDuration, mMaxDuration));
        mJCameraView.setRecordShortTip(String.format("录制时间%d~%d秒", mMaxDuration, mMaxDuration));
        mJCameraView.setMediaQuality(JCameraView.MEDIA_QUALITY_MIDDLE);

        // 失败回调
        mJCameraView.setErrorLisenter(new ErrorListener() {

            @Override
            public void onError() {
                finish();
            }

            @Override
            public void AudioPermissionError() {
                Toast.makeText(VideoCameraActivity.this, "给点录音权限可以?", Toast.LENGTH_SHORT).show();
            }

        });

        // 成功回调
        mJCameraView.setJCameraLisenter(new JCameraListener() {

            @Override
            public void captureSuccess(Bitmap bitmap) {
                // 返回图片
            }

            @Override
            public void recordSuccess(String url, Bitmap firstFrame) {
                String firstFramePath = VideoUtil.saveImageToSD(firstFrame,
                        VideoUtil.getSaveEditThumbnailDir(VideoCameraActivity.this));

                setResult(Activity.RESULT_OK, new Intent()
                        .putExtra(Constant.RESULT_VIDEO_PATH, url)
                        .putExtra(Constant.RESULT_FIRST_FRAME_IMAGE_PATH, firstFramePath));
                finish();
            }

        });

        mJCameraView.setLeftClickListener(this::finish);

//        mJCameraView.setRightClickListener(() -> Toast.makeText(VideoCameraActivity.this, "Right", Toast.LENGTH_SHORT).show());

//        mJCameraView.setRecordStateListener(new RecordStateListener() {
//
//            @Override
//            public void recordStart() {
//
//            }
//
//            @Override
//            public void recordEnd(long time) {
//                Log.e("录制状态回调", "录制时长：" + time);
//            }
//
//            @Override
//            public void recordCancel() {
//                // 录音取消
//            }
//
//        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            );
        } else {
            View decorView = getWindow().getDecorView();
            int option = View.SYSTEM_UI_FLAG_FULLSCREEN;
            decorView.setSystemUiVisibility(option);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mJCameraView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mJCameraView.onPause();
    }
}
