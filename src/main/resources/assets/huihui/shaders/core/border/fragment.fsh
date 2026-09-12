#version 150

#moj_import <huihui:common.glsl>

in vec2 FragCoord; // normalized fragment.fsh coord relative to the primitive
in vec4 FragColor;

uniform vec2 Size;
uniform vec4 Radius;
uniform vec2 Smoothness;
uniform float Thickness;
uniform float Style; // 0 = скруглённые углы, 1 = срезанные углы
uniform vec4 ColorModulator;

out vec4 OutColor;

void main() {
    vec2 center = Size * 0.5;
    vec2 pos = center - (FragCoord * Size);
    vec2 halfSize = center - 1.0;
    float distRounded = roundedBoxSDF(pos, halfSize, Radius);
    float distCut = cutCornerSDF(pos, halfSize, cutLegFromRadius(Radius));
    float distance = mix(distRounded, distCut, Style);

    float alpha = smoothstep(1.0 - Thickness - Smoothness.x - Smoothness.y,
        1.0 - Thickness - Smoothness.y, distance); // internal edge
    alpha *= 1.0 - smoothstep(1.0 - Smoothness.y, 1.0, distance); // external edge

    vec4 finalColor = vec4(FragColor.rgb, FragColor.a * alpha);

    if (finalColor.a == 0.0) { // alpha test
        discard;
    }

    OutColor = finalColor * ColorModulator;
}