#version 150

#moj_import <huihui:common.glsl>

in vec2 FragCoord;
in vec4 FragColor;

uniform vec2 Size;
uniform vec4 Radius;
uniform float Smoothness;
uniform vec4 ColorModulator;
uniform float Progress;
uniform float StripeWidth;
uniform float Fade;
uniform float Style; // 0 = скруглённые углы, 1 = срезанные углы

out vec4 OutColor;

void main() {
    vec2 center = Size * 0.5;
    vec2 pos = center - (FragCoord * Size);
    vec2 halfSize = center - 1.0;
    float distRounded = roundedBoxSDF(pos, halfSize, Radius);
    float distCut = cutCornerSDF(pos, halfSize, cutLegFromRadius(Radius));
    float distance = mix(distRounded, distCut, Style);
    float alpha = 1.0 - smoothstep(1.0 - Smoothness, 1.0, distance);

    float diag = (FragCoord.x + FragCoord.y) * 0.5;
    float stripeWidth = StripeWidth;
    float fade = Fade;

    float stripeMask = smoothstep(Progress - stripeWidth * 0.5 - fade, Progress - stripeWidth * 0.5, diag)
                     * (1.0 - smoothstep(Progress + stripeWidth * 0.5, Progress + stripeWidth * 0.5 + fade, diag));

    vec4 stripeColor = FragColor * ColorModulator;

    vec4 finalColor = vec4(stripeColor.rgb, stripeColor.a * alpha * stripeMask);

    if (finalColor.a < 0.01) discard;
    OutColor = finalColor;
}
