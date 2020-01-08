package and.fast.video.eidtor.model;

import android.os.Parcel;
import android.os.Parcelable;

public class TrimVideoConfigModel implements Parcelable {

    private int    minDuration; // 最小时长
    private int    maxDuration; // 最大时长
    private String path; // 视频路径

    public int getMinDuration() {
        return minDuration;
    }

    public void setMinDuration(int minDuration) {
        this.minDuration = minDuration;
    }

    public int getMaxDuration() {
        return maxDuration;
    }

    public void setMaxDuration(int maxDuration) {
        this.maxDuration = maxDuration;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public TrimVideoConfigModel() {
    }

    protected TrimVideoConfigModel(Parcel in) {
        minDuration = in.readInt();
        maxDuration = in.readInt();
        path = in.readString();
    }

    public static final Creator<TrimVideoConfigModel> CREATOR = new Creator<TrimVideoConfigModel>() {
        @Override
        public TrimVideoConfigModel createFromParcel(Parcel in) {
            return new TrimVideoConfigModel(in);
        }

        @Override
        public TrimVideoConfigModel[] newArray(int size) {
            return new TrimVideoConfigModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(minDuration);
        parcel.writeInt(maxDuration);
        parcel.writeString(path);
    }
}
