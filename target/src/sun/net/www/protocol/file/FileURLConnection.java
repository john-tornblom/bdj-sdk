package sun.net.www.protocol.file;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.net.URL;

public class FileURLConnection {

    protected FileURLConnection(URL url, File file) {
    }
    
    public void connect() throws IOException {
    }

    public synchronized InputStream getInputStream() throws IOException {
	return null;
    }
}
