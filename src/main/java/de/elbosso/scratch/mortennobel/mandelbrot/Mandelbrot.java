package de.elbosso.scratch.mortennobel.mandelbrot;

//https://blog.nobel-joergensen.com/2010/02/23/real-time-mandelbrot-in-java-%E2%80%93-part-2-jogl/

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;

public class Mandelbrot extends JFrame implements ChangeListener
,java.awt.event.ComponentListener
	,de.netsysit.util.pattern.command.ResetAction.Resetable
	,java.awt.event.MouseWheelListener
	,java.awt.event.MouseListener
{
	static
{
		try
		{
			java.util.Properties iconFallbacks = new java.util.Properties();
			iconFallbacks.setProperty("de/netsysit/ressources/gfx/ca/screenshot_48.png", "image/drawable-mdpi/ic_photo_camera_black_48dp.png");
			iconFallbacks.setProperty("de/netsysit/ressources/gfx/common/Reset24.gif", "navigation/drawable-mdpi/ic_refresh_black_48dp.png");
			iconFallbacks.setProperty("toolbarButtonGraphics/media/Play24.gif", "av/drawable-mdpi/ic_play_arrow_black_48dp.png");
			iconFallbacks.setProperty("toolbarButtonGraphics/media/Stop24.gif", "av/drawable-mdpi/ic_stop_black_48dp.png");
			de.netsysit.util.ResourceLoader.configure(iconFallbacks);
		}
		catch(java.io.IOException ioexp)
		{
			ioexp.printStackTrace();
		}
}


	private final static String[] EXPORTSUFFIXES =javax.imageio.ImageIO.getWriterFileSuffixes();

	private MandelbrotAnimation anim;
	private JPanel subPanel = new JPanel(new GridLayout(0,1));
	private JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
	private JRadioButton java2dCheckbox = new JRadioButton("Java2D",true);
	private JRadioButton joglCheckbox = new JRadioButton("JOGL",false);
	private JComboBox iterations = new JComboBox(new Object[]{32,64,128,256,512,1024,2048,4096});
	private JComboBox movementSpeed = new JComboBox(new Object[]{2,4,6,8,10,15,20});

	private ButtonGroup buttonGroup = new ButtonGroup();
	private JLabel fpsLabel = new JLabel("");

	private MandelbrotJava2D mandelbrotJava2D;
	private MandelbrotJOGL mandelbrotJOGL;
	private MandelbrotRender currentRendere;
	private javax.swing.JPanel topLevel;
	private javax.swing.Action resetAction;
	private javax.swing.Action snapshotPngAction;
	private de.elbosso.util.pattern.command.StartStopActionPair startStopActionPair;
	private de.netsysit.util.threads.CubbyHole ch;
	private float zoom=1.0f;
	private float centerX;
	private float centerY;

	public Mandelbrot() {
		super();
		ch=new de.elbosso.util.threads.StopAndGoCubbyHole();
		anim = new MandelbrotAnimation(this,ch);
		mandelbrotJava2D = new MandelbrotJava2D(anim.getSetting());
		mandelbrotJOGL = new MandelbrotJOGL(anim.getSetting());
		setSize(800,800);
		topLevel=new javax.swing.JPanel(new BorderLayout());
		setContentPane(topLevel);
		setRenderPanel(mandelbrotJava2D);
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
		buttonGroup.add(joglCheckbox);
		subPanel.add(fpsLabel);
		fpsLabel.setHorizontalAlignment(SwingConstants.CENTER);
		controlPanel.add(new JLabel("Render"));
		controlPanel.add(java2dCheckbox);
		controlPanel.add(joglCheckbox);
		controlPanel.add(new JLabel("# Iterations"));
		controlPanel.add(iterations);
		iterations.setSelectedItem(anim.getSetting().getIterations());
		iterations.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				int iter = (Integer) iterations.getSelectedItem();
				anim.getSetting().setIterations(iter);
				mandelbrotJava2D.getStats().clear();
				mandelbrotJOGL.getStats().clear();
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
				if (e.getSource()==joglCheckbox){
					setRenderPanel(mandelbrotJOGL);
				} else {
					setRenderPanel(mandelbrotJava2D);
				}
			}
		};

		joglCheckbox.addActionListener(al);
		java2dCheckbox.addActionListener(al);
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
		resetAction.putValue(Action.SHORT_DESCRIPTION,"Reset");
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
			img = new de.netsysit.util.pattern.command.ChooseFileAction(exportImgClient, /*i18n.getString("ImageViewer.*/"snapshotPngAction"/*.text")*/, null);
		}
		startStopActionPair.getStopAction().putValue(Action.SHORT_DESCRIPTION,"Stop animation");
		startStopActionPair.getStartAction().putValue(Action.SHORT_DESCRIPTION,"Start animation");

//					de.netsysit.db.ui.Utilities.configureOpenFileChooser(img.getFilechooser());
		img.setAllowedSuffixes(EXPORTSUFFIXES);
		img.setSaveDialog(true);
		img.setDefaultFileEnding(".png");
//		img.putValue(javax.swing.Action.SHORT_DESCRIPTION, i18n.getString("ImageViewer.snapshotPngAction.tooltip"));
		snapshotPngAction = img;
		snapshotPngAction.putValue(Action.SHORT_DESCRIPTION,"Screenshot (as PNG)");
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

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				new Mandelbrot();
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
		if (currentRendere==mandelbrotJava2D){
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
		if(anim.isRunning()==false)
		{
			if(e.getWheelRotation()<0)
				zoom *= 0.95f;
			else
				zoom /= 0.95f;
			float smallestsize = zoom * MandelbrotAnimation.ZOOM_FACTOR;
			float width = (float) (currentRendere.getComponent().getSize() != null ? currentRendere.getComponent().getSize().width : 800);
			float height = (float) (currentRendere.getComponent().getSize() != null ? currentRendere.getComponent().getSize().height : 800);
			float fac = width / height;
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
			anim.getSetting().setX(centerX-anim.getSetting().getWidth()*0.5f);
			anim.getSetting().setY(centerY-anim.getSetting().getHeight()*0.5f);
			stateChanged(null);
		}
	}

	@Override
	public void mouseClicked(MouseEvent e)
	{
		if(anim.isRunning()==false)
		{
			float x=anim.getSetting().getX();
			float fac=(float)e.getX()/(float)e.getComponent().getSize().width;
			centerX=x+anim.getSetting().getWidth()*fac;
			float y=anim.getSetting().getY();
			fac=(float)e.getY()/(float)e.getComponent().getSize().height;
			centerY=y+anim.getSetting().getHeight()*fac;
			anim.getSetting().setX(centerX-anim.getSetting().getWidth()*0.5f);
			anim.getSetting().setY(centerY-anim.getSetting().getHeight()*0.5f);
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
