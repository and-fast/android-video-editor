package and.fast.video.effect.filter;

import and.fast.video.effect.filter.base.GlFilter;
import and.fast.video.effect.utils.OpenGlUtils;

/**
 * Apply Gamma Effect on Video being played
 */
public class GammaEffect extends GlFilter {

    private static final String FRAGMENT_SHADER =
        "#extension GL_OES_EGL_image_external : require\n"
        + "precision mediump float;\n"

        + "varying vec2 vTextureCoord;\n"
        + "uniform samplerExternalOES sTexture;\n"
        + "float gamma=" + 0.8f + ";\n"

        + "void main() {\n"

        + "vec4 textureColor = texture2D(sTexture, vTextureCoord);\n"
        + "gl_FragColor = vec4(pow(textureColor.rgb, vec3(gamma)), textureColor.w);\n"

        + "}\n";

    private float gammaValue;

    public GammaEffect() {
        this(2.0f);
    }

    /**
     * Initialize Effect
     *
     * @param gammaValue Range should be between 0.0 - 2.0 with 1.0 being normal.
     */
    public GammaEffect(float gammaValue) {
        super(OpenGlUtils.DEFAULT_VERTEX_SHADER, FRAGMENT_SHADER);

        if (gammaValue < 0.0f)
            gammaValue = 0.0f;
        if (gammaValue > 2.0f)
            gammaValue = 2.0f;
        this.gammaValue = gammaValue;

    }
}