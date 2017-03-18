uniform mat4 u_Matrix;

uniform float u_Slope;
uniform float u_Intercept;
uniform float u_Normal;
uniform float u_Height;

attribute float a_S0;
attribute float a_S1;

varying vec2 v_TextureCoordinates;

const float ONE_OVER_64  = 0.015625;
const float ONE_OVER_384 = 0.002604167;
const float ONE_OVER_192 = 0.005208333;

void main()                    
{

    // data comes in packed to save memory
    // all this code goes hand in hand with java
    float row  = floor(a_S0 * ONE_OVER_64);
    float col  = floor(a_S1 * ONE_OVER_64);
    float pxr  = row * -64.0 + a_S0;
    float pxc  = col * -64.0 + a_S1;
    float px   = pxr * 64.0 + pxc;
    // this from Helper.java class under utils
    float ht   = u_Normal * (px * u_Slope + u_Intercept);

    // map coordinates
    //-1,1    1,1
    //-1,-1   1,-1
    vec4 ap = vec4(col * ONE_OVER_192 - 1.0, row * -ONE_OVER_192 + 1.0, ht, 1.0);

    // texture coordinates
    // 0,0   0,1
    // 1,0   1,1
    vec2 at;
    // find couple of heights where we will palette
    float refHt = u_Normal * (u_Height * u_Slope + u_Intercept);
    float refHt1000 = u_Normal * ((u_Height - 13.0) * u_Slope + u_Intercept); // 1000 ft = 24 * 12 * 3.28
    if(u_Height > 255.0) {
        at = vec2(col * ONE_OVER_384, row * ONE_OVER_384); // map texture
    }
    else if(ht >= refHt) {
        at = vec2(ht - refHt + 0.25, 0.125); // shades of red, below
    }
    else if(ht >= refHt1000) {
        at = vec2(ht - refHt1000 + 0.25, 0.625); // shades of yellow, just above
    }
    else {
        at = vec2(ht - refHt + 0.25, 0.375); // shades of green, much above
    }
    v_TextureCoordinates = at;
    gl_Position = u_Matrix * ap;
}
