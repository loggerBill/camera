package com.zhongmin.camerafilter;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class CameraGLRenderer implements GLSurfaceView.Renderer {
    private Context mContext;
    private CameraGLSurfaceView glSurfaceView;
    private Surface mSurface;
    private Size preViewSize;
    private int textureId;
    private SurfaceTexture surfaceTexture;
    private final float[] texMatrix = new float[16];


    private int programHandle;

    private int positionHandle;
    private int texCoordHandle;
    private int texMatrixHandle;
    private int textureHandle;

    private FloatBuffer vertexBuffer;
    private FloatBuffer texCoordBuffer;


    private int filterType = 0;
    private int filterTypeHandle = -1;



    private float viewAspectRatio = 1.0f; // 视图宽高比
    private float previewAspectRatio = 1.0f; // 预览宽高比
    private int mvpMatrixHandle; // MVP矩阵的uniform位置
    private final float[] mvpMatrix = new float[16]; // MVP矩阵


    // 新增：设置滤镜类型的方法
    public void setFilterType(int type) {
        filterType = type;
    }



    // 顶点和纹理坐标数据
    private static final float[] VERTEX_DATA = {
            -1.0f, -1.0f, 0f, // 左下
            1.0f, -1.0f, 0f, // 右下
            -1.0f,  1.0f, 0f, // 左上
            1.0f,  1.0f, 0f  // 右上
    };

    private static float[] TEX_COORD_DATA = {
//            0f, 1f, // 左下
//            1f, 1f, // 右下
//            0f, 0f, // 左上
//            1f, 0f  // 右上
            0f, 0f, // 原左上 -> 变为右下

            1f, 0f, // 原右上 -> 变为左下

            0f, 1f,  // 原左下 -> 变为右上


            1f, 1f // 原右下 -> 变为左上

    };

    public CameraGLRenderer(Context context, CameraGLSurfaceView cameraGLSurfaceView) {
        mContext = context;
        glSurfaceView = cameraGLSurfaceView;
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.e("camerafilter","onSurfaceCreated");
        // 生成纹理
        int[] textures = new int[1];
        GLES20.glGenTextures(1, textures, 0);
        textureId = textures[0];

        // 创建 SurfaceTexture 和 Surface
        surfaceTexture = new SurfaceTexture(textureId);
        surfaceTexture.setOnFrameAvailableListener(st -> {
            // 通过 glSurfaceView 调用 requestRender()
            glSurfaceView.requestRender();
        });

        if (preViewSize != null) {
            surfaceTexture.setDefaultBufferSize(
                    preViewSize.getWidth(),
                    preViewSize.getHeight()
            );
        }

        mSurface = new Surface(surfaceTexture);
        // 初始化着色器
        initShader();
    }

    private void initShader() {
        Log.d("camerafilter","initShader");
        // 顶点着色器代码（使用 OpenGL ES 2.0 语法）
        String vertexShaderSource =
                "attribute vec4 aPosition;\n" +
                        "attribute vec2 aTexCoord;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "uniform mat4 uTexMatrix;\n" +
                        "uniform mat4 uMvpMatrix;\n" + // 新增MVP矩阵
                        "void main() {\n" +
                        "    gl_Position = uMvpMatrix * aPosition;\n" + // 应用MVP矩阵
                        "    vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;\n" +
                        "}";

        // 片段着色器代码（处理外部纹理）
        String fragmentShaderSource =
                "#extension GL_OES_EGL_image_external : require\n" +
                        "precision mediump float;\n" +
                        "varying vec2 vTexCoord;\n" +
                        "uniform samplerExternalOES uTexture;\n" +
                        "uniform int uFilterType; // 新增：滤镜类型参数\n" +
                        "\n" +
                        "void main() {\n" +
                        "    vec4 color = texture2D(uTexture, vTexCoord);\n" +
                        "    \n" +
                        "    // 根据滤镜类型处理颜色\n" +
                        "    if (uFilterType == 0) { // 无滤镜\n" +
                        "        gl_FragColor = color;\n" +
                        "    } \n" +
                        "    else if (uFilterType == 1) { // 灰度滤镜\n" +
                        "        float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));\n" +
                        "        gl_FragColor = vec4(gray, gray, gray, color.a);\n" +
                        "    } \n" +
                        "    else if (uFilterType == 2) { // 反色滤镜\n" +
                        "        gl_FragColor = vec4(1.0 - color.r, 1.0 - color.g, 1.0 - color.b, color.a);\n" +
                        "    } \n" +
                        "    else if (uFilterType == 3) { // 复古棕褐色滤镜\n" +
                        "        float r = dot(color.rgb, vec3(0.393, 0.769, 0.189));\n" +
                        "        float g = dot(color.rgb, vec3(0.349, 0.686, 0.168));\n" +
                        "        float b = dot(color.rgb, vec3(0.272, 0.534, 0.131));\n" +
                        "        gl_FragColor = vec4(min(r, 1.0), min(g, 1.0), min(b, 1.0), color.a);\n" +
                        "    }\n" +
                        "    else if (uFilterType == 4) { // 冷色调滤镜\n" +
                        "        gl_FragColor = vec4(color.r * 0.8, color.g * 0.9, color.b * 1.2, color.a);\n" +
                        "    }\n" +
                        "    else if (uFilterType == 5) { // 暖色调滤镜\n" +
                        "        gl_FragColor = vec4(color.r * 1.2, color.g * 1.0, color.b * 0.8, color.a);\n" +
                        "    }\n" +
                        "    else if (uFilterType == 6) { // 卡通效果\n" +
                        "        float levels = 4.0;\n" +
                        "        vec3 posterized = floor(color.rgb * levels) / levels;\n" +
                        "        gl_FragColor = vec4(posterized, color.a);\n" +
                        "    }\n" +
                        "    else if (uFilterType == 7) { // 边缘检测\n" +
                        "        vec2 texelSize = vec2(1.0) / vec2(1920.0, 1080.0);" +
                        "        float sx = 0.0;\n" +
                        "        float sy = 0.0;\n" +
                        "        for (int i = -1; i <= 1; i++) {\n" +
                        "            for (int j = -1; j <= 1; j++) {\n" +
                        "                vec2 offset = vec2(i, j) * texelSize;\n" +
                        "                vec4 sample = texture2D(uTexture, vTexCoord + offset);\n" +
                        "                float gray = dot(sample.rgb, vec3(0.299, 0.587, 0.114));\n" +
                        "                sx += gray * float(i);\n" +
                        "                sy += gray * float(j);\n" +
                        "            }\n" +
                        "        }\n" +
                        "        float edge = sqrt(sx*sx + sy*sy);\n" +
                        "        gl_FragColor = vec4(edge, edge, edge, 1.0);\n" +
                        "    }\n" +
                        "    else if (uFilterType == 8) { // 模糊效果\n" +
                        "        vec4 sum = vec4(0.0);\n" +
                        "        float blur = 0.01; // 模糊强度\n" +
                        "        sum += texture2D(uTexture, vTexCoord + vec2(-4.0*blur, 0.0)) * 0.05;\n" +
                        "        sum += texture2D(uTexture, vTexCoord + vec2(-3.0*blur, 0.0)) * 0.09;\n" +
                        "        sum += texture2D(uTexture, vTexCoord + vec2(-2.0*blur, 0.0)) * 0.12;\n" +
                        "        sum += texture2D(uTexture, vTexCoord + vec2(-1.0*blur, 0.0)) * 0.15;\n" +
                        "        sum += texture2D(uTexture, vTexCoord) * 0.16;\n" +
                        "        sum += texture2D(uTexture, vTexCoord + vec2(1.0*blur, 0.0)) * 0.15;\n" +
                        "        sum += texture2D(uTexture, vTexCoord + vec2(2.0*blur, 0.0)) * 0.12;\n" +
                        "        sum += texture2D(uTexture, vTexCoord + vec2(3.0*blur, 0.0)) * 0.09;\n" +
                        "        sum += texture2D(uTexture, vTexCoord + vec2(4.0*blur, 0.0)) * 0.05;\n" +
                        "    }\n" +
                        "    else { // 默认无滤镜\n" +
                        "        gl_FragColor = color;\n" +
                        "    }\n" +
                        "}";

        // 编译着色器
        int vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderSource);
        int fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSource);

        // 创建着色器程序
        programHandle = GLES20.glCreateProgram();
        GLES20.glAttachShader(programHandle, vertexShader);
        GLES20.glAttachShader(programHandle, fragmentShader);
        GLES20.glLinkProgram(programHandle);

        // 检查链接状态
        int[] linkStatus = new int[1];
        GLES20.glGetProgramiv(programHandle, GLES20.GL_LINK_STATUS, linkStatus, 0);
        if (linkStatus[0] != GLES20.GL_TRUE) {
            String errorMsg = GLES20.glGetProgramInfoLog(programHandle);
            GLES20.glDeleteProgram(programHandle);
            throw new RuntimeException("Shader program link error: " + errorMsg);
        }

        Log.d("camerafilter","positionHandle");

        // 获取属性位置
        positionHandle = GLES20.glGetAttribLocation(programHandle, "aPosition");
        texCoordHandle = GLES20.glGetAttribLocation(programHandle, "aTexCoord");
        filterTypeHandle = GLES20.glGetUniformLocation(programHandle, "uFilterType");
        texMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uTexMatrix");
        textureHandle = GLES20.glGetUniformLocation(programHandle, "uTexture");
        mvpMatrixHandle = GLES20.glGetUniformLocation(programHandle, "uMvpMatrix");

        // 初始化MVP矩阵为单位矩阵
        Matrix.setIdentityM(mvpMatrix, 0);

        // 准备顶点数据缓冲区
        vertexBuffer = ByteBuffer.allocateDirect(VERTEX_DATA.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        vertexBuffer.put(VERTEX_DATA).position(0);

        // 准备纹理坐标缓冲区
        texCoordBuffer = ByteBuffer.allocateDirect(TEX_COORD_DATA.length * 4)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        texCoordBuffer.put(TEX_COORD_DATA).position(0);

        // 设置纹理参数
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        Log.d("camerafilter","init end");

    }


    // 辅助方法：编译着色器
    private int loadShader(int type, String shaderSource) {
        int shader = GLES20.glCreateShader(type);
        GLES20.glShaderSource(shader, shaderSource);
        GLES20.glCompileShader(shader);

        // 检查编译状态
        int[] compiled = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
        if (compiled[0] == 0) {
            String errorMsg = GLES20.glGetShaderInfoLog(shader);
            GLES20.glDeleteShader(shader);
            throw new RuntimeException("Shader compile error: " + errorMsg);
        }
        return shader;
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        viewAspectRatio = (float) width / height; // 计算视图宽高比
    }

    @Override
    public void onDrawFrame(GL10 gl) {

        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // 更新纹理
        surfaceTexture.updateTexImage();
        surfaceTexture.getTransformMatrix(texMatrix);

        // 绘制纹理
        drawTexture();
    }

    private void updateMvpMatrix() {
        // 重置为单位矩阵
        Matrix.setIdentityM(mvpMatrix, 0);

        if (previewAspectRatio > viewAspectRatio) {
            // 预览比视图宽，缩放高度
            float scale = viewAspectRatio / previewAspectRatio;
            Matrix.scaleM(mvpMatrix, 0, 1f, scale, 1f);
        } else {
            // 预览比视图高，缩放宽度
            float scale = previewAspectRatio / viewAspectRatio;
            Matrix.scaleM(mvpMatrix, 0, scale, 1f, 1f);
        }
    }


    private void drawTexture() {
        // 更新MVP矩阵
        updateMvpMatrix();
        // 使用着色器程序
        GLES20.glUseProgram(programHandle);

        // 启用顶点属性数组
        GLES20.glEnableVertexAttribArray(positionHandle);
        GLES20.glEnableVertexAttribArray(texCoordHandle);

        // 传递顶点数据
        vertexBuffer.position(0);
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer);

        // 传递纹理坐标数据
        texCoordBuffer.position(0);
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, texCoordBuffer);

        // 传递MVP矩阵
        if (mvpMatrixHandle != -1) {
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0);
        }

        // 传递纹理变换矩阵
        GLES20.glUniformMatrix4fv(texMatrixHandle, 1, false, texMatrix, 0);

        // 设置纹理单元
        if (filterTypeHandle != -1) {
            GLES20.glUniform1i(filterTypeHandle, filterType);
        }

        // 绘制
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        // 禁用顶点属性数组
        GLES20.glDisableVertexAttribArray(positionHandle);
        GLES20.glDisableVertexAttribArray(texCoordHandle);
    }


    public Surface getSurface() {
        return mSurface;
    }

    public void setPreviewSize(Size size) {
        preViewSize = size;
        previewAspectRatio = (float) size.getHeight() / size.getWidth(); // 计算预览宽高比
    }
}
