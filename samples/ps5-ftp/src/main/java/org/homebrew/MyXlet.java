package org.homebrew;

import java.awt.BorderLayout;
import java.io.PrintStream;

import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.homebrew.ftp.FtpServer;

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
			PrivilegeEscalation.disableSecurityManager();
			PrintStream os = new PrintStream(new UIOutputStream(), true);
			System.setOut(os);
			System.setErr(os);
			PrivilegeEscalation.openModuleToAllUnnamed("jdk.internal.helper.IOHelper");
			PrivilegeEscalation.openModuleToAllUnnamed("jdk.internal.helper.MathHelper");
			PrivilegeEscalation.openModuleToAllUnnamed("sun.nio.fs.UnixNativeDispatcher");
		} catch (Throwable t) {
			LoggingUI.getInstance().log(t);
		}

		FtpServer ftp = null;
		try {
			try {
				ftp = new FtpServer();
				ftp.run();
			} finally {
				if (ftp != null) {
					ftp.close();
				}
			}
		} catch (Throwable t) {
			LoggingUI.getInstance().log(t);
		}
		LoggingUI.getInstance().log("done");
	}
}


