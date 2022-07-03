package org.homebrew;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
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
	    LoggingUI.getInstance().log("To receive files, run the following command on " +
					LoggingTCP.host);	    
	    LoggingUI.getInstance().log("$ nc -l 18194 > app0.zip");
	    LoggingUI.getInstance().log("Sending...");

	    File zipFile = File.createTempFile("app0", ".zip");
	    ZipOutputStream zipOut = new ZipOutputStream(new FileOutputStream(zipFile));
	    zipDir(zipOut, "/app0");
	    zipOut.close();
	    
	    FileInputStream fis = new FileInputStream(zipFile.getPath());
	    byte[] buf = new byte[4096];

	    while(true) {
		int len = fis.read(buf, 0, buf.length);
		if(len > 0) {
		    LoggingTCP.getInstance().log(buf, len);
		} else {
		    break;
		}
	    }
	    LoggingUI.getInstance().log("Done");
	} catch (Throwable t) {
	    logUI.log(t);
	}
    }
    
    private static void zipDir(ZipOutputStream zipOut, String path) throws IOException {
	PrivilegedFile dir = new PrivilegedFile(path);
	
	if(!dir.isDirectory()) {
	    zipFile(zipOut, path);
	    return;
	}

	String[] listing = dir.list();
	for(int i=0; i<listing.length; i++) {
	    path = dir.getPath() + "/" + listing[i];
	    zipFile(zipOut, path);
	}
    }

    private static void zipFile(ZipOutputStream zipOut, String path) throws IOException {
	PrivilegedFile file = new PrivilegedFile(path);
	
	if(file.isDirectory()) {
	    zipDir(zipOut, path);
	} else {
	    byte[] data = readFile(path);
	    ZipEntry entry = new ZipEntry(path.substring(1));
	    zipOut.putNextEntry(entry);
	    zipOut.write(data, 0, data.length);
	    zipOut.closeEntry();
	}
    }

    private static byte[] readFile(String path) {
	PrivilegedFile f = new PrivilegedFile(path);
	byte[] bytes = new byte[(int)f.length()];
	
	try {
	    PrivilegedFileReader fr = new PrivilegedFileReader(f.getPath());
	    fr.read(bytes);
	} catch(Throwable t) {
	}
	return bytes;
    }
}

