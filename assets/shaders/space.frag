precision highp float;

uniform vec2 u_resolution;
uniform vec2 u_camPos;
uniform float u_time;

float random(vec2 st) {
    st = mod(st, 10000.0); 
    return fract(sin(dot(st.xy, vec2(42.4868, 78.9191))) * 43758.545356123);
}

float starLayer(vec2 uv, float scale, float speed, float r) {
    vec2 pos = (uv * scale) + (u_camPos * 0.002 * speed);
    
    vec2 gridIndex = floor(pos);
    vec2 gridPos = fract(pos);
    
    float totalBrightness = 0.0;
    
    for (int y = -1; y <= 1; y++) {
        for (int x = -1; x <= 1; x++) {
            vec2 neighborOffset = vec2(float(x), float(y));
            vec2 neighborIndex = gridIndex + neighborOffset;
            
            float rnd = random(neighborIndex + r);
            
            if (rnd > 0.75) {
                vec2 seed1 = vec2(r + 57.0, r + 123.456);
                vec2 seed2 = vec2(r + 83.0, r + 789.012);
                
                vec2 starOffsetInCell = vec2(random(seed1), random(seed2));
                
                vec2 starPosRelativeToPixel = neighborOffset + starOffsetInCell - gridPos;
                
                float dist = length(starPosRelativeToPixel);
                
                float size = (rnd - 0.75) * 0.5; 
                
                float glow = 0.015 / (dist * dist + 0.0005);
                
                float cutoff = smoothstep(100.0, 0.05, dist);
                
                totalBrightness += glow * size * cutoff;
            }
        }
    }
    
    return totalBrightness;
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution.y;
    vec3 color = vec3(0.01, 0.01, 0.03); 
    
    color += vec3(starLayer(uv, 30.0, 0.4, 7.0)) * 0.4;
    color += vec3(starLayer(uv, 20.0, 0.7, 11.0)) * 0.7;
    color += vec3(starLayer(uv, 10.0, 1.0, 17.0)) * 1.0;
    
    gl_FragColor = vec4(color, 1.0);
}