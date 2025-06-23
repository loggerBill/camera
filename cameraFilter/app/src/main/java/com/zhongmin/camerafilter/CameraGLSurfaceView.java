package com.zhongmin.camerafilter;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.util.AttributeSet;
import android.util.Size;
import android.view.Surface;

public class CameraGLSurfaceView extends GLSurfaceView {

    private final CameraGLRenderer renderer;

    public CameraGLSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setEGLContextClientVersion(2); // 使用 OpenGL ES 2.0
        renderer = new CameraGLRenderer(context,this);
        setRenderer(renderer);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY); // 有帧时渲染
    }

    public Surface getSurface() {
        return renderer.getSurface();
    }

    public void setPreviewSize(Size size) {
        renderer.setPreviewSize(size);
    }

    public CameraGLRenderer getRenderer(){
        return renderer;
    }
}
