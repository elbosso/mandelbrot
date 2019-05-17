/*
 * Copyright (c) 2019.
 *
 * Juergen Key. Alle Rechte vorbehalten.
 *
 * Weiterverbreitung und Verwendung in nichtkompilierter oder kompilierter Form,
 * mit oder ohne Veraenderung, sind unter den folgenden Bedingungen zulaessig:
 *
 *    1. Weiterverbreitete nichtkompilierte Exemplare muessen das obige Copyright,
 * die Liste der Bedingungen und den folgenden Haftungsausschluss im Quelltext
 * enthalten.
 *    2. Weiterverbreitete kompilierte Exemplare muessen das obige Copyright,
 * die Liste der Bedingungen und den folgenden Haftungsausschluss in der
 * Dokumentation und/oder anderen Materialien, die mit dem Exemplar verbreitet
 * werden, enthalten.
 *    3. Weder der Name des Autors noch die Namen der Beitragsleistenden
 * duerfen zum Kennzeichnen oder Bewerben von Produkten, die von dieser Software
 * abgeleitet wurden, ohne spezielle vorherige schriftliche Genehmigung verwendet
 * werden.
 *
 * DIESE SOFTWARE WIRD VOM AUTOR UND DEN BEITRAGSLEISTENDEN OHNE
 * JEGLICHE SPEZIELLE ODER IMPLIZIERTE GARANTIEN ZUR VERFUEGUNG GESTELLT, DIE
 * UNTER ANDEREM EINSCHLIESSEN: DIE IMPLIZIERTE GARANTIE DER VERWENDBARKEIT DER
 * SOFTWARE FUER EINEN BESTIMMTEN ZWECK. AUF KEINEN FALL IST DER AUTOR
 * ODER DIE BEITRAGSLEISTENDEN FUER IRGENDWELCHE DIREKTEN, INDIREKTEN,
 * ZUFAELLIGEN, SPEZIELLEN, BEISPIELHAFTEN ODER FOLGENDEN SCHAEDEN (UNTER ANDEREM
 * VERSCHAFFEN VON ERSATZGUETERN ODER -DIENSTLEISTUNGEN; EINSCHRAENKUNG DER
 * NUTZUNGSFAEHIGKEIT; VERLUST VON NUTZUNGSFAEHIGKEIT; DATEN; PROFIT ODER
 * GESCHAEFTSUNTERBRECHUNG), WIE AUCH IMMER VERURSACHT UND UNTER WELCHER
 * VERPFLICHTUNG AUCH IMMER, OB IN VERTRAG, STRIKTER VERPFLICHTUNG ODER
 * UNERLAUBTE HANDLUNG (INKLUSIVE FAHRLAESSIGKEIT) VERANTWORTLICH, AUF WELCHEM
 * WEG SIE AUCH IMMER DURCH DIE BENUTZUNG DIESER SOFTWARE ENTSTANDEN SIND, SOGAR,
 * WENN SIE AUF DIE MOEGLICHKEIT EINES SOLCHEN SCHADENS HINGEWIESEN WORDEN SIND.
 *
 */

package de.elbosso.scratch.mortennobel.mandelbrot;

import de.netsysit.util.threads.CubbyHole;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.WritableRaster;

public class MandelbrotJava2DThreaded extends JComponent implements MandelbrotRender {

	private BufferedImage image = new BufferedImage(1,1, BufferedImage.TYPE_INT_RGB);
	private WritableRaster imageRaster = image.getRaster();
	private DataBufferInt dataBuffer = (DataBufferInt) imageRaster.getDataBuffer();
	private final MandelbrotSetting settings;
	private final MandelbrotSetting settingsCopy = new MandelbrotSetting();

	private FPSStat stat = new FPSStat();
	private int[] precomputedColors = new int[0];
	private de.netsysit.util.threads.ThreadManager threadManager=new de.netsysit.util.threads.ThreadManager(MandelbrotJava2DThreaded.class.getName(),Runtime.getRuntime().availableProcessors()+1);

