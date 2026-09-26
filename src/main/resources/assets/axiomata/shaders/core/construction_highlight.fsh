#version 150

#moj_import <fog.glsl>

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float FogStart;
uniform float FogEnd;
uniform vec4 FogColor;

in float vertexDistance;
in vec4 vertexLight;
in vec4 lightMapColor;
in vec4 overlayColor;
in vec2 texCoord0;
in vec2 glowCode;

out vec4 fragColor;

void main() {
    vec4 textureColor = texture(Sampler0, texCoord0);
    if (textureColor.a < 0.1) {
        discard;
    }

    vec4 color = textureColor * vertexLight * ColorModulator;
    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);
    color *= lightMapColor;

    float pulse = (round(glowCode.r * 255.0) * 256.0 + round(glowCode.g * 255.0)) / 65535.0;
    float glowStrength = mix(0.035, 0.18, pulse);
    color.rgb = min(vec3(1.0), color.rgb + vec3(0.18, 0.62, 1.0) * glowStrength);
    color.a = textureColor.a * ColorModulator.a;
    fragColor = linear_fog(color, vertexDistance, FogStart, FogEnd, FogColor);
}
