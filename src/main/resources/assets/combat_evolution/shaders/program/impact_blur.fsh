#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 center;

uniform float intensity;
uniform float strength;
uniform int samples;

in vec2 texCoord;
out vec4 fragColor;

void main()
{
    vec2 dir = texCoord - center;
    float dist = length(dir);
    vec2 ndir = (dist > 0.00001) ? dir / dist : vec2(0.0);

    float distFactor = smoothstep(0.0, 0.8, dist);
    float blurBoost = mix(0.2, 1.5, distFactor);
    float blast = 1.0 - intensity;
    float core = 1.0 - smoothstep(0.0, 0.4, dist);

    float centerProtect = smoothstep(0.0, 0.2, dist);
    float blurScale = strength * blast * blurBoost * distFactor * (1.0 + core * 1.5) * centerProtect;
    vec2 offset = ndir * blurScale;

    vec4 color = texture(DiffuseSampler, texCoord);
    float total = 1.0;

    for (int i = 1; i <= samples; i++){
        float t = float(i) / float(samples);
        float weight = pow(1.0 - t, 2.0);
        vec2 uv = texCoord - offset * t;
        uv = clamp(uv, vec2(0.001), vec2(0.999));
        color += texture(DiffuseSampler, uv) * weight;
        total += weight;
    }
    color /= total;


    float redIntensity = 1.0 - intensity * 0.7;
    float rangeStart = 0.01;
    float rangeFactor = smoothstep(rangeStart, 1.0, dist);
    float centerClear = smoothstep(0.0, 0.05, dist);
    rangeFactor *= centerClear;

    float alpha = 0.7 * rangeFactor;
    alpha = clamp(alpha, 0.0, 1.0);

    vec3 bloodColor = vec3(0.45, 0.01, 0.01);
    float brightness = 0.7 + 0.3 * rangeFactor;
    bloodColor *= brightness;

    vec3 redOverlay = bloodColor * alpha * redIntensity;
    color.rgb += redOverlay;

    float tint = alpha * 0.35 * redIntensity;
    color.r *= (1.0 + tint * 0.8);
    color.g *= (1.0 - tint * 0.5);
    color.b *= (1.0 - tint * 0.7);

    color.rgb = clamp(color.rgb, 0.0, 1.0);

    fragColor = color;
}