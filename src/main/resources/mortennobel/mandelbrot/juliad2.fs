uniform vec2 mandel_x;
uniform vec2 mandel_y;
uniform vec2 mandel_reC;
uniform vec2 mandel_imC;
uniform vec2 mandel_width;
uniform vec2 mandel_height;
uniform float mandel_iterations;

vec2 ds_set(float a)
{
    vec2 z;
    z.x = a;
    z.y = 0.0;
    return z;
}
vec2 ds_two = ds_set(2.0);
vec2 ds_four = ds_set(4.0);

vec2 ds_add (vec2 dsa, vec2 dsb)
{
    vec2 dsc;
    float t1, t2, e;

    t1 = dsa.x + dsb.x;
    e = t1 - dsa.x;
    t2 = ((dsb.x - e) + (dsa.x - (t1 - e))) + dsa.y + dsb.y;

    dsc.x = t1 + t2;
    dsc.y = t2 - (dsc.x - t1);
    return dsc;
}

vec2 ds_sub (vec2 dsa, vec2 dsb)
{
    vec2 dsc;
    vec2 dsbb;
    dsbb.x=-dsb.x;
    dsbb.y=-dsb.y;
    float t1, t2, e;

    t1 = dsa.x + dsbb.x;
    e = t1 - dsa.x;
    t2 = ((dsbb.x - e) + (dsa.x - (t1 - e))) + dsa.y + dsbb.y;

    dsc.x = t1 + t2;
    dsc.y = t2 - (dsc.x - t1);
    return dsc;
}

vec2 ds_mul (vec2 dsa, vec2 dsb)
{
    vec2 dsc;
    float c11, c21, c2, e, t1, t2;
    float a1, a2, b1, b2, cona, conb, split = 8193.;

    cona = dsa.x * split;
    conb = dsb.x * split;
    a1 = cona - (cona - dsa.x);
    b1 = conb - (conb - dsb.x);
    a2 = dsa.x - a1;
    b2 = dsb.x - b1;

    c11 = dsa.x * dsb.x;
    c21 = a2 * b2 + (a2 * b1 + (a1 * b2 + (a1 * b1 - c11)));

    c2 = dsa.x * dsb.y + dsa.y * dsb.x;

    t1 = c11 + c2;
    e = t1 - c11;
    t2 = dsa.y * dsb.y + ((c2 - e) + (c11 - (t1 - e))) + c21;

    dsc.x = t1 + t2;
    dsc.y = t2 - (dsc.x - t1);

    return dsc;
}

bool ds_lessOrEqual(vec2 dsa, vec2 dsb)
{
    bool rv;
    if(dsb.x > dsa.x)
    {
        rv=true;
    }
    else
    {
        if(dsb.x < dsa.x)
        {
            rv=false;
        }
        else
        {
            if(dsb.y > dsa.y)
            {
                rv=true;
            }
            else
            {
                rv=false;
            }
        }
    }
    return rv;
}

float calculateMandelbrotIterations(vec2 x, vec2 y) {
    vec2 reZ = x;
    vec2 imZ = y;
    vec2 reC=mandel_reC;
    vec2 imC=mandel_imC;
    int iter = 0;
    // optimized - caching xx*xx and yy*yy
    vec2 xxXxCache=ds_mul(reZ,reZ);
    vec2 yyYyCache=ds_mul(imZ,imZ);
    while ( ds_lessOrEqual(ds_add(xxXxCache,yyYyCache), ds_four) && iter<mandel_iterations) {
        vec2 temp=ds_mul(reZ,imZ);
        reZ=ds_add(ds_sub(xxXxCache,yyYyCache),reC);
        imZ=ds_add(ds_mul(ds_two,temp),imC);

        xxXxCache=ds_mul(reZ,reZ);
        yyYyCache=ds_mul(imZ,imZ);
        iter ++;
    }

    return iter;
}

const vec3 blue = vec3(0.0,0.0,1.0);
const vec3 white = vec3(1.0,1.0,1.0);
const vec3 yellow = vec3(1.0,1.0,0.0);
const vec3 red = vec3(1.0,0.0,0.0);
const float colorResolution = 16.0; // how many iterations the first color band should use (2nd use the double amount)

vec3 getColorByIndex(float index){
	float i = mod(index,4.0);
	if (i<0.5){
		return blue;
	}
	if (i<1.5){
		return white;
	}
	if (i<2.5){
		return yellow;
	}
	return red;
}

vec4 getColor(float iterations) {
    	if (iterations==mandel_iterations){
     	return vec4(0.0,0.0,0.0,1.0);
     }
	float colorIndex = 0.0;
	float iterationsFloat = iterations;
	float colorRes = colorResolution;
	while (iterationsFloat>colorRes){
		iterationsFloat -= colorRes;
		colorRes = colorRes*2.0;
		colorIndex ++;
	}
	float fraction = iterationsFloat/colorRes;
	vec3 from = getColorByIndex(colorIndex);
	vec3 to = getColorByIndex(colorIndex+1.0);
	vec3 res = mix(from,to,fraction);
	return vec4(res.x,res.y,res.z,1.0);
}

void main()
{
	vec2 x = ds_add(mandel_x,ds_mul(ds_set(gl_TexCoord[0].x),mandel_width));
	vec2 y = ds_add(mandel_y,ds_mul(ds_set(gl_TexCoord[0].y),mandel_height));
	float iterations = calculateMandelbrotIterations(x,y);
	gl_FragColor = getColor(iterations);
}
