package and.fast.simple.ui.activity;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.tbruyelle.rxpermissions2.RxPermissions;

import and.fast.simple.base.BaseActivity;
import and.fast.simple.helper.ToolbarHelper;
import and.fast.simple.videoe.R;
import and.fast.video.eidtor.Constant;
import and.fast.video.eidtor.model.TrimVideoConfigModel;
import and.fast.video.eidtor.ui.TrimVideoActivity;
import and.fast.video.eidtor.ui.VideoCameraActivity;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

public class MainActivity extends BaseActivity {

    private TextView  mTvPath;
    private ImageView mIvCover;

    private RxPermissions mRxPermissions;

    @Override
    protected int getLayoutId() {
        return R.layout.activity_main;
    }

    @Override
    protected void initToolbar(ToolbarHelper toolbarHelper) {
        toolbarHelper.setTitle("视频编辑");
        toolbarHelper.hideBackArrow();
    }

    @Override
    protected void initView() {
        mRxPermissions = new RxPermissions(this);
        mTvPath = findViewById(R.id.tv_path);
        mIvCover = findViewById(R.id.iv_cover);
    }

    public void takeCamera(View view) {
        mRxPermissions
                .request(permission.WRITE_EXTERNAL_STORAGE, permission.RECORD_AUDIO, permission.CAMERA)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        subscribe(d);
                    }

                    @Override
                    public void onNext(Boolean granted) {
                        if (granted) { //已获取权限
                            //Intent intent = new Intent(MainActivity.this, VideoCameraActivity.class);
                            TrimVideoConfigModel trimModel = new TrimVideoConfigModel();
                            trimModel.setMinDuration(3);
                            trimModel.setMaxDuration(15);
                            startActivityForResult(VideoCameraActivity.newIntent(MainActivity.this, trimModel), 101);
                        } else {
                            Toast.makeText(MainActivity.this, "给点权限行不行？", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    public void takeAlbum(View view) {
        mRxPermissions
                .request(permission.WRITE_EXTERNAL_STORAGE, permission.READ_EXTERNAL_STORAGE)
                .subscribe(new Observer<Boolean>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        subscribe(d);
                    }

                    @Override
                    public void onNext(Boolean granted) {
                        if (granted) { //已获取权限
                            Intent intent = new Intent(MainActivity.this, VideoAlbumActivity.class);
                            startActivityForResult(intent, 100);
                        } else {
                            Toast.makeText(MainActivity.this, "给点权限行不行？", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) { // 选择视频
            String path = data.getStringExtra("result");
            TrimVideoConfigModel model = new TrimVideoConfigModel();
            model.setPath(path);
            model.setMinDuration(3);
            model.setMaxDuration(15);
            startActivityForResult(TrimVideoActivity.newIntent(this, model), 101);

        } else if (requestCode == 101 && resultCode == Activity.RESULT_OK) {
            mTvPath.setText(data.getStringExtra(Constant.RESULT_VIDEO_PATH));
            String firstFrame = data.getStringExtra(Constant.RESULT_FIRST_FRAME_IMAGE_PATH);
            Glide.with(this).load(firstFrame).into(mIvCover);
        }
    }
}
