package de.elbosso.scratch.mortennobel.mandelbrot;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;

public class JuliaJava2D extends JComponent implements MandelbrotRender {

	private BufferedImage image = new BufferedImage(1,1, BufferedImage.TYPE_INT_RGB);
	private WritableRaster imageRaster = image.getRaster();
	private DataBufferInt dataBuffer = (DataBufferInt) imageRaster.getDataBuffer();
	private final MandelbrotSetting settings;
	private final MandelbrotSetting settingsCopy = new MandelbrotSetting();

	private FPSStat stat = new FPSStat();
	private int[] precomputedColors = new int[0];

	public JuliaJava2D(MandelbrotSetting settings) {
		this.settings = settings;
        settings.copyTo(settingsCopy);
		buildPrecomputedColors();
	}

	public void buildPrecomputedColors(){
		// only grow the precomputedColors array
		if (settingsCopy.getIterations()<precomputedColors.length){
			return;
		}
		precomputedColors = new int[settingsCopy.getIterations()+1];
		for (int i=0;i<precomputedColors.length;i++){
			precomputedColors[i] = getColor(i);
		}
	}

	private void rebuildImage(int width, int height){
		image = new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
		imageRaster = image.getRaster();
		dataBuffer = (DataBufferInt) imageRaster.getDataBuffer();
	}

	private int calculateMandelbrotIterations(double x, double y) {
		double reZ = x;
		double imZ = y;
		double reC=settings.getReC();
		double imC=settings.getImC();
		int iter = 0;
        // optimized - caching xx*xx and yy*yy
		double xxXxCache=reZ*reZ;
		double yyYyCache=imZ*imZ;
		while ( xxXxCache+yyYyCache<= DEFININGRADIUSSQUARED && iter<settingsCopy.getIterations()) {
			double temp=reZ*imZ;
			reZ=xxXxCache-yyYyCache+reC;
			imZ=2.0*temp+imC;


			xxXxCache=reZ*reZ;
			yyYyCache=imZ*imZ;
			iter ++;
		}

		return iter;
	}

	private static final float[] blue = Color.blue.getRGBComponents(null);
	private static final float[] white = Color.white.getRGBComponents(null);
	private static final float[] yellow = Color.yellow.getRGBComponents(null);
	private static final float[] red = Color.red.getRGBComponents(null);
	private static final float[][] colorCycle = {blue, white, yellow, red};
	// how many iterations the first color band should use (2nd use the double amount)
	private static final int colorResolution = 16;
	private int getColor(int iterations) {
		if (iterations>=settingsCopy.getIterations()){
			return 0; // black
		}
		float[] from;
		float[] to;
		int colorIndex = 0;
		int iterationsFloat = iterations;
		int colorRes = colorResolution;
		while (iterationsFloat>colorRes){
			iterationsFloat -= colorRes;
			colorRes = colorRes<<1;
			colorIndex ++;
		}
		from = colorCycle[colorIndex%colorCycle.length];
		to = colorCycle[(colorIndex+1)%colorCycle.length];
		float fraction = iterationsFloat/(float)colorRes;
		int[] res = new int[3];

		// interpolate between from and to color
		for (int i=0;i<3;i++){
			float delta = to[i]-from[i];
			res[i] = (int) ((from[i]+delta*fraction)*255);
		}
		return ((res[0])<<16)+((res[1])<<8)+(res[2]);
	}


	private void updateMandelbrotImage() {
		stat.timerStart();
        settings.copyTo(settingsCopy);
        buildPrecomputedColors();
		double deltaX = settingsCopy.getWidth()/image.getWidth();
		double deltaY = settingsCopy.getHeight()/image.getHeight();
		int index = 0;
		int[] data = dataBuffer.getData();
		double y = settingsCopy.getY();
		for (int pY = 0;pY < image.getHeight() ; pY++, y+= deltaY) {
			double x = settingsCopy.getX();
			for (int pX=0;pX<image.getWidth();pX++,x+=deltaX) {
				int iterations = calculateMandelbrotIterations(x,y);

				data[index] = precomputedColors[iterations];
				index++;
			}
		}
		stat.timerEnd();
	}

	@Override
	protected void paintComponent(Graphics g) {
		if (getWidth()!=image.getWidth() || getHeight()!=image.getHeight()){
			rebuildImage(getWidth(),getHeight());
		}
		updateMandelbrotImage();
		g.drawImage(image,0,0,null);
	}

	public void pauseAnimation() {
	}

	public void resumeAnimation() {
	}

	public Component getComponent() {
		return this;
	}

	public FPSStat getStats() {
		return stat;
	}

	@Override
	public void screenshot(java.io.File f)
	{
		try
		{
			BufferedImage bimg=new BufferedImage(getComponent().getWidth(),getComponent().getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics g=bimg.getGraphics();
			getComponent().paint(g);
			javax.imageio.ImageIO.write(bimg,f.getName().substring(f.getName().lastIndexOf(".")+1), f);
			g.dispose();
		}
		catch (Throwable exp)
		{
			de.elbosso.util.Utilities.handleException(null, getComponent(), exp);
		}
	}
}
