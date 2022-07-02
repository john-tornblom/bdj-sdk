package org.homebrew;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

public class LoggingUI extends Container {
    private final Font font = new Font(null, Font.PLAIN, 14);
    private final Color fontColor = new Color(240, 240, 240);
    private final Color bgColor = new Color(5, 5, 5);
    private final ArrayList rows = new ArrayList();
    private static LoggingUI instance = null;

    private LoggingUI() {
    }

    public static LoggingUI getInstance() {
	if(instance == null) {
	    instance = new LoggingUI();
	}

	return instance;
    }

    public void paint(Graphics g) {
	g.setFont(font);
	g.setColor(bgColor);
	g.fillRect(20, 20, getWidth() - 40, getHeight() - 40);
	g.setColor(fontColor);

	for(int i=0; i<rows.size(); i++) {
	    String row = (String)rows.get(i);
	    int len = g.getFontMetrics().stringWidth(row);
	    g.drawString(row, 30, 40 + (i*15));
	}
    }

    public void log(String msg) {
	rows.add(msg);
	if(rows.size() > 45) {
	    rows.remove(0);
	}
	repaint();
    }

    public void log(Throwable t) {
	StringWriter sw = new StringWriter();
	PrintWriter pw = new PrintWriter(sw);
	StringBuffer sb = new StringBuffer();

	t.printStackTrace(pw);
	String st = sw.toString();

	for(int i=0; i<st.length(); i++) {
	    char ch = st.charAt(i);

	    switch(ch) {
	    case '\t':
		sb.append("    ");
		break;

	    case '\n':
		log(sb.toString());
		sb = new StringBuffer();
		break;

	    default:
		sb.append(ch);
		break;
	    }
	}

	if(sb.length() > 0) {
	    log(sb.toString());
	}
    }
}

