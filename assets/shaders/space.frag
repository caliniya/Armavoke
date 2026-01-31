#define HIGHP

uniform sampler2D u_texture; // 传入的宇宙背景图
uniform vec2 u_resolution;   // 屏幕分辨率
uniform vec2 u_camPos;       // 相机世界坐标
uniform float u_zoom;        // 相机缩放比例
uniform vec2 u_texSize;      // 图片的像素尺寸
uniform float u_parallax;    // 视差系数

void main() {
    vec2 screenOffset = gl_FragCoord.xy - (u_resolution * 0.5);

    vec2 worldPos = (screenOffset * u_zoom) + (u_camPos * u_parallax);

    vec2 uv = worldPos / u_texSize;
    
    gl_FragColor = texture2D(u_texture, uv);
}