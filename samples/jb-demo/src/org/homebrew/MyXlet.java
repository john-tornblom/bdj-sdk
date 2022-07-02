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
	try {
	    logDir("/app0");
	} catch (Throwable t) {
	    logUI.log(t);
	}
    }

    private void logDir(String path) {
	PrivilegedFile dir = new PrivilegedFile(path);
	if(!dir.isDirectory()) {
	    logFile(path);
	    return;
	}

	String[] listing = dir.list();
	for(int i=0; i<listing.length; i++) {
	    path = dir.getPath() + "/" + listing[i];
	    logFile(path);
	}
    }

    private void logFile(String path) {
	PrivilegedFile file = new PrivilegedFile(path);

	/*
	StringBuffer sb = new StringBuffer();

	if(file.isDirectory()) {
	    sb.append("d");
	} else if(!file.isFile()) {
	    sb.append("c");
	} else {
	    sb.append("-");
	}
	
	if(file.canRead()) {
	    sb.append("r");
	} else {
	    sb.append("-");
	}
	
	if(file.canWrite()) {
	    sb.append("w");
	} else {
	    sb.append("-");
	}

	sb.append("   ");
	sb.append(Long.toString(file.length()));
	sb.append("   ");
	sb.append(file.getName());
	*/
	
	if(file.isDirectory()) {
	    logDir(path);
	} else {
	    logUI.log(file.getPath());
	}
    }
}

