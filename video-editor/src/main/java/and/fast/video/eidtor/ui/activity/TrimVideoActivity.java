package and.fast.video.eidtor.ui.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.cjt2325.cameralibrary.R;
import com.cjt2325.cameralibrary.util.FileUtil;
import com.iceteck.silicompressorr.SiliCompressor;
import com.marvhong.videoeffect.FillMode;
import com.marvhong.videoeffect.GlVideoView;
import com.marvhong.videoeffect.composer.Mp4Composer;
import com.marvhong.videoeffect.helper.MagicFilterFactory;
import com.marvhong.videoeffect.helper.MagicFilterType;
import com.marvhong.videoeffect.utils.ConfigUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.math.BigDecimal;

import and.fast.video.eidtor.Constant;
import and.fast.video.eidtor.adapter.TrimVideoAdapter;
import and.fast.video.eidtor.model.VideoEditInfo;
import and.fast.video.eidtor.utils.ExtractFrameWorkThread;
import and.fast.video.eidtor.utils.ExtractVideoInfoUtil;
import and.fast.video.eidtor.utils.UIUtils;
import and.fast.video.eidtor.utils.VideoUtil;
import and.fast.video.eidtor.view.NormalProgressDialog;
import and.fast.video.eidtor.view.RangeSeekBar;
import and.fast.video.eidtor.view.VideoThumbSpacingItemDecoration;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class TrimVideoActivity extends AppCompatActivity {

    public static void startActivity(Context context, String videoPath) {
        Intent intent = new Intent(context, TrimVideoActivity.class);
        intent.putExtra(Constant.EXT_VIDEO_PATH, videoPath);
        context.startActivity(intent);
    }

    private RelativeLayout mRlVideo;
    private TextView       mTvDuration;
//    private ImageView      mIvPosition;
    private GlVideoView    mSurfaceView;
    private RecyclerView   mRecyclerView;
    private LinearLayout   seekBarLayout;
    private TextView       mTvEndDuration;
    private TextView       mTvStartDuration;

    private RangeSeekBar seekBar;

    private static final String TAG = TrimVideoActivity.class.getSimpleName();

    private static final long MIN_CUT_DURATION = 3 * 1000L;// 最小剪辑时间3s
    private static final long MAX_CUT_DURATION = 60 * 1000L;// 视频最多剪切多长时间
    private static final int  MAX_COUNT_RANGE  = 10;   // seekBar的区域内一共有多少张图片
    private static final int  MARGIN           = UIUtils.dp2Px(56); // 左右两边间距

    private int    mMaxWidth; // 可裁剪区域的最大宽度
    private long   duration; // 视频总时长
    private float  averageMsPx;// 每毫秒所占的px
    private float  averagePxMs;// 每px所占用的ms毫秒
    private String OutPutFileDirPath;
    private long   leftProgress, rightProgress; // 裁剪视频左边区域的时间位置, 右边时间位置
    private long    scrollPos = 0;
    private int     mScaledTouchSlop;
    private int     lastScrollX;
    private boolean isSeeking;
    private String  mVideoPath;
    private int     mOriginalWidth, mOriginalHeight; // 视频原始宽高

    private MediaPlayer            mMediaPlayer;
    private Mp4Composer            mMp4Composer;
    private TrimVideoAdapter       videoEditAdapter;
    private ExtractVideoInfoUtil   mExtractVideoInfoUtil;
    private ExtractFrameWorkThread mExtractFrameWorkThread;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 状态栏
        initStatusBar();

        // 设置视图
        setContentView(R.layout.activity_trim_video);

        // 初始化参数
        init();

        // 初始化 view
        initViews();
    }

    private void initStatusBar() {
        if (Build.VERSION.SDK_INT >= 21) {
            getWindow().setStatusBarColor(Color.BLACK);
        }
    }

    private void initViews() {
        mSurfaceView = findViewById(R.id.glsurfaceview);
        mRecyclerView = findViewById(R.id.video_thumb_listview);
//        mIvPosition = findViewById(R.id.positionIcon);
        seekBarLayout = findViewById(R.id.id_seekBarLayout);
        mRlVideo = findViewById(R.id.layout_surface_view);
        mTvDuration = findViewById(R.id.tv_duration);
        mTvStartDuration = findViewById(R.id.tv_start_duration);
        mTvEndDuration = findViewById(R.id.tv_end_duration);

        // 确定裁剪
        findViewById(R.id.tv_sure).setOnClickListener(view -> trimmerVideo());

        // 预览列表
        videoEditAdapter = new TrimVideoAdapter(this, mMaxWidth / 10);
        mRecyclerView.setAdapter(videoEditAdapter);
        mRecyclerView.addOnScrollListener(mOnScrollListener);

        // 初始化视频
        mSurfaceView.init(this::initMediaPlayer);
    }

    protected void init() {
        mVideoPath = getIntent().getStringExtra(Constant.EXT_VIDEO_PATH);
        mExtractVideoInfoUtil = new ExtractVideoInfoUtil(mVideoPath);
        mMaxWidth = UIUtils.getScreenWidth() - MARGIN * 2;
        mScaledTouchSlop = ViewConfiguration.get(this).getScaledTouchSlop();

        // 获取视频时长
        Observable.create((ObservableOnSubscribe<String>) e -> {
            e.onNext(mExtractVideoInfoUtil.getVideoLength());
            e.onComplete();
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        // subscribe(d);
                    }

                    @Override
                    public void onNext(String s) {
                        duration = Long.valueOf(mExtractVideoInfoUtil.getVideoLength());

                        // 矫正获取到的视频时长不是整数问题
                        float tempDuration = duration / 1000f;
                        duration = new BigDecimal(tempDuration).setScale(0, BigDecimal.ROUND_HALF_UP).intValue() * 1000;
                        mTvDuration.setText(getGapTime(duration));

                        initEditVideo();
                    }

                    @Override
                    public void onError(Throwable e) {

                    }

                    @Override
                    public void onComplete() {

                    }

                });
    }

    private void initEditVideo() {
        boolean isOver_10_s;
        int thumbnailsCount, rangeWidth;
        long startPosition = 0, endPosition = duration;

        if (endPosition <= MAX_CUT_DURATION) {
            isOver_10_s = false;
            thumbnailsCount = MAX_COUNT_RANGE;
            rangeWidth = mMaxWidth;

        } else {
            isOver_10_s = true;
            thumbnailsCount = (int) (endPosition * 1.0f / (MAX_CUT_DURATION * 1.0f) * MAX_COUNT_RANGE);
            rangeWidth = mMaxWidth / MAX_COUNT_RANGE * thumbnailsCount;
        }

        mRecyclerView.addItemDecoration(new VideoThumbSpacingItemDecoration(MARGIN, thumbnailsCount));

        // init seekBar
        if (isOver_10_s) {
            seekBar = new RangeSeekBar(this, 0L, MAX_CUT_DURATION);
            seekBar.setSelectedMinValue(0L);
            seekBar.setSelectedMaxValue(MAX_CUT_DURATION);

        } else {
            seekBar = new RangeSeekBar(this, 0L, endPosition);
            seekBar.setSelectedMinValue(0L);
            seekBar.setSelectedMaxValue(endPosition);
        }

        // 设置最小裁剪时间
        seekBar.setMin_cut_time(MIN_CUT_DURATION);
        seekBar.setNotifyWhileDragging(true);
        seekBar.setOnRangeSeekBarChangeListener(mOnRangeSeekBarChangeListener);
        seekBarLayout.addView(seekBar);
        averageMsPx = duration * 1.0f / rangeWidth * 1.0f;
        OutPutFileDirPath = VideoUtil.getSaveEditThumbnailDir(this);
        int extractW = mMaxWidth / MAX_COUNT_RANGE;
        int extractH = UIUtils.dp2Px(62);

        // 开始提取视频帧
        mExtractFrameWorkThread = new ExtractFrameWorkThread(extractW, extractH, mUIHandler,
                mVideoPath, OutPutFileDirPath, startPosition, endPosition, thumbnailsCount);
        mExtractFrameWorkThread.start();

        // init pos icon start
        leftProgress = 0;
        if (isOver_10_s) {
            rightProgress = MAX_CUT_DURATION;
        } else {
            rightProgress = endPosition;
        }

        averagePxMs = (mMaxWidth * 1.0f / (rightProgress - leftProgress));
        mTvStartDuration.setText(getGapTime(leftProgress));
        mTvEndDuration.setText(getGapTime(rightProgress));
    }

    /**
     * 初始化MediaPlayer
     */
    private void initMediaPlayer(SurfaceTexture surfaceTexture) {
        mMediaPlayer = new MediaPlayer();

        try {
            mMediaPlayer.setDataSource(mVideoPath);
            Surface surface = new Surface(surfaceTexture);
            mMediaPlayer.setSurface(surface);
            surface.release();
            mMediaPlayer.setLooping(true);

            mMediaPlayer.setOnPreparedListener(mp -> {
                ViewGroup.LayoutParams lp = mSurfaceView.getLayoutParams();
                int videoWidth = mp.getVideoWidth();
                int videoHeight = mp.getVideoHeight();
                float videoProportion = (float) videoWidth / (float) videoHeight;
                int screenWidth = mRlVideo.getWidth();
                int screenHeight = mRlVideo.getHeight();
                float screenProportion = (float) screenWidth / (float) screenHeight;

                if (videoProportion > screenProportion) {
                    lp.width = screenWidth;
                    lp.height = (int) ((float) screenWidth / videoProportion);
                } else {
                    lp.width = (int) (videoProportion * (float) screenHeight);
                    lp.height = screenHeight;
                }

                mSurfaceView.setLayoutParams(lp);
                mOriginalWidth = videoWidth;
                mOriginalHeight = videoHeight;

                // 设置MediaPlayer的OnSeekComplete监听
                mp.setOnSeekCompleteListener(mp1 -> {
                    if (!isSeeking) {
                        videoStart();
                    }
                });
            });

            mMediaPlayer.prepare();
            videoStart();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 视频裁剪
     */
    private void trimmerVideo() {
        NormalProgressDialog.showLoading(this, getResources().getString(R.string.in_process), false);
        videoPause();

        VideoUtil.cutVideo(mVideoPath,
                VideoUtil.getTrimmedVideoPath(this, Constant.TRIMMER_VIDEO_DIR_NAME, Constant.TRIMMER_VIDEO_DIR_NAME_PREFIX),
                leftProgress / 1000,
                rightProgress / 1000)
                .subscribe(new Observer<String>() {

                    @Override
                    public void onSubscribe(Disposable d) {
                        // subscribe(d); TODO
                    }

                    @Override
                    public void onNext(String outputPath) {
                        try {
                            startMediaCodec(outputPath);
                        } catch (Exception e) {
                            onError(e);
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        NormalProgressDialog.stopLoading();
                        Toast.makeText(TrimVideoActivity.this, "视频裁剪失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {

                    }

                });
    }

    /**
     * 视频添加滤镜效果
     */
    private void startMediaCodec(String srcPath) {
        final String outputPath = VideoUtil.getTrimmedVideoPath(this, Constant.TRIMMER_VIDEO_DIR_NAME, Constant.TRIMMER_VIDEO_DIR_NAME_PREFIX);

        mMp4Composer = new Mp4Composer(srcPath, outputPath)
                .fillMode(FillMode.PRESERVE_ASPECT_FIT)
                .filter(MagicFilterFactory.getFilter())
                .mute(false)
                .flipHorizontal(false)
                .flipVertical(false)
                .listener(new Mp4Composer.Listener() {

                    @Override
                    public void onProgress(double progress) {
                        Log.d(TAG, "filterVideo---onProgress: " + (int) (progress * 100));
                    }

                    @Override
                    public void onCompleted() {
                        runOnUiThread(() -> {
                            compressVideo(outputPath);
                        });
                    }

                    @Override
                    public void onCanceled() {
                        NormalProgressDialog.stopLoading();
                    }

                    @Override
                    public void onFailed(Exception exception) {
                        NormalProgressDialog.stopLoading();
                        Toast.makeText(TrimVideoActivity.this, "视频处理失败", Toast.LENGTH_SHORT).show();
                    }

                })
                .start();
    }

    /**
     * 视频压缩
     */
    private void compressVideo(String srcPath) {
        String destDirPath = VideoUtil.getTrimmedVideoDir(this, Constant.COMPRESS_VIDEO_DIR_NAME);
        Observable.create((ObservableOnSubscribe<String>) emitter -> {
            try {

                int outWidth, outHeight;

                if (mOriginalWidth > mOriginalHeight) {
                    //横屏
                    outWidth = 720;
                    outHeight = 480;
                } else {
                    //竖屏
                    outWidth = 480;
                    outHeight = 720;
                }

                String compressedFilePath = SiliCompressor
                        .with(TrimVideoActivity.this)
                        .compressVideo(srcPath, destDirPath, outWidth, outHeight, 900000);

                emitter.onNext(compressedFilePath);
            } catch (Exception e) {
                emitter.onError(e);
            }

            emitter.onComplete();
        })
                .subscribeOn(Schedulers.newThread())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<String>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        // subscribe(d); TODO
                    }

                    @Override
                    public void onNext(String outputPath) {

                        //源路径: /storage/emulated/0/Android/data/com.kangoo.diaoyur/cache/small_video/trimmedVideo_20180514_163858.mp4
                        //压缩路径: /storage/emulated/0/Android/data/com.kangoo.diaoyur/cache/small_video/VIDEO_20180514_163859.mp4

                        // 获取视频第一帧图片
                        mExtractVideoInfoUtil = new ExtractVideoInfoUtil(outputPath);
                        Bitmap bitmap = mExtractVideoInfoUtil.extractFrame();
                        String firstFrame = FileUtil.saveBitmap(Constant.COMPRESS_VIDEO_DIR_NAME, bitmap);

                        if (!bitmap.isRecycled()) {
                            bitmap.recycle();
                        }

                        NormalProgressDialog.stopLoading();
                        // VideoPreviewActivity.startActivity(TrimVideoActivity.this, outputPath, firstFrame);
                        finish();
                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                        NormalProgressDialog.stopLoading();
                        Toast.makeText(TrimVideoActivity.this, "视频压缩失败", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onComplete() {
                    }
                });
    }

    private boolean isOverScaledTouchSlop;

    private final RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {

        @Override
        public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
            super.onScrollStateChanged(recyclerView, newState);
            if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                isSeeking = false;
                videoStart();
            } else {
                isSeeking = true;
                if (isOverScaledTouchSlop) {
                    videoPause();
                }
            }
        }

        @Override
        public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
            super.onScrolled(recyclerView, dx, dy);
            isSeeking = false;
            int scrollX = getScrollXDistance();
            //达不到滑动的距离
            if (Math.abs(lastScrollX - scrollX) < mScaledTouchSlop) {
                isOverScaledTouchSlop = false;
                return;
            }

            isOverScaledTouchSlop = true;
            // 初始状态,why ? 因为默认的时候有56dp的空白！
            if (scrollX == -MARGIN) {
                scrollPos = 0;

            } else {
                // why 在这里处理一下,因为onScrollStateChanged早于onScrolled回调
                videoPause();
                isSeeking = true;
                scrollPos = (long) (averageMsPx * (MARGIN + scrollX));
                leftProgress = seekBar.getSelectedMinValue() + scrollPos;
                rightProgress = seekBar.getSelectedMaxValue() + scrollPos;
                mMediaPlayer.seekTo((int) leftProgress);
            }

            lastScrollX = scrollX;
        }
    };

    /**
     * 水平滑动了多少px
     */
    private int getScrollXDistance() {
        LinearLayoutManager layoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
        int position = layoutManager.findFirstVisibleItemPosition();
        View firstVisibleChildView = layoutManager.findViewByPosition(position);
        int itemWidth = firstVisibleChildView.getWidth();
        return (position) * itemWidth - firstVisibleChildView.getLeft();
    }

