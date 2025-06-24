#extension GL_OES_EGL_image_external : require 
precision mediump float; 
varying vec2 vTexCoord; 
uniform samplerExternalOES uTexture; 
uniform int uFilterType; // 新增：滤镜类型参数
 
void main() { 
    vec4 color = texture2D(uTexture, vTexCoord); 
     
    // 根据滤镜类型处理颜色 
    if (uFilterType == 0) { // 无滤镜 
        gl_FragColor = color; 
    }  
    else if (uFilterType == 1) { // 灰度滤镜 
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
    else if (uFilterType == 4) { // 冷色调滤镜 
        gl_FragColor = vec4(color.r * 0.8, color.g * 0.9, color.b * 1.2, color.a); 
    } 
    else if (uFilterType == 5) { // 暖色调滤镜 
        gl_FragColor = vec4(color.r * 1.2, color.g * 1.0, color.b * 0.8, color.a); 
    } 
    else if (uFilterType == 6) { // 卡通效果 
        float levels = 4.0; 
        vec3 posterized = floor(color.rgb * levels) / levels; 
        gl_FragColor = vec4(posterized, color.a); 
    }
    else if (uFilterType == 7) { // 边缘检测
        vec2 texelSize = vec2(1.0) / vec2(1920.0, 1080.0); // 需要实际分辨率
        float sx = 0.0;
        float sy = 0.0;

        // Sobel算子
        for (int i = -1; i <= 1; i++) {
            for (int j = -1; j <= 1; j++) {
                vec2 offset = vec2(i, j) * texelSize;
                vec4 sample1 = texture2D(uTexture, vTexCoord + offset);
                float gray = dot(sample1.rgb, vec3(0.299, 0.587, 0.114));
                sx += gray * float(i);
                sy += gray * float(j);
            }
        }
        float edge = sqrt(sx*sx + sy*sy);
        gl_FragColor = vec4(edge, edge, edge, 1.0);
    }
    else if (uFilterType == 8) { // 模糊效果
        vec4 sum = vec4(0.0);
        float blur = 0.01; // 模糊强度

        sum += texture2D(uTexture, vTexCoord + vec2(-4.0*blur, 0.0)) * 0.05;
        sum += texture2D(uTexture, vTexCoord + vec2(-3.0*blur, 0.0)) * 0.09;
        sum += texture2D(uTexture, vTexCoord + vec2(-2.0*blur, 0.0)) * 0.12;
        sum += texture2D(uTexture, vTexCoord + vec2(-1.0*blur, 0.0)) * 0.15;
        sum += texture2D(uTexture, vTexCoord) * 0.16;
        sum += texture2D(uTexture, vTexCoord + vec2(1.0*blur, 0.0)) * 0.15;
        sum += texture2D(uTexture, vTexCoord + vec2(2.0*blur, 0.0)) * 0.12;
        sum += texture2D(uTexture, vTexCoord + vec2(3.0*blur, 0.0)) * 0.09;
        sum += texture2D(uTexture, vTexCoord + vec2(4.0*blur, 0.0)) * 0.05;

        gl_FragColor = sum;
    }
    else { // 默认无滤镜 
        gl_FragColor = color;
    }
}