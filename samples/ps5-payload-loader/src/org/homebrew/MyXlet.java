package org.homebrew;

import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import javax.tv.xlet.Xlet;
import javax.tv.xlet.XletContext;
import org.havi.ui.HScene;
import org.havi.ui.HSceneFactory;
import org.havi.ui.event.HRcEvent;
import org.dvb.event.EventManager;
import org.dvb.event.UserEvent;
import org.dvb.event.UserEventListener;
import org.dvb.event.UserEventRepository;

public class MyXlet implements UserEventListener, Xlet {
    private static final int ELF_PORT = 9020;
    private static final int JAR_PORT = 9025;
    private static final int LUA_PORT = 9938;

    private HScene scene;
    private LoggingUI logUI;
    private ListUI listUI;
    private UserEventRepository evtRepo;

    public void initXlet(XletContext context) {
	logUI = LoggingUI.getInstance();
	logUI.setSize(1280, 720);

	listUI = new ListUI();
	listUI.setSize(1280, 720);
	listUI.addItem("Launch ELF loading daemon on port " + (ELF_PORT + 1),
		       new Runnable() {
			   public void run() {
			       try {
				   ElfLoading.runElf("/disc/elfldr.elf");
				   libkernel.sendNotificationRequest("ELF loading daemon running on port " + (ELF_PORT + 1));
			       } catch (Throwable t) {
				   libkernel.sendNotificationRequest(t.getMessage());
				   LoggingUI.getInstance().log(t);
				   logUI.setVisible(true);
			       }
			   }
		       });
	listUI.addItem("Launch ELF loading server on port " + ELF_PORT,
		       new Runnable() {
			   public void run() {
			       try {
				   ElfLoading.spawnServer(ELF_PORT);
				   libkernel.sendNotificationRequest("ELF loading server running on port " + ELF_PORT);
			       } catch (Throwable t) {
				   libkernel.sendNotificationRequest(t.getMessage());
				   LoggingUI.getInstance().log(t);
				   logUI.setVisible(true);
			       }
			   }
		       });
	listUI.addItem("Launch JAR loading server on port " + JAR_PORT,
		       new Runnable() {
			   public void run() {
			       try {
				   JarLoading.spawnServer(JAR_PORT);
				   libkernel.sendNotificationRequest("JAR loading server running on port " + JAR_PORT);
			       } catch (Throwable t) {
				   libkernel.sendNotificationRequest(t.getMessage());
				   LoggingUI.getInstance().log(t);
				   logUI.setVisible(true);
			       }
			   }
		       });
	listUI.addItem("Launch LUA loading server on port " + LUA_PORT,
		       new Runnable() {
			   public void run() {
			       try {
				   LuaLoading.spawnServer(LUA_PORT);
				   libkernel.sendNotificationRequest("Lua loading server running on port " + LUA_PORT);
			       } catch (Throwable t) {
				   libkernel.sendNotificationRequest(t.getMessage());
				   LoggingUI.getInstance().log(t);
				   logUI.setVisible(true);
			       }
			   }
		       });
	listUI.addItem("Jailbreak BD-J player",
		       new Runnable() {
			   public void run() {
			       try {
				   KernelPatching.jailbreak();
				   libkernel.sendNotificationRequest("BD-J player escaped FreeBSD jail");
			       } catch (Throwable t) {
				   libkernel.sendNotificationRequest(t.getMessage());
				   LoggingUI.getInstance().log(t);
				   logUI.setVisible(true);
			       }
			   }
		       });
	listUI.addItem("View BD-J logs (toggle with square)",
		       new Runnable() {
			   public void run() {
			       logUI.setVisible(true);
			   }
		       });

	evtRepo = new UserEventRepository("input");
	evtRepo.addKey(HRcEvent.VK_ENTER);
	evtRepo.addKey(HRcEvent.VK_UP);
	evtRepo.addKey(HRcEvent.VK_DOWN);
	evtRepo.addKey(461); // □

	scene = HSceneFactory.getInstance().getDefaultHScene();
	scene.add(logUI, BorderLayout.CENTER);
	scene.add(listUI, BorderLayout.CENTER);
        scene.validate();
	scene.repaint();
    }

    public void startXlet() {
        scene.setVisible(true);

	try {
	    LoggingUI.getInstance().log("[*] Disabling Java security manager...");
	    PrivilegeEscalation.disableSecurityManager();
	    LoggingUI.getInstance().log("[+] Java security manager disabled");

	    LoggingUI.getInstance().log("[*] Obtaining kernel .data R/W capabilities...");
	    KernelMemory.enableRW();
	    LoggingUI.getInstance().log("[+] Kernel .data R/W achieved");

	    KernelPatching.escalatePrivileges();
	    KernelPatching.setSceAuthId(0x4801000000000013l);
	    KernelPatching.setSceCaps(0xffffffffffffffffl, 0xffffffffffffffffl);
	    KernelPatching.setSceAttr(KernelPatching.getSceAttr() | 0x80);
	    LoggingUI.getInstance().log("[+] Escalated privileges");

	    KernelPatching.setSecurityFlags(KernelPatching.getSecurityFlags() | 0x14);
	    KernelPatching.setUtokenFlags((byte)(KernelPatching.getUtokenFlags() | 0x1));
	    KernelPatching.setQAFlags(KernelPatching.getQAFlags() | 0x0000000000010300l);
	    KernelPatching.setTargetId((byte)0x82);
	    LoggingUI.getInstance().log("[+] Debug/dev mode enabled");

	    logUI.setVisible(false);
	    EventManager.getInstance().addUserEventListener(this, evtRepo);
	} catch (Throwable t) {
	    LoggingUI.getInstance().log(t);
	}
    }

    public void pauseXlet() {
	scene.setVisible(false);
    }

    public void destroyXlet(boolean unconditional) {
        scene = null;
    }

    public void userEventReceived(UserEvent evt) {
	if(evt.getType() != KeyEvent.KEY_RELEASED) {
	    return;
	}

	if(evt.getCode() == 461) {
	    logUI.setVisible(!logUI.isVisible());
	    return;
	}

	switch(evt.getCode()) {
	case HRcEvent.VK_ENTER:
	    Runnable r = (Runnable)listUI.getSelected();
	    r.run();
	    break;
	case HRcEvent.VK_UP:
	    listUI.itemUp();
	    break;
	case HRcEvent.VK_DOWN:
	    listUI.itemDown();
	    break;
	}
    }
}
