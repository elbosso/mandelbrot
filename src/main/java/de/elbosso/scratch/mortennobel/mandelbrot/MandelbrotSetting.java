package de.elbosso.scratch.mortennobel.mandelbrot;

public class MandelbrotSetting {
	private double x = 0.42483058528569073;//0.302857;//-2;
	private double y = -0.2134979507302626;//-0.451429;//-2;
	private double height = 0.3;//4;
	private double width = 0.3;//4;
	private int iterations = 128;
	private double reC=-0.6;
	private double imC=0.6;


	public double getX() {
		return x;
	}
	public float getXAsFloat() {
		return (float)x;
	}

	public void setX(double x) {
		this.x = x;
	}

	public double getY() {
		return y;
	}

	public float getYAsFloat() {
		return (float)y;
	}

	public void setY(double y) {
		this.y = y;
	}

	public double getHeight() {
		return height;
	}

	public float getHeightAsFloat() {
		return (float)height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getWidth() {
		return width;
	}

	public float getWidthAsFloat() {
		return (float)width;
	}

	public void setWidth(double width) {
		this.width = width;
	}

	public int getIterations() {
		return iterations;
	}

	public void setIterations(int iterations) {
		this.iterations = iterations;
	}

	public double getImC() {
		return imC;
	}

	public float getImCAsFloat() {
		return (float)imC;
	}

	public void setImC(double imC) {
		this.imC = imC;
	}

	public double getReC() {
		return reC;
	}

	public float getReCAsFloat() {
		return (float)reC;
	}

	public void setReC(double reC) {
		this.reC = reC;
	}

	public void copyTo(MandelbrotSetting copy) {
		copy.x = x;
		copy.y = y;
		copy.height = height;
		copy.width = width;
		copy.iterations = iterations;
		copy.imC=imC;
		copy.reC=reC;
	}
}
