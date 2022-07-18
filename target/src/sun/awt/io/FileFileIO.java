package sun.awt.io;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

public class FileFileIO {
    protected FileFileIO(String filename) {
    }

    public String getName() {
	return null;
    }
  
    public String getPath() {
	return null;
    }
    
    public InputStream getInputStream() throws IOException {
	return null;
    }
    
    public OutputStream getOutputStream() throws IOException {
	return null;
    }

    public String getAbsolutePath() {
	return null;
    }

    public String getCanonicalPath() {
	return null;
    }

    public String getParent() {
	return null;
    }
    
    public String canonPath(String path) throws IOException {
	return null;
    }
    
    public boolean exists() {
	return false;
    }
    
    public boolean canWrite() {
	return false;
    }
    
    public boolean canRead() {
	return false;
    }
    
    public boolean isFile() {
	return false;
    }
    
    public boolean isDirectory() {
	return false;
    }
    
    public boolean isAbsolute() {
	return false;
    }
    
    public long lastModified() {
	return 0;
    }
    
    public long length() {
	return 0;
    }
    
    public String[] list() {
	return null;
    }
    
    public boolean delete() {
	return false;
    }
    
    public boolean mkdir() {
	return false;
    }

    public boolean mkdirs() {
	return false;
    }
}
