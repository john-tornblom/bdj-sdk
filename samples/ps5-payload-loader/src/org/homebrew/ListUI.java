package org.homebrew;

import java.awt.Color;
import java.awt.Container;
import java.awt.Font;
import java.awt.Graphics;
import java.util.ArrayList;

public class ListUI extends Container {
    private final Font font = new Font(Font.MONOSPACED, Font.PLAIN, 22);
    private final Color fontColor = new Color(240, 240, 240);
    private final Color selectedColor = new Color(240, 240, 0);
    private final Color bgColor = new Color(5, 5, 5);
    private final ArrayList labels = new ArrayList();
    private final ArrayList items = new ArrayList();
    private final int VISIBLE_ITEMS = 26;

    private int topItem = 0;
    private int selectedItem = 0;
    private int bottomItem = VISIBLE_ITEMS;

    void addItem(String label, Object item) {
	items.add(item);
	labels.add(label);

	if(bottomItem - topItem < VISIBLE_ITEMS) {
	    bottomItem += 1;
	}
	repaint();
    }

    Object getSelected() {
	return items.get(selectedItem);
    }

    void itemUp() {
	if(selectedItem > 0) {
	    selectedItem -= 1;
	}

	if(selectedItem < topItem) {
	    topItem -= 1;
	    bottomItem -= 1;
	}
	repaint();
    }

    void itemDown() {
	if(selectedItem < items.size() - 1) {
	    selectedItem++;
	}
	if(selectedItem > bottomItem) {
	    topItem += 1;
	    bottomItem += 1;
	}
	repaint();
    }

    public void paint(Graphics g) {
	g.setFont(font);
	g.setColor(bgColor);
	g.fillRect(20, 20, getWidth() - 40, getHeight() - 40);
	g.setColor(fontColor);

	for(int i=topItem; i<=bottomItem && i<items.size(); i++) {
	    String label = (String)labels.get(i);

	    if(i == selectedItem) {
		g.setColor(selectedColor);
		label = "-> " + label;
	    } else {
		g.setColor(fontColor);
		label = "   " + label;
	    }
	    g.drawString(label, 30, 40 + ((i-topItem)*25));
	}
    }
}

