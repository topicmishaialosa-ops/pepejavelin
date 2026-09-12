#version 150

#moj_import <huihui:common.glsl>

in vec2 FragCoord; // normalized fragment.fsh.fsh coord relative to the primitive
in vec4 FragColor;
in vec2 TexCoord;

uniform sampler2D Sampler0;
uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
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

    float alpha = 1.0 - smoothstep(1.0 - Smoothness, 1.0, distance);
    vec4 whiteColor = vec4(1.0, 1.0, 1.0, alpha); // white color - no color modulation applied by default

    vec4 finalColor = whiteColor * texture(Sampler0, TexCoord) * FragColor;

    if (finalColor.a == 0.0) { // alpha test
        discard;
    }

    OutColor = finalColor * ColorModulator;
}