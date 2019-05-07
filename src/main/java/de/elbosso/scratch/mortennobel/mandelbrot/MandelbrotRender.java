package de.elbosso.scratch.mortennobel.mandelbrot;

import java.awt.*;

public interface MandelbrotRender {
	/**
	 * Only used for AWT
	 */
	public void repaint();


	/**
	 * Only used for JOGL
	 */
	public void pauseAnimation();

	/**
	 * Only used for JOGL
	 */
	public void resumeAnimation();

	public Component getComponent();

	public FPSStat getStats();
	public void screenshot(java.io.File f);
}
