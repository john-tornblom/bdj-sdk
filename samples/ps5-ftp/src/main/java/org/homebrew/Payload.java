package org.homebrew;

import java.security.AccessController;
import java.security.PrivilegedAction;

@SuppressWarnings({"deprecation","rawtypes", "removal", "unchecked"})
public class Payload {
	public Payload() {
		AccessController.doPrivileged(new PrivilegedAction() {
				public Object run() {
					System.setSecurityManager(null);
					return null;
				}
			});
	}
}
