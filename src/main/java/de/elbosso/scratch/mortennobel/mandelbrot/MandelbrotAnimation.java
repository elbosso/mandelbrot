package de.elbosso.scratch.mortennobel.mandelbrot;

import javax.swing.event.ChangeListener;
import java.awt.*;
import java.util.Random;

public class MandelbrotAnimation extends de.elbosso.util.beans.EventHandlingSupport implements java.lang.Runnable,
		de.elbosso.util.pattern.command.StartStopActionPair.Agent{
	private static long randomSeed = 21128324L; 
	private static Random random = new Random(randomSeed);
	private ChangeListener changeListener;
	private Dimension componentSize;

	// private helper function to generate some useful zoom-levels
	private static float rndZoom() {float f = Math.max(0.001f,random.nextFloat());return f*f;}
	// x,y, zoom level values
	private static float[][] pointOfInterest = {
			{0f,0f,1},
			{-1.628571f,0.017143f,rndZoom()},
			{-1.251429f,0.074286f,rndZoom()},
			{-1.171429f,0.325714f,rndZoom()},
			{-0.537143f,0.645714f,rndZoom()},
			{-0.211429f,0.685714f,rndZoom()},
			{0.137143f,0.662857f,rndZoom()},
			{0.268571f,0.005429f,rndZoom()},
			{0.434286f,-0.171429f,rndZoom()},
			{0.302857f,-0.451429f,rndZoom()},
			{-0.142857f,-1.005714f,rndZoom()},
			{-0.565714f,-0.628571f,rndZoom()},
			{-1.240000f,-0.325714f,rndZoom()},
			{-1.245714f,-0.022857f,rndZoom()},
	};

	private float centerXFrom = 0;
	private float centerXDelta = 0;
	private float centerYFrom = 0;
	private float centerYDelta = 0;
	public static final float ZOOM_FACTOR = 4; // width + height when zoom is 1
	private float zoomFrom = 1;
	private float zoomDelta = 0;
	private int currentIndex = 0;
	private float millisPerAnimation = 10000;

	private Thread thread = new Thread(this);

	private static final long frameRateWait = 1000/60;
	private boolean restart = false;

	private final MandelbrotSetting setting = new MandelbrotSetting();
	private int animationStart;
	private boolean running;
	private de.netsysit.util.threads.CubbyHole ch;
	private int latch=0;
	private float zoom=1.0f;

	public MandelbrotAnimation(ChangeListener changeListener,de.netsysit.util.threads.CubbyHole ch) {
		this.changeListener = changeListener;
		this.ch=ch;//new de.elbosso.util.threads.StopAndGoCubbyHole();
		reset();
	}

	public void setRunning(boolean newrunning)
	{
		boolean oldrunning=running;
		running=newrunning;
		if(isRunning()==true)
		{
			int currentTime = (int) (System.currentTimeMillis())&Integer.MAX_VALUE; // ignore sign bit
			animationStart=currentTime-latch;
			ch.put(java.lang.Boolean.TRUE);
		}
		else
		{
			ch.put(null);
			int currentTime = (int) (System.currentTimeMillis())&Integer.MAX_VALUE; // ignore sign bit
			latch = (currentTime-animationStart);
		}

		send("running",oldrunning,running);
	}

	public boolean isRunning()
	{
		return running;
	}
	public void setMovementSpeed(int seconds){
		millisPerAnimation = seconds * 1000;
	}

	public int getMovementSpeed(){
		return Math.round(millisPerAnimation/1000);
	}

	public void start(){
		thread.start();
	}

	private void reset(){
		currentIndex = 0;
		animationStart = (int) (System.currentTimeMillis())&Integer.MAX_VALUE; // ignore sign bit
		updateIndex();
	}

	public void restart(){
		restart = true;
	}

	private void updateIndex(){
		int oldIndex = currentIndex;
		currentIndex = (currentIndex+1)%pointOfInterest.length;
		centerXFrom = pointOfInterest[oldIndex][0];
		centerYFrom = pointOfInterest[oldIndex][1];
		zoomFrom = pointOfInterest[oldIndex][2];
		centerXDelta = pointOfInterest[currentIndex][0]-centerXFrom;
		centerYDelta = pointOfInterest[currentIndex][1]-centerYFrom;
		zoomDelta = pointOfInterest[currentIndex][2]-zoomFrom;
	}

	public void run() {
		while (thread!=null){
			try{
				ch.get();
				if (restart){
					reset();
					restart=false;
				}
				// assume this calculation does not take any significant amount of time
				int currentTime = (int) (System.currentTimeMillis())&Integer.MAX_VALUE; // ignore sign bit
				float fraction = (currentTime-animationStart)/millisPerAnimation;

				if(isRunning())
				{
					if (fraction > 1)
					{
						interpolate(1); // move to end
						updateIndex();
						fraction = 0;
						animationStart = currentTime;

					}
					else
					{
						interpolate(easyBoth(fraction));
					}
				}
				changeListener.stateChanged(null);
				synchronized (thread){
					thread.wait(frameRateWait);
				}
			}
			catch (Exception e){
				e.printStackTrace();
			}
		}
	}

	/**
	 * Make easing effect. (Inspired by Yahoo UI)
	 */
	private static float easyBoth(float t) {
		if ((t/= 1f /2) < 1) {
			return 1f /2*t*t;
		}

		return -1f /2 * ((--t)*(t-2) - 1) ;
	}

	public float getZoom()
	{
		return zoom;
	}

	private void interpolate(float fraction){
		float centerX = centerXFrom+fraction*centerXDelta;
		float centerY = centerYFrom+fraction*centerYDelta;
		zoom = zoomFrom+fraction*zoomDelta;
		float smallestsize = zoom*ZOOM_FACTOR;
		float width=(float)(componentSize!=null?componentSize.width:800);
		float height=(float)(componentSize!=null?componentSize.height:800);
		float fac=width/height;
		if(fac<1)
		{
			setting.setHeight(smallestsize/fac);
			setting.setWidth(smallestsize);
		}
		else
		{
			setting.setHeight (smallestsize);
			setting.setWidth(smallestsize*fac);
		}
		setting.setX(centerX-setting.getWidth()*0.5f);
		setting.setY(centerY-setting.getHeight()*0.5f);
	}

	public MandelbrotSetting getSetting() {
		return setting;
	}

	public void setComponentSize(Dimension componentSize)
	{
		this.componentSize=componentSize;
	}
}
