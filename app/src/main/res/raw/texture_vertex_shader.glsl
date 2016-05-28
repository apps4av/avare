uniform mat4 u_Matrix;


attribute float a_Position;
attribute float a_TextureCoordinates;

varying vec2 v_TextureCoordinates;

void main()                    
{
    // data comes in packed to save memory
    float row  = floor(a_TextureCoordinates / 1024.0);
    float col  = a_TextureCoordinates - row * 1024.0;
    //-1,1    1,1
    //-1,-1   1,-1
    vec4 ap = vec4((col * 2.0 - 512.0) / 512.0, (-row * 2.0 + 512.0) / 512.0, a_Position, 1.0);
    vec2 at = vec2(col / 512.0, row / 512.0);
    v_TextureCoordinates = at;
    gl_Position = u_Matrix * ap;
}          