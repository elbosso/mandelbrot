package de.elbosso.scratch.mortennobel.mandelbrot;

/**
 * Simple class used for updating FPS
 */
public class FPSStat {
	private long priorusedTimeMillis;
	private long usedTimeMillis;
	private long lastTimeInMillis;
	private int totalRenderTimeMs;
	private int numberOfRepaints;

	private long nanoTimeStart;

	private boolean clearFlag = false;

	public void timerStart(){
		nanoTimeStart = System.nanoTime();
		priorusedTimeMillis=(System.nanoTime()-nanoTimeStart)/1000000L;
		usedTimeMillis=priorusedTimeMillis;

	}

	public void timerEnd(){
		// find delta time and convert to millis
		usedTimeMillis = (System.nanoTime()-nanoTimeStart)/1000000L;
		if(usedTimeMillis!=priorusedTimeMillis)
			lastTimeInMillis=usedTimeMillis-priorusedTimeMillis;
		priorusedTimeMillis=usedTimeMillis;
		if (usedTimeMillis==0){
			return;
		}
		totalRenderTimeMs += usedTimeMillis;
		numberOfRepaints++;
		if (clearFlag){
			clearFlag = false;
			totalRenderTimeMs = 0;
			numberOfRepaints = 0;
		}
	}

	public float getAvarageRendertime(){
		if (numberOfRepaints==0 || totalRenderTimeMs==0){
			return Float.MAX_VALUE;
		}
		return totalRenderTimeMs/(float)numberOfRepaints;
	}

	public float getLastRendertime(){
		if (numberOfRepaints==0 || totalRenderTimeMs==0){
			return Float.MAX_VALUE;
		}
		return lastTimeInMillis;
	}


	public void clear(){
		clearFlag = true;
	}
}
