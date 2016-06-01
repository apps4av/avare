uniform mat4 u_Matrix;


attribute float a_Position;
attribute float a_TextureCoordinates;

varying vec2 v_TextureCoordinates;

const float ONE_OVER_32  = 0.03125;
const float ONE_OVER_512 = 0.001953125;
const float ONE_OVER_256 = 0.00390625;
const float OFFSET_ELEVATION = 0.1;

void main()                    
{
    // data comes in packed to save memory
    float row  = floor(a_Position * ONE_OVER_32);
    float col  = floor(a_TextureCoordinates * ONE_OVER_32);
    float pxr  = a_Position - row * 32.0;
    float pxc  = a_TextureCoordinates - col * 32.0;
    float px   = (pxr * 32.0 + pxc) * ONE_OVER_512 - OFFSET_ELEVATION;

    //-1,1    1,1
    //-1,-1   1,-1
    vec4 ap = vec4(col * ONE_OVER_256 - 1.0, -row * ONE_OVER_256 + 1.0, px, 1.0);
    vec2 at = vec2(col * ONE_OVER_512, row * ONE_OVER_512);
    v_TextureCoordinates = at;
    gl_Position = u_Matrix * ap;
}          