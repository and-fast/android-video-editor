package and.fast.video.eidtor.widget;

import android.graphics.Rect;
import android.view.View;

import androidx.recyclerview.widget.RecyclerView;

public class VideoThumbSpacingItemDecoration extends RecyclerView.ItemDecoration {

    private int mSpace; //间距
    private int mThumbnailsCount; //缩略图item总数量

    public VideoThumbSpacingItemDecoration(int space, int thumbnailsCount) {
        mSpace = space;
        mThumbnailsCount = thumbnailsCount;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        int position = parent.getChildAdapterPosition(view);
        //第一个与最后一个添加空白间距
        if (position == 0) {
            outRect.left = mSpace;
            outRect.right = 0;
        } else if (mThumbnailsCount > 10 && position == mThumbnailsCount - 1) {
            outRect.left = 0;
            outRect.right = mSpace;
        } else {
            outRect.left = 0;
            outRect.right = 0;
        }
    }
}
