package org.homebrew;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
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


public class JarLoading {

    public static void runJar(byte[] bytes, OutputStream os) throws Exception {
	Path jarPath = Files.createTempFile("JarLoading", ".jar");
	OutputStream jarOut = Files.newOutputStream(jarPath);

	jarPath.toFile().deleteOnExit();

	try {
	    jarOut.write(bytes);
	} finally {
	    jarOut.close();
	}

	runJar(new JarFile(jarPath.toFile()), os);
    }

    public static void runJar(JarFile jarFile, OutputStream os) throws Exception {
	JarEntry manifestEntry = jarFile.getJarEntry("META-INF/MANIFEST.MF");
	InputStream manifestStream = jarFile.getInputStream(manifestEntry);
	PrintStream stdout = System.out;
	PrintStream stderr = System.err;

	try {
	    Manifest mf = new Manifest(manifestStream);
	    String mainClassName = mf.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
	    URL url = new File(jarFile.getName()).toURI().toURL();
	    URLClassLoader ldr = URLClassLoader.newInstance(new URL[] {url}, JarLoading.class.getClassLoader());
	    Class mainClass = ldr.loadClass(mainClassName);
	    Method mainMethod = mainClass.getDeclaredMethod("main", new Class[] {String[].class});
	    String[] args = new String[]{jarFile.getName()};

	    if(os != null) {
		System.setOut(new PrintStream(os));
		System.setErr(new PrintStream(os));
	    }
	    mainMethod.invoke(null, new Object[] {args});
	} finally {
	    System.setOut(stdout);
	    System.setErr(stderr);
	    manifestStream.close();
	    jarFile.close();
	}
    }

    public static void spawnServer(int port) throws Exception {
	new LoadingServer(port) {
	    public void runPayload(byte[] bytes, OutputStream os) throws Exception {
		JarLoading.runJar(bytes, os);
	    }
	}.spawn();
    }
}
