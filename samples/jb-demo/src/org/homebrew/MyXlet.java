package org.homebrew;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.BorderLayout;

import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import java.util.ArrayList;

public class MyXlet implements Xlet {
    private static Font font;
    private HScene scene;
    private Container gui;
    private Thread thread;
    private final ArrayList rows = new ArrayList();
    
    public void initXlet(XletContext context) {
        final Font font = new Font(null, Font.PLAIN, 22);
	final Color font_color = new Color(240, 240, 240);
	final Color bg_color = new Color(5, 5, 5);
		
	scene = HSceneFactory.getInstance().getDefaultHScene();
        gui = new Container() {
		public void paint(Graphics g) {
		    g.setFont(font);
		    g.setColor(bg_color);
		    g.fillRect(20, 20, getWidth() - 40, getHeight() - 40);
		    g.setColor(font_color);

		    for(int i=0; i<rows.size(); i++) {
			String row = (String)rows.get(i);
			int len = g.getFontMetrics().stringWidth(row);
			g.drawString(row, 30, 40 + (i*26));
		    }
		}
	    };
	
	thread = new Thread(new Runnable() {
		public void run() {
		    boolean running = true;

		    while(running) {
			gui.repaint();
			scene.repaint();
			try {
			    Thread.sleep(40);
			} catch(InterruptedException ex) {
			    running = false;
			}
		    }
		}
	    });

        gui.setSize(1280, 720);
        scene.add(gui, BorderLayout.CENTER);
        scene.validate();

	log("$ ls -l /app0");
	String[] files = new PrivilegedFile("/app0").list();
	for(int i=0; i<files.length; i++) {
	    log(files[i]);
	}
    }

    public void log(String msg) {
	rows.add(msg);
	if(rows.size() > 26) {
	    rows.remove(0);
	}
    }
    
    public void startXlet() {
	gui.setVisible(true);
        scene.setVisible(true);
	thread.start();
    }

    public void pauseXlet() {
	gui.setVisible(false);
    }

    public void destroyXlet(boolean unconditional) {
	gui.setVisible(false);
	thread.interrupt();
	scene.remove(gui);
        scene = null;
    }
}
