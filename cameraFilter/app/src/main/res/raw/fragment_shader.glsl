#extension GL_OES_EGL_image_external : require
precision mediump float;
varying vec2 vTextureCoord;
uniform samplerExternalOES sTexture;
uniform int uFilterType;

void main() {
    vec4 color = texture2D(sTexture, vTextureCoord);

    if (uFilterType == 1) { // 灰度滤镜
        float gray = dot(color.rgb, vec3(0.299, 0.587, 0.114));
        gl_FragColor = vec4(gray, gray, gray, color.a);
    }
    else if (uFilterType == 2) { // 反色滤镜
        gl_FragColor = vec4(1.0 - color.r, 1.0 - color.g, 1.0 - color.b, color.a);
    }
    else if (uFilterType == 3) { // 复古棕褐色滤镜
        float r = dot(color.rgb, vec3(0.393, 0.769, 0.189));
        float g = dot(color.rgb, vec3(0.349, 0.686, 0.168));
        float b = dot(color.rgb, vec3(0.272, 0.534, 0.131));
        gl_FragColor = vec4(min(r, 1.0), min(g, 1.0), min(b, 1.0), color.a);
    }
    else { // 无滤镜
        gl_FragColor = color;
    }
}