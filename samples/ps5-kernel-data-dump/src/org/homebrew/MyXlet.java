package org.homebrew;

import java.awt.BorderLayout;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import java.io.PrintStream;

public class MyXlet extends Thread implements Xlet {
    private static final int PORT = 5656;
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
	    PrivilegeEscalation.disableSecurityManager();
	    KernelMemory.enableRW();
	    KernelPatching.enableDebugMenu();
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}

	try {
	    LoggingUI.getInstance().log("launching kernel dump server on port " + PORT);
	    KernelDataDumping.run(PORT);
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }
}


