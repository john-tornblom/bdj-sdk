package org.homebrew;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.net.URL;
import java.net.URLClassLoader;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;


public class JarLoadingServer {

    public static void run(int port) throws IOException {
        ServerSocket ss = new ServerSocket(port);
        ss.setReuseAddress(true);
	
        while (true) {
            try {
                serve(ss.accept());
            } catch (Throwable t) {
		LoggingUI.getInstance().log(t);
            }
        }
    }

    private static void serve(final Socket s) throws Exception {
        final PrintStream err = new PrintStream(s.getOutputStream());
	s.setSoTimeout(5000);
	
        new Thread(new Runnable() {
            public void run() {
                try {
                    JarFile jarFile = readJarFile(s);
                    runJarFile(jarFile, s.getOutputStream());
                } catch (Throwable t) {
                    t.printStackTrace(err);
                }

                try {
                    s.close();
                } catch (Throwable t) {
		    LoggingUI.getInstance().log(t);
                }
            }

        }).start();
    }

    private static JarFile readJarFile(Socket s) throws IOException {
	Path jarPath = Files.createTempFile("JarLoading", ".jar");
	OutputStream jarOut = Files.newOutputStream(jarPath);
	InputStream jarIn = s.getInputStream();
	byte[] buf = new byte[8192];
	int length = 0;
	
	jarPath.toFile().deleteOnExit();

	try {
	    while((length = jarIn.read(buf)) != -1) {
		jarOut.write(buf, 0, length);
	    }
	} finally {
	    jarOut.close();
	}

	return new JarFile(jarPath.toFile());
    }

    private static void runJarFile(JarFile jarFile, OutputStream os)
	throws IOException, ClassNotFoundException, NoSuchMethodException,
	       IllegalAccessException, InvocationTargetException {
	JarEntry manifestEntry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
	InputStream manifestStream = jarFile.getInputStream(manifestEntry);
	PrintStream stdout = System.out;
	PrintStream stderr = System.err;
	
	try {
	    Manifest mf = new Manifest(manifestStream);
	    String mainClassName = mf.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
	    URL url = new File(jarFile.getName()).toURI().toURL();
	    URLClassLoader ldr = URLClassLoader.newInstance(new URL[] {url}, JarLoadingServer.class.getClassLoader());
	    Class mainClass = ldr.loadClass(mainClassName);
	    Method mainMethod = mainClass.getDeclaredMethod("main", new Class[] {String[].class});
	    String[] args = new String[]{jarFile.getName()};

	    System.setOut(new PrintStream(os));
	    System.setErr(new PrintStream(os));
	    mainMethod.invoke(null, new Object[] {args});
	} finally {
	    System.setOut(stdout);
	    System.setErr(stderr);
	    manifestStream.close();
	    jarFile.close();
	}
    }
}
