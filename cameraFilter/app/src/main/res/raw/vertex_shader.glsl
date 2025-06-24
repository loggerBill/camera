attribute vec4 aPosition;
attribute vec2 aTexCoord;
varying vec2 vTexCoord;
uniform mat4 uTexMatrix;
uniform mat4 uMvpMatrix;
uniform vec2 uTextureSize;

void main() {
    gl_Position = uMvpMatrix * aPosition;
    vTexCoord = (uTexMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
}