//    private ValueAnimator animator;

    // 扫描动画
//    private void anim() {
//        if (mIvPosition.getVisibility() == View.GONE) {
//            mIvPosition.setVisibility(View.VISIBLE);
//        }
//
//        final FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) mIvPosition.getLayoutParams();
//        int start = (int) (MARGIN + (leftProgress - scrollPos) * averagePxMs);
//        int end = (int) (MARGIN + (rightProgress - scrollPos) * averagePxMs);
//        animator = ValueAnimator
//                .ofInt(start, end)
//                .setDuration((rightProgress - scrollPos) - (leftProgress - scrollPos));
//        animator.setInterpolator(new LinearInterpolator());
//        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
//
//            @Override
//            public void onAnimationUpdate(ValueAnimator animation) {
//                params.leftMargin = (int) animation.getAnimatedValue();
//                mIvPosition.setLayoutParams(params);
//            }
//
//        });
//
//        animator.start();
//    }

    private final MainHandler mUIHandler = new MainHandler(this);

    private static class MainHandler extends Handler {

        private final WeakReference<TrimVideoActivity> mActivity;

        MainHandler(TrimVideoActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            TrimVideoActivity activity = mActivity.get();
            if (activity != null) {
                if (msg.what == ExtractFrameWorkThread.MSG_SAVE_SUCCESS) {
                    if (activity.videoEditAdapter != null) {
                        VideoEditInfo info = (VideoEditInfo) msg.obj;
                        activity.videoEditAdapter.addItemVideoInfo(info);
                    }
                }
            }
        }
    }

    // 视频滚动
    private final RangeSeekBar.OnRangeSeekBarChangeListener mOnRangeSeekBarChangeListener = new RangeSeekBar.OnRangeSeekBarChangeListener() {

        @Override
        public void onRangeSeekBarValuesChanged(
                RangeSeekBar bar, long minValue, long maxValue, int action, boolean isMin, RangeSeekBar.Thumb pressedThumb) {

            leftProgress = minValue + scrollPos;
            rightProgress = maxValue + scrollPos;
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                    isSeeking = false;
                    videoPause();
                    break;
                case MotionEvent.ACTION_MOVE:
                    isSeeking = true;
                    mMediaPlayer.seekTo((int) (pressedThumb == RangeSeekBar.Thumb.MIN ? leftProgress : rightProgress));
                    break;
                case MotionEvent.ACTION_UP:
                    isSeeking = false;
                    //从minValue开始播
                    mMediaPlayer.seekTo((int) leftProgress);
                    mTvStartDuration.setText(getGapTime(leftProgress));
                    mTvEndDuration.setText(getGapTime(rightProgress));
                    break;
                default:
                    break;
            }
        }
    };

    // 获取时、分、秒
    @SuppressLint("DefaultLocale")
    public String getGapTime(long millis) {
        long second = (millis / 1000) % 60;
        long minute = (millis / (1000 * 60)) % 60;
        long hour = (millis / (1000 * 60 * 60)) % 24;

        return hour > 0
                ? String.format("%02d:%02d:%02d", hour, minute, second)
                : String.format("%02d:%02d", minute, second);
    }

    private void videoStart() {
        mMediaPlayer.start();
//        mIvPosition.clearAnimation();

//        if (animator != null && animator.isRunning()) {
//            animator.cancel();
//        }

//        anim();
//        handler.removeCallbacks(run);
//        handler.post(run);
    }

