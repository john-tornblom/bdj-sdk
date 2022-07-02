package org.homebrew;

import java.awt.BorderLayout;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

public class MyXlet extends Thread implements Xlet {
    private HScene scene;
    private LoggingUI logUI;
    
    public void initXlet(XletContext context) {
	logUI = LoggingUI.getInstance();
	logUI.setSize(1280, 720);
	
	scene = HSceneFactory.getInstance().getDefaultHScene();
	scene.add(logUI, BorderLayout.CENTER);
        scene.validate();
	scene.repaint();
    }
    
    public void startXlet() {
	logUI.setVisible(true);
        scene.setVisible(true);
	start();
    }
    
    public void pauseXlet() {
	logUI.setVisible(false);
    }
    
    public void destroyXlet(boolean unconditional) {
	interrupt();
	logUI.setVisible(false);
	scene.remove(logUI);
        scene = null;
    }
    
    public void run() {
	logUI.log("Hello, World!");
    }
}



