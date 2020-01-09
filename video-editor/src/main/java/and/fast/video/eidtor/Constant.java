package and.fast.video.eidtor;

/**
 * Desc: 常量
 */
public interface Constant {

    String EXT_TRIM_CONFIG_MODEL = "TRIM_CONFIG_MODEL";

    // 压缩视频
    String COMPRESS_VIDEO_DIR_NAME = "small_video";

    // 裁剪视频
    String TRIMMER_VIDEO_DIR_NAME = "small_video/trimmedVideo";

    // 裁剪视频前缀
    String TRIMMER_VIDEO_DIR_NAME_PREFIX = "trimmedVideo_";

    // 返回视频剪辑后路径
    String RESULT_VIDEO_PATH = "RESULT_VIDEO_PATH";

    // 返回第一帧图片路径
    String RESULT_FIRST_FRAME_IMAGE_PATH = "RESULT_FIRST_FRAME_IMAGE_PATH";

    String RESULT_FIRST_FRAME_IMAGE_BITMAP = "RESULT_FIRST_FRAME_IMAGE_BITMAP";
}