	public MandelbrotJava2DThreaded(MandelbrotSetting settings) {
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
		double xx = 0.0f;
		double yy = 0.0f;
		int iter = 0;
//      Original
//		while (xx * xx + yy * yy <= 4.0f && iter<settingsCopy.getIterations()) {
//			float temp = xx*xx - yy*yy + x;
//			yy = 2.0f*xx*yy + y;
//
//			xx = temp;
//
//			iter ++;
//		}

        // optimized - caching xx*xx and yy*yy
		double xxXxCache;
		double yyYyCache;
        while ((xxXxCache=xx * xx) + (yyYyCache=yy * yy) <= 4.0f && iter<settingsCopy.getIterations()) {
			double temp = xxXxCache - yyYyCache + x;
			yy = 2.0f*xx*yy + y;

			xx = temp;

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
		java.awt.Graphics gfx=image.getGraphics();
		java.util.List<MandelbrotTile> tiles=new java.util.LinkedList();
		int tiledim=128;
		int yoff=0;
		de.netsysit.util.threads.CubbyHole<MandelbrotTile> ch=new de.netsysit.util.threads.SimpleBufferingCubbyHole();
		while(yoff<image.getHeight())
		{
			int xoff=0;
			while(xoff<image.getWidth())
			{
				int w=java.lang.Math.min(image.getWidth()-xoff,tiledim);
				//System.out.println("#"+image.getWidth()+" "+xoff+" "+tiledim);
				int h=java.lang.Math.min(image.getHeight()-yoff,tiledim);
				MandelbrotTile tile=new MandelbrotTile(xoff,yoff,w,h,deltaX,deltaY,image.getWidth(),ch);
				tiles.add(tile);
				xoff+=tiledim;
			}
			yoff+=tiledim;
		}
		MandelbrotCollector collector=new MandelbrotCollector(ch,tiles.size(),gfx);
		java.lang.Thread t=new java.lang.Thread(collector);
		t.start();
//		for(MandelbrotTile tile:tiles)
//		{
			threadManager.execute(tiles.toArray(new Runnable[0]));
//		}
		try
		{
			t.join();
		} catch (InterruptedException e)
		{
			e.printStackTrace();
		}
/*		int index = 0;
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
*/		stat.timerEnd();
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

	class MandelbrotCollector extends de.elbosso.util.threads.StoppableImpl
	{
		private de.netsysit.util.threads.CubbyHole<MandelbrotTile> ch;
		private int counter;
		private java.awt.Graphics gfx;

		public MandelbrotCollector(CubbyHole<MandelbrotTile> ch, int counter, Graphics gfx)
		{
			super();
			this.ch = ch;
			this.counter = counter;
			this.gfx = gfx;
		}

		@Override
		public void run()
		{
			while(true)
			{
				try
				{
					MandelbrotTile tile = ch.get();
					if(tile!=null)
					{
						gfx.drawImage(tile.getImage(), tile.getXoff(), tile.getYoff(), null);
						--counter;
						if(counter<1)
							break;
					}
				}
				catch(java.lang.Throwable t)
				{
					t.printStackTrace();
				}
			}
			gfx.dispose();
		}
	}

	class MandelbrotTile extends de.elbosso.util.threads.StoppableImpl
	{
		private final int imageWidth;
		private final int xoff;
		private final int yoff;
		private final int width;
		private final int height;
		private final double deltaX;
		private final double deltaY;
		private java.awt.image.BufferedImage image;
		private final de.netsysit.util.threads.CubbyHole<MandelbrotTile> ch;

		public MandelbrotTile(int xoff, int yoff, int width, int height, double deltaX, double deltaY, int imageWidth,de.netsysit.util.threads.CubbyHole<MandelbrotTile> ch)
		{
			super();
//			System.out.println(xoff+" "+yoff+" "+width+" "+height);
			this.xoff = xoff;
			this.yoff = yoff;
			this.width = width;
			this.height = height;
			this.deltaX = deltaX;
			this.deltaY = deltaY;
			this.imageWidth=imageWidth;
			this.ch=ch;
			image = new BufferedImage(width,height, BufferedImage.TYPE_INT_RGB);
		}

		@Override
		public void run()
		{
			WritableRaster imageRaster = image.getRaster();
			DataBufferInt dataBuffer = (DataBufferInt) imageRaster.getDataBuffer();
			int index = 0;
			int[] data = dataBuffer.getData();
			double y = settingsCopy.getY()+yoff*deltaY;
			for (int pY = 0;pY < height ; pY++, y+= deltaY) {
				double x = settingsCopy.getX()+xoff*deltaX;
				for (int pX=0;pX<width;pX++,x+=deltaX) {
					int iterations = calculateMandelbrotIterations(x,y);

					data[index] = precomputedColors[iterations];
					index++;
				}
			}
			ch.put(this);
		}

		public int getXoff()
		{
			return xoff;
		}

		public int getYoff()
		{
			return yoff;
		}

		public int getWidth()
		{
			return width;
		}

		public int getHeight()
		{
			return height;
		}

		public BufferedImage getImage()
		{
			return image;
		}
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
