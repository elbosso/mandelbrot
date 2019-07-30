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

//https://www.thasler.com/blog/blog/glsl-part2-emu

package de.elbosso.scratch.mortennobel.mandelbrot;

import com.jogamp.opengl.util.Animator;

import java.awt.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.util.FPSAnimator;
import de.netsysit.util.ResourceLoader;

public class MandelbrotThaslerJOGL extends GLCanvas implements GLEventListener, MandelbrotRender {
	private boolean updateUniformVars = true;
	private int vertexShaderProgram;
	private int fragmentShaderProgram;
	private int shaderprogram;
	private final MandelbrotSetting settings;
	private static final int FPS = 30; // Animator's target frames per second
	private final FPSAnimator animator = new FPSAnimator(this, FPS);
	//new Animator(this);
	private java.lang.Object monitor=new java.lang.Object();
	private java.io.File f;
	private float zoom=128;

	private FPSStat fpsStat = new FPSStat();

	public MandelbrotThaslerJOGL(MandelbrotSetting settings) {
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

		String[] vsrc = loadShaderSrc("mandelbrotd2.vs");
		gl.glShaderSource(vertexShaderProgram, 1, vsrc, null, 0);
		gl.glCompileShader(vertexShaderProgram);

		String[] fsrc = loadShaderSrc("mandelbrotd2.fs");
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

		java.io.File file=null;
		synchronized (monitor)
		{
			file = f;
		}
		if(file!=null)
		{
			java.awt.image.BufferedImage bimg=new java.awt.image.BufferedImage(getComponent().getWidth(),getComponent().getHeight(), java.awt.image.BufferedImage.TYPE_INT_ARGB);
			java.awt.Graphics g=bimg.getGraphics();
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
		int mandel_width = gl.glGetUniformLocation(shaderprogram, "mandel_width");
		int mandel_height = gl.glGetUniformLocation(shaderprogram, "mandel_height");
		int mandel_iterations = gl.glGetUniformLocation(shaderprogram, "mandel_iterations");
		assert(mandel_x!=-1);
		assert(mandel_y!=-1);
		assert(mandel_width!=-1);
		assert(mandel_height!=-1);
		assert(mandel_iterations!=-1);
		// set uniform shader variables
		gl.glUniform2f(mandel_x, settings.getXAsFloat(),(float)(settings.getX()-(double)settings.getXAsFloat()));
		gl.glUniform2f(mandel_y, settings.getYAsFloat(),(float)(settings.getY()-(double)settings.getYAsFloat()));
		gl.glUniform2f(mandel_width, settings.getWidthAsFloat(),(float)(settings.getWidth()-(double)settings.getWidthAsFloat()));
		gl.glUniform2f(mandel_height, settings.getHeightAsFloat(),(float)(settings.getHeight()-(double)settings.getHeightAsFloat()));
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
	public void screenshot(java.io.File f)
	{
		synchronized (monitor)
		{
			this.f = f;
		}
	}
}


