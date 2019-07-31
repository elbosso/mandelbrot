package de.elbosso.scratch.mortennobel.mandelbrot;

import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import de.netsysit.util.ResourceLoader;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class JuliaJOGL extends GLCanvas implements GLEventListener, MandelbrotRender {
	private boolean updateUniformVars = true;
	private int vertexShaderProgram;
	private int fragmentShaderProgram;
	private int shaderprogram;
	private final MandelbrotSetting settings;
	private static final int FPS = 30; // Animator's target frames per second
	private final FPSAnimator animator = new FPSAnimator(this, FPS);
	//new Animator(this);
	private Object monitor=new Object();
	private File f;

	private FPSStat fpsStat = new FPSStat();

	public JuliaJOGL(MandelbrotSetting settings) {
		this.settings = settings;
//		animator.setRunAsFastAsPossible(true);
		addGLEventListener(this);
    }

    public void init(GLAutoDrawable drawable) {
        GL2 gl = drawable.getGL().getGL2();

        // Enable VSync
        gl.setSwapInterval(1);
		gl.glShadeModel(GL2.GL_FLAT);
		try {
			attachShaders(gl);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void dispose(GLAutoDrawable glAutoDrawable) {
		GL2 gl2 = glAutoDrawable.getGL().getGL2();
		gl2.glDetachShader(shaderprogram, vertexShaderProgram);
		gl2.glDetachShader(shaderprogram, fragmentShaderProgram);
		gl2.glDeleteProgram(shaderprogram);
	}

	private String[] loadShaderSrc(String name){
		StringBuilder sb = new StringBuilder();
		try{
			InputStream is = ResourceLoader.getResource("de/elbosso/scratch/data/mortennobel/mandelbrot/"+name).openStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			while ((line = br.readLine())!=null){
				sb.append(line);
				sb.append('\n');
			}
			is.close();
		}
		catch (Exception e){
			e.printStackTrace();
		}
		return new String[]{sb.toString()};
	}

	private void attachShaders(GL2 gl) throws Exception {
		vertexShaderProgram = gl.glCreateShader(GL2.GL_VERTEX_SHADER);
		fragmentShaderProgram = gl.glCreateShader(GL2.GL_FRAGMENT_SHADER);

		String[] vsrc = loadShaderSrc("julia.vs");
		gl.glShaderSource(vertexShaderProgram, 1, vsrc, null, 0);
		gl.glCompileShader(vertexShaderProgram);

		String[] fsrc = loadShaderSrc("julia.fs");
		gl.glShaderSource(fragmentShaderProgram, 1, fsrc, null, 0);
		gl.glCompileShader(fragmentShaderProgram);

		shaderprogram = gl.glCreateProgram();
		gl.glAttachShader(shaderprogram, vertexShaderProgram);
		gl.glAttachShader(shaderprogram, fragmentShaderProgram);
		gl.glLinkProgram(shaderprogram);
		gl.glValidateProgram(shaderprogram);
		IntBuffer intBuffer = IntBuffer.allocate(1);
		gl.glGetProgramiv(shaderprogram, GL2.GL_LINK_STATUS,intBuffer);
		if (intBuffer.get(0)!=1){
			gl.glGetProgramiv(shaderprogram, GL2.GL_INFO_LOG_LENGTH,intBuffer);
			int size = intBuffer.get(0);
			System.err.println("Program link error: ");
			if (size>0){
				ByteBuffer byteBuffer = ByteBuffer.allocate(size);
				gl.glGetProgramInfoLog(shaderprogram, size, intBuffer, byteBuffer);
				for (byte b:byteBuffer.array()){
					System.err.print((char)b);
				}
			} else {
				System.out.println("Unknown error");
			}
			System.exit(1);
		}
		gl.glUseProgram(shaderprogram);
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		GL2 gl = drawable.getGL().getGL2();
        GLU glu = new GLU();

        if (height <= 0) { // avoid a divide by zero error!

            height = 1;
        }
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0, 1, 0, 1);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

    public void display(GLAutoDrawable drawable) {
		fpsStat.timerStart();
		GL2 gl = drawable.getGL().getGL2();

		if (updateUniformVars){
			updateUniformVars(gl);
		}

		// Reset the current matrix to the "identity"
		gl.glLoadIdentity();

        // Draw A Quad
        gl.glBegin(GL2.GL_QUADS);
		{
			gl.glTexCoord2f(0.0f, 0.0f);
			gl.glVertex3f(0.0f, 1.0f, 1.0f);
			gl.glTexCoord2f(1.0f, 0.0f);
			gl.glVertex3f(1.0f, 1.0f, 1.0f);
			gl.glTexCoord2f(1.0f, 1.0f);
			gl.glVertex3f(1.0f, 0.0f, 1.0f);
			gl.glTexCoord2f(0.0f, 1.0f);
			gl.glVertex3f(0.0f, 0.0f, 1.0f);
		}
		// Done Drawing The Quad
        gl.glEnd();

        // Flush all drawing operations to the graphics card
        gl.glFlush();

        File file=null;
        synchronized (monitor)
		{
			file = f;
		}
        if(file!=null)
		{
			java.awt.image.BufferedImage bimg=new java.awt.image.BufferedImage(getComponent().getWidth(),getComponent().getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
			Graphics g=bimg.getGraphics();
			ByteBuffer buffer = com.jogamp.opengl.util.GLBuffers.newDirectByteBuffer(getWidth() * getHeight() * 4);
			// be sure you are reading from the right fbo (here is supposed to be the default one)
			// bind the right buffer to read from
			gl.glReadBuffer(com.jogamp.opengl.GL.GL_BACK);
			// if the width is not multiple of 4, set unpackPixel = 1
			gl.glReadPixels(0, 0, getWidth(), getHeight(), com.jogamp.opengl.GL.GL_RGBA, com.jogamp.opengl.GL.GL_UNSIGNED_BYTE, buffer);

			for (int h = 0; h < getHeight(); h++)
			{
				for (int w = 0; w < getWidth(); w++)
				{
					// The color are the three consecutive bytes, it's like referencing
					// to the next consecutive array elements, so we got red, green, blue..
					// red, green, blue, and so on..+ ", "
					g.setColor(new Color((buffer.get() & 0xff), (buffer.get() & 0xff),
							(buffer.get() & 0xff)));
					buffer.get();   // consume alpha
					g.drawRect(w, getHeight() - h-1, 1, 1); // height - h is for flipping the image
				}
			}
			// This is one util of mine, it make sure you clean the direct buffer
			buffer.clear();
			synchronized (monitor)
			{
				f = null;
			}
			try
			{
				javax.imageio.ImageIO.write(bimg,file.getName().substring(file.getName().lastIndexOf(".")+1), file);
			}
			catch (Throwable exp)
			{
				de.elbosso.util.Utilities.handleException(null, getComponent(), exp);
			}
			g.dispose();
		}
		fpsStat.timerEnd();
    }

	private void updateUniformVars(GL2 gl) {
        // get memory address of uniform shader variables
		int mandel_x = gl.glGetUniformLocation(shaderprogram, "mandel_x");
		int mandel_y = gl.glGetUniformLocation(shaderprogram, "mandel_y");
		int mandel_reC = gl.glGetUniformLocation(shaderprogram, "mandel_reC");
		int mandel_imC = gl.glGetUniformLocation(shaderprogram, "mandel_imC");
		int mandel_width = gl.glGetUniformLocation(shaderprogram, "mandel_width");
		int mandel_height = gl.glGetUniformLocation(shaderprogram, "mandel_height");
		int mandel_iterations = gl.glGetUniformLocation(shaderprogram, "mandel_iterations");
		assert(mandel_x!=-1);
		assert(mandel_y!=-1);
		assert(mandel_reC!=-1);
		assert(mandel_imC!=-1);
		assert(mandel_width!=-1);
		assert(mandel_height!=-1);
		assert(mandel_iterations!=-1);
        // set uniform shader variables
		gl.glUniform1f(mandel_x, settings.getXAsFloat());
		gl.glUniform1f(mandel_y, settings.getYAsFloat());
		gl.glUniform1f(mandel_reC, settings.getReCAsFloat());
		gl.glUniform1f(mandel_imC, settings.getImCAsFloat());
		gl.glUniform1f(mandel_width, settings.getWidthAsFloat());
		gl.glUniform1f(mandel_height, settings.getHeightAsFloat());
		gl.glUniform1f(mandel_iterations, settings.getIterations());

	}

	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
    }

	public void pauseAnimation() {
		animator.stop();
	}

	public void resumeAnimation() {
		animator.start();
	}

	public Component getComponent() {
		return this;
	}

	public FPSStat getStats() {
		return fpsStat;
	}

	@Override
	public void screenshot(File f)
	{
		synchronized (monitor)
		{
			this.f = f;
		}
	}
}


