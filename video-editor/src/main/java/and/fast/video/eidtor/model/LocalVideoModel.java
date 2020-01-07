package and.fast.video.eidtor.model;

import android.content.Context;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;

import java.io.File;

public class LocalVideoModel {

    private long videoId;

    private String videoName = "";

    private String authorName = "";

    private String description = "";

    // 视频全路径,包含视频文件名的路径信息
    private String videoPath;

    //视频所在文件夹的路径
    private String videoFolderPath;

    private String createTime;

    private long duration = 0;

    private String thumbPath;

    private int rotate;

    private String lat;

    private String lon;

    public long getVideoId() {
        return videoId;
    }

    public void setVideoId(long videoId) {
        this.videoId = videoId;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVideoPath() {
        return videoPath;
    }

    public int getRotate() {
        return rotate;
    }

    public void setRotate(int rotate) {
        this.rotate = rotate;
    }

    public String getThumbPath() {
        return thumbPath;
    }

    public void setThumbPath(String thumbPath) {
        this.thumbPath = thumbPath;
    }

    public void setVideoPath(String videoPath) {
        this.videoPath = videoPath;
    }

    public String getVideoFolderPath() {
        return videoFolderPath;
    }

    public void setVideoFolderPath(String videoFolderPath) {
        this.videoFolderPath = videoFolderPath;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public static LocalVideoModel buildVideo(Context context, String videoPath) {
        LocalVideoModel info = new LocalVideoModel();
        info.setVideoPath(videoPath);

        try {
            MediaPlayer mp = MediaPlayer.create(context, Uri.fromFile(new File(videoPath)));
            if (mp != null) {
                info.setDuration(mp.getDuration());
                mp.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return info;
    }

    public LocalVideoModel calcDuration() {
        try {
            MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
            mediaMetadataRetriever.setDataSource(getVideoPath());
            String time = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
            duration = Long.parseLong(time);

        } catch (Exception e) {
            e.printStackTrace();
        }

        return this;
    }
}