//    private void videoProgressUpdate() {
//        long currentPosition = mMediaPlayer.getCurrentPosition();
//        if (currentPosition >= (rightProgress)) {
//            mMediaPlayer.seekTo((int) leftProgress);
//            mIvPosition.clearAnimation();
//            if (animator != null && animator.isRunning()) {
//                animator.cancel();
//            }
//            anim();
//        }
//    }

    private void videoPause() {
        isSeeking = false;
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
//            handler.removeCallbacks(run);
        }

//        if (mIvPosition.getVisibility() == View.VISIBLE) {
//            mIvPosition.setVisibility(View.GONE);
//        }

//        mIvPosition.clearAnimation();
//        if (animator != null && animator.isRunning()) {
//            animator.cancel();
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo((int) leftProgress);
            videoStart();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoPause();
    }

    // 重复动画
//    private Handler  handler = new Handler();
//    private Runnable run     = new Runnable() {
//
//        @Override
//        public void run() {
//            videoProgressUpdate();
//            handler.postDelayed(run, 1000);
//        }
//    };

    @Override
    protected void onDestroy() {
        NormalProgressDialog.stopLoading();
        ConfigUtils.getInstance().setMagicFilterType(MagicFilterType.NONE);

//        if (animator != null) {
//            animator.cancel();
//        }

        if (mMediaPlayer != null) {
            mMediaPlayer.release();
        }

        if (mMp4Composer != null) {
            mMp4Composer.cancel();
        }

        if (mExtractVideoInfoUtil != null) {
            mExtractVideoInfoUtil.release();
        }

        if (mExtractFrameWorkThread != null) {
            mExtractFrameWorkThread.stopExtract();
        }

        mRecyclerView.removeOnScrollListener(mOnScrollListener);
        mUIHandler.removeCallbacksAndMessages(null);
//        handler.removeCallbacksAndMessages(null);

        //删除视频每一帧的预览图
        if (!TextUtils.isEmpty(OutPutFileDirPath)) {
            VideoUtil.deleteFile(new File(OutPutFileDirPath));
        }

        //删除裁剪后的视频，滤镜视频
        String trimmedDirPath = VideoUtil.getTrimmedVideoDir(this, Constant.TRIMMER_VIDEO_DIR_NAME);
        if (!TextUtils.isEmpty(trimmedDirPath)) {
            // TODO
            // VideoUtil.deleteFile(new File(trimmedDirPath));
        }

        super.onDestroy();
    }
}
