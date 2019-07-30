uniform vec2 mandel_x;
uniform vec2 mandel_y;
uniform vec2 mandel_width;
uniform vec2 mandel_height;
uniform float mandel_iterations;

void main()
{
	gl_TexCoord[0] = gl_MultiTexCoord0;
	gl_Position = ftransform();
}
