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


public class MyXlet implements Xlet {
    private static Font font;
    private HScene scene;
    private Container gui;
    private Thread thread;
    
    public void initXlet(XletContext context) {
        final Font font = new Font(null, Font.PLAIN, 72);
	final Color font_color = new Color(240, 240, 240);
	final Color bg_color = new Color(5, 5, 5);
	final String message = "Hello, World!";
		
	scene = HSceneFactory.getInstance().getDefaultHScene();
        gui = new Container() {
		public void paint(Graphics g) {
		    g.setFont(font);
		    g.setColor(bg_color);
		    g.fillRect(20, 20, getWidth() - 40, getHeight() - 40);

		    g.setColor(font_color);
		    int message_width = g.getFontMetrics().stringWidth(message);
		    g.drawString(message, (getWidth() - message_width) / 2, 320);
		}
	    };

	thread = new Thread(new Runnable() {
		public void run() {
		    boolean running = true;

		    while(running) {
			gui.repaint();
			scene.repaint();
			try {
			    Thread.sleep(500);
			} catch(InterruptedException ex) {
			    running = false;
			}
		    }
		}
	    });

        gui.setSize(1280, 720);
        scene.add(gui, BorderLayout.CENTER);
        scene.validate();
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
