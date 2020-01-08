package and.fast.video.eidtor.utils;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.text.TextUtils;

import java.io.File;

/**
 * 提取视频信息
 */
public class ExtractVideoInfoUtil {

    private long mFileLength = 0;

    private MediaMetadataRetriever mMetadataRetriever;

    public ExtractVideoInfoUtil(String path) {
        if (TextUtils.isEmpty(path)) {
            throw new RuntimeException("路径不能为空");
        }

        File file = new File(path);
        if (!file.exists()) {
            throw new RuntimeException("文件路径不存在");
        }

        mMetadataRetriever = new MediaMetadataRetriever();
        mMetadataRetriever.setDataSource(path);
    }

    // 获取视频的典型的一帧图片，不耗时
    public Bitmap getFrameAtTime() {
        return mMetadataRetriever.getFrameAtTime();
    }

    // 获取视频某一帧,不一定是关键帧
    public Bitmap getFrameAtTime(long timeMs) {
        Bitmap bitmap = null;

        for (long i = timeMs; i < mFileLength; i += 1000) {
            bitmap = mMetadataRetriever.getFrameAtTime(i * 1000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (bitmap != null) {
                break;
            }
        }

        return bitmap;
    }

    // 获取视频的长度时间
    public String getVideoLength() {
        String len = mMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        mFileLength = TextUtils.isEmpty(len) ? 0 : Long.valueOf(len);
        return len;
    }

    // 释文资源
    public void release() {
        if (mMetadataRetriever != null) {
            mMetadataRetriever.release();
        }
    }

}
