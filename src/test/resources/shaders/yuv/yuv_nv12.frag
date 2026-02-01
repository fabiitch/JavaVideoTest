#ifdef GL_ES
precision mediump float;
#endif

varying vec2 v_tex;
uniform sampler2D u_texture; // Y
uniform sampler2D u_texUV;   // UV (NV12: U in .r, V in .g)
uniform int u_fullRange;     // 1 = full [0..1], 0 = TV 16..235
uniform int u_uvIsLA;        // 1 si fallback LUMINANCE_ALPHA
uniform int u_swapUV;        // 1 pour inverser U/V (NV21)

void main() {
    float y = texture2D(u_texture, v_tex).r;

    vec2 uvRG = texture2D(u_texUV, v_tex).rg;
    float u = (u_uvIsLA == 1) ? uvRG.r : uvRG.r;  // r
    float v = (u_uvIsLA == 1) ? texture2D(u_texUV, v_tex).a : uvRG.g; // a ou g

    // Swap si la source est en NV21 (VU)
    if (u_swapUV == 1) {
        float tmp = u; u = v; v = tmp;
    }

    // Centre à 0
    u -= 0.5;
    v -= 0.5;

    // Etend la plage si TV-range
    if (u_fullRange == 0) {
        y = (y - 16.0/255.0) * (255.0/219.0);
    }

    // BT.709 (mets 601 si ta source est SD)
    float r = y + 1.5748 * v;
    float g = y - 0.1873 * u - 0.4681 * v;
    float b = y + 1.8556 * u;

    gl_FragColor = vec4(r, g, b, 1.0);
}
