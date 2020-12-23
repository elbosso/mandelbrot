package de.elbosso.scratch.mortennobel.mandelbrot;

//https://blog.nobel-joergensen.com/2010/02/23/real-time-mandelbrot-in-java-%E2%80%93-part-2-jogl/

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;

public class Mandelbrot extends JFrame implements ChangeListener
,java.awt.event.ComponentListener
	,de.netsysit.util.pattern.command.ResetAction.Resetable
	,java.awt.event.MouseWheelListener
	,java.awt.event.MouseListener
{
	//interesting points:
	//0.42483058528569073;-0.2134979507302626
	private final static String[] EXPORTSUFFIXES =javax.imageio.ImageIO.getWriterFileSuffixes();

	private MandelbrotAnimation anim;
	private JPanel subPanel = new JPanel(new GridLayout(0,1));
	private JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
	private JRadioButton java2dCheckbox = new JRadioButton("Java2D",true);
	private JRadioButton juliajava2dCheckbox = new JRadioButton("Julia Java2D",true);
	private JRadioButton juliajava2dThreadedCheckbox = new JRadioButton("Julia Java2D Threaded",true);
	private JRadioButton java2dThreadedCheckbox = new JRadioButton("Java2D Threaded",true);
	private JRadioButton joglCheckbox = new JRadioButton("JOGL single",false);
	private JRadioButton joglThaslerCheckbox = new JRadioButton("JOGL double",false);
	private JRadioButton juliaJoglCheckbox = new JRadioButton("Julia JOGL single",false);
	private JRadioButton juliaJoglDoubleCheckbox = new JRadioButton("Julia JOGL double",false);
	private JComboBox iterations = new JComboBox(new Object[]{32,64,128,256,512,1024,2048,4096,10000, 20000});
	private JComboBox movementSpeed = new JComboBox(new Object[]{2,4,6,8,10,15,20});

	private ButtonGroup buttonGroup = new ButtonGroup();
	private JLabel fpsLabel = new JLabel("");

	private MandelbrotJava2D mandelbrotJava2D;
	private MandelbrotJava2DThreaded mandelbrotJava2DThreaded;
	private JuliaJava2D juliaJava2D;
	private JuliaJava2DThreaded juliaJava2DThreaded;
	private MandelbrotJOGL mandelbrotJOGL;//3.073639559620469E-5
	private MandelbrotThaslerJOGL mandelbrotThaslerJOGL;//0.296909197039668
	private JuliaJOGL juliaJOGL;//3.073639559620469E-5
	private JuliaDoubleJOGL juliaDoubleJOGL;//3.073639559620469E-5
	private MandelbrotRender currentRendere;
	private javax.swing.JPanel topLevel;
	private javax.swing.Action resetAction;
	private javax.swing.Action snapshotPngAction;
	private de.elbosso.util.pattern.command.StartStopActionPair startStopActionPair;
	private de.netsysit.util.threads.CubbyHole ch;
	private double zoom=1.0f;
	private double centerX;
	private double centerY;

	public Mandelbrot() throws IOException
	{
		super();

		System.out.println("available Threads for multithreading: "+(Runtime.getRuntime().availableProcessors()+1));
		ch=new de.elbosso.util.threads.StopAndGoCubbyHole();
		anim = new MandelbrotAnimation(this,ch);
		centerX=anim.getSetting().getX();
		centerY=anim.getSetting().getY();
		mandelbrotJava2D = new MandelbrotJava2D(anim.getSetting());
		mandelbrotJava2DThreaded = new MandelbrotJava2DThreaded(anim.getSetting());
		mandelbrotJOGL = new MandelbrotJOGL(anim.getSetting());
		mandelbrotThaslerJOGL = new MandelbrotThaslerJOGL(anim.getSetting());
		juliaJava2D = new JuliaJava2D(anim.getSetting());
		juliaJava2DThreaded = new JuliaJava2DThreaded(anim.getSetting());
		juliaJOGL = new JuliaJOGL(anim.getSetting());
		juliaDoubleJOGL = new JuliaDoubleJOGL(anim.getSetting());

		setSize(800,800);
		topLevel=new javax.swing.JPanel(new BorderLayout());
		setContentPane(topLevel);
		setRenderPanel(juliaJava2D);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		createActions();
		topLevel.add(subPanel, BorderLayout.SOUTH);
		javax.swing.JToolBar tb=new javax.swing.JToolBar();
		tb.setFloatable(false);
		tb.add(resetAction);
		tb.addSeparator();
		tb.add(startStopActionPair.getStartAction());
		tb.add(startStopActionPair.getStopAction());
		tb.addSeparator();
		tb.add(snapshotPngAction);
		topLevel.add(tb, BorderLayout.NORTH);
		buttonGroup.add(java2dCheckbox);
		buttonGroup.add(java2dThreadedCheckbox);
		buttonGroup.add(juliajava2dCheckbox);
		buttonGroup.add(juliajava2dThreadedCheckbox);
		buttonGroup.add(joglCheckbox);
		buttonGroup.add(joglThaslerCheckbox);
		buttonGroup.add(juliaJoglCheckbox);
		buttonGroup.add(juliaJoglDoubleCheckbox);
		subPanel.add(fpsLabel);
		fpsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		controlPanel.add(new JLabel("Render"));
		controlPanel.add(java2dCheckbox);
		controlPanel.add(java2dThreadedCheckbox);
		controlPanel.add(juliajava2dCheckbox);
		controlPanel.add(juliajava2dThreadedCheckbox);
		controlPanel.add(joglCheckbox);
		controlPanel.add(joglThaslerCheckbox);
		controlPanel.add(juliaJoglCheckbox);
		controlPanel.add(juliaJoglDoubleCheckbox);
		juliajava2dCheckbox.setSelected(true);
		controlPanel.add(new JLabel("# Iterations"));
		controlPanel.add(iterations);
		iterations.setSelectedItem(anim.getSetting().getIterations());
		iterations.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int iter = (Integer) iterations.getSelectedItem();
				anim.getSetting().setIterations(iter);
				mandelbrotJava2D.getStats().clear();
				mandelbrotJOGL.getStats().clear();
				mandelbrotThaslerJOGL.getStats().clear();
				mandelbrotJava2DThreaded.getStats().clear();
				juliaJava2D.getStats().clear();
				juliaJava2DThreaded.getStats().clear();
				juliaJOGL.getStats().clear();
				juliaDoubleJOGL.getStats().clear();
				stateChanged(null);
			}
		});
		controlPanel.add(new JLabel("Movement speed"));
		controlPanel.add(movementSpeed);
		movementSpeed.setSelectedItem(anim.getMovementSpeed());
		movementSpeed.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int speed = (Integer) movementSpeed.getSelectedItem();
				anim.setMovementSpeed(speed);
			}
		});

		subPanel.add(controlPanel);


		ActionListener al = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (joglCheckbox.isSelected()){
					setRenderPanel(mandelbrotJOGL);
				} else if (joglThaslerCheckbox.isSelected()){
					setRenderPanel(mandelbrotThaslerJOGL);
				} else if (java2dThreadedCheckbox.isSelected()){
					setRenderPanel(mandelbrotJava2DThreaded);
				} else if (juliajava2dCheckbox.isSelected()){
					setRenderPanel(juliaJava2D);
				} else if (juliajava2dThreadedCheckbox.isSelected()){
					setRenderPanel(juliaJava2DThreaded);
				} else if (juliaJoglCheckbox.isSelected()){
					setRenderPanel(juliaJOGL);
				} else if (juliaJoglDoubleCheckbox.isSelected()){
					setRenderPanel(juliaDoubleJOGL);
				} else {
					setRenderPanel(mandelbrotJava2D);
				}
			}
		};

		joglCheckbox.addActionListener(al);
		joglThaslerCheckbox.addActionListener(al);
		java2dCheckbox.addActionListener(al);
		java2dThreadedCheckbox.addActionListener(al);
		juliajava2dCheckbox.addActionListener(al);
		juliajava2dThreadedCheckbox.addActionListener(al);
		juliaJoglCheckbox.addActionListener(al);
		juliaJoglDoubleCheckbox.addActionListener(al);
		anim.start();
		setVisible(true);
	}

 	private boolean exportImg(java.io.File file)
	{
		boolean rv = true;
		if(anim.isRunning()==false)
		{
			ch.put(Boolean.TRUE);
			ch.put(null);
		}
		currentRendere.screenshot(file);
		return rv;
	}
	private void createActions()
	{
		resetAction=new de.netsysit.util.pattern.command.ResetAction(this);
		startStopActionPair=new de.elbosso.util.pattern.command.StartStopActionPair(anim);
		de.netsysit.util.pattern.command.FileProcessor exportImgClient =
				new de.netsysit.util.pattern.command.FileProcessor()
				{
					public boolean process(java.io.File[] files)
					{
						return exportImg(files[0]);
					}
				};
		de.netsysit.util.pattern.command.ChooseFileAction img = null;
		if (de.netsysit.util.ResourceLoader.getResource("de/netsysit/ressources/gfx/ca/screenshot_48.png") != null)
		{
			img = new de.netsysit.util.pattern.command.ChooseFileAction(exportImgClient, /*i18n.getString("ImageViewer.*/"snapshotPngAction"/*.text")*/, new javax.swing.ImageIcon(de.netsysit.util.ResourceLoader.getImgResource("de/netsysit/ressources/gfx/ca/screenshot_48.png")));
		}
		else
		{
			img = new de.netsysit.util.pattern.command.ChooseFileAction(exportImgClient, /*i18n.getString("ImageViewer.*/"snapshotPngAction"/*.text")*/, new javax.swing.ImageIcon(de.netsysit.util.ResourceLoader.getImgResource("image/drawable-mdpi/ic_camera_alt_black_48dp.png")));
		}
//					de.netsysit.db.ui.Utilities.configureOpenFileChooser(img.getFilechooser());
		img.setAllowedSuffixes(EXPORTSUFFIXES);
		img.setSaveDialog(true);
		img.setDefaultFileEnding(".png");
//		img.putValue(javax.swing.Action.SHORT_DESCRIPTION, i18n.getString("ImageViewer.snapshotPngAction.tooltip"));
		snapshotPngAction = img;
	}
	private void setRenderPanel(MandelbrotRender newRendere){
		assert(newRendere!=null);
		if (newRendere==currentRendere){
			return;
		}
		if (currentRendere!=null){
			currentRendere.pauseAnimation();
			currentRendere.getComponent().removeComponentListener(this);
			currentRendere.getComponent().removeMouseWheelListener(this);
			currentRendere.getComponent().removeMouseListener(this);
			topLevel.remove(currentRendere.getComponent());
		}
		currentRendere = newRendere;
		currentRendere.resumeAnimation();
		topLevel.add(currentRendere.getComponent(), BorderLayout.CENTER);
		currentRendere.getComponent().addComponentListener(this);
		currentRendere.getComponent().addMouseWheelListener(this);
		currentRendere.getComponent().addMouseListener(this);
		if(anim.isRunning()==false)
		{
			ch.put(Boolean.TRUE);
			ch.put(null);
		}
	}

	public static void main(String[] args) throws java.io.IOException{
		try
		{
			java.util.Properties iconFallbacks = new java.util.Properties();
			java.io.InputStream is=de.netsysit.util.ResourceLoader.getResource("de/elbosso/ressources/data/icon_trans_material.properties").openStream();
			iconFallbacks.load(is);
			is.close();
			de.netsysit.util.ResourceLoader.configure(iconFallbacks);
		}
		catch(java.io.IOException ioexp)
		{
			ioexp.printStackTrace();
		}

		de.netsysit.util.ResourceLoader.setSize(de.netsysit.util.ResourceLoader.IconSize.small);

		java.util.Properties iconFallbacks = new java.util.Properties();
//		iconFallbacks.setProperty("de/netsysit/ressources/gfx/common/Reset24.gif", "av/drawable-mdpi/ic_replay_black_48dp.png");
//		iconFallbacks.setProperty("toolbarButtonGraphics/media/Play24.gif", "av/drawable-mdpi/ic_play_arrow_black_48dp.png");
//		iconFallbacks.setProperty("toolbarButtonGraphics/media/Stop24.gif", "av/drawable-mdpi/ic_stop_black_48dp.png");
//		iconFallbacks.setProperty("de/netsysit/ressources/gfx/common/Reset24.gif", "av/drawable-mdpi/ic_replay_black_48dp.png");
		de.netsysit.util.ResourceLoader.configure(iconFallbacks);


		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				try
				{
					new Mandelbrot();
				} catch (IOException e)
				{
					de.elbosso.util.Utilities.handleException(null,e);
				}
			}
		});
	}

	private void updateFps(){
		float avarageRendertimeMillis = currentRendere.getStats().getAvarageRendertime();
		float averagefps = 1000/avarageRendertimeMillis;
		float lastRendertimeMillis=currentRendere.getStats().getLastRendertime();
		float lastfps = 1000/lastRendertimeMillis;
		String fpsString = String.format("avg: Rendertime %4.1fms  FPS %4.1f; current: Rendertime %4.1fms  FPS %4.1f",avarageRendertimeMillis,averagefps,lastRendertimeMillis,lastfps);
		fpsLabel.setText(fpsString);
	}

	public void stateChanged(ChangeEvent e) {
		if (((currentRendere==mandelbrotJava2D)||(currentRendere==juliaJava2DThreaded))||((currentRendere==mandelbrotJava2DThreaded)||(currentRendere==juliaJava2D))){
			repaint();
		}
		updateFps();
	}

	@Override
	public void componentResized(ComponentEvent e)
	{
		anim.setComponentSize(e.getComponent().getSize());
		stateChanged(null);
	}

	@Override
	public void componentMoved(ComponentEvent e)
	{

	}

	@Override
	public void componentShown(ComponentEvent e)
	{

	}

	@Override
	public void componentHidden(ComponentEvent e)
	{

	}

	@Override
	public void reset()
	{
		anim.restart();
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e)
	{
//		System.out.println("mouseWheelMoved "+anim.isRunning());
		if(anim.isRunning()==false)
		{
			if(e.getWheelRotation()<0)
				zoom *= 0.95;
			else
				zoom /= 0.95;
			double smallestsize = zoom * MandelbrotAnimation.ZOOM_FACTOR;
			double width = (float) (currentRendere.getComponent().getSize() != null ? currentRendere.getComponent().getSize().width : 800);
			double height = (float) (currentRendere.getComponent().getSize() != null ? currentRendere.getComponent().getSize().height : 800);
			double fac = width / height;
			if (fac < 1)
			{
				anim.getSetting().setHeight(smallestsize / fac);
				anim.getSetting().setWidth(smallestsize);
			}
			else
			{
				anim.getSetting().setHeight(smallestsize);
				anim.getSetting().setWidth(smallestsize * fac);
			}
			anim.getSetting().setX(centerX-anim.getSetting().getWidth()*0.5);
			anim.getSetting().setY(centerY-anim.getSetting().getHeight()*0.5);
			stateChanged(null);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if(anim.isRunning()==false)
		{
			double x=anim.getSetting().getX();
			double fac=(double)e.getX()/(double)e.getComponent().getSize().width;
			centerX=x+anim.getSetting().getWidth()*fac;
			double y=anim.getSetting().getY();
			fac=(float)e.getY()/(float)e.getComponent().getSize().height;
			centerY=y+anim.getSetting().getHeight()*fac;
			anim.getSetting().setX(centerX-anim.getSetting().getWidth()*0.5f);
			anim.getSetting().setY(centerY-anim.getSetting().getHeight()*0.5f);
			System.out.println(""+anim.getSetting().getX()+";"+anim.getSetting().getY());
			stateChanged(null);
		}
	}

	@Override
	public void mousePressed(MouseEvent e)
	{

	}

	@Override
	public void mouseReleased(MouseEvent e)
	{

	}

	@Override
	public void mouseEntered(MouseEvent e)
	{

	}

	@Override
	public void mouseExited(MouseEvent e)
	{

	}
}
