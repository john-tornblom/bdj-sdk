package org.homebrew;

import java.awt.BorderLayout;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;

public class MyXlet extends Thread implements Xlet {
    private HScene scene;
    private LoggingUI gui;

    public void initXlet(XletContext context) {
	gui = LoggingUI.getInstance();
	gui.setSize(1280, 720);

	scene = HSceneFactory.getInstance().getDefaultHScene();
	scene.add(gui, BorderLayout.CENTER);
        scene.validate();
	scene.repaint();
    }

    public void startXlet() {
	gui.setVisible(true);
        scene.setVisible(true);
	start();
    }

    public void pauseXlet() {
	gui.setVisible(false);
    }

    public void destroyXlet(boolean unconditional) {
	interrupt();
	gui.setVisible(false);
	scene.remove(gui);
        scene = null;
    }

    public void run() {
	try {
	    Thread.sleep(5000);
	    PrivilegeEscalation.disableSecurityManager();
	    LoggingUI.getInstance().log("java.version: " + System.getProperty("java.version"));
	    LoggingUI.getInstance().log("pid: " + libkernel.getpid());
	    libkernel.sendNotificationRequest("Hello, World!");
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }
}




