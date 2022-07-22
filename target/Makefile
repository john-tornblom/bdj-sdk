MAKEFILE_DIR := $(dir $(realpath $(lastword $(MAKEFILE_LIST))))
BDJSDK_HOME  ?= $(MAKEFILE_DIR)/../
JAVA8_HOME    ?= $(BDJSDK_HOME)/host/jdk8
JAVAC        := $(JAVA8_HOME)/bin/javac
JAR          := $(JAVA8_HOME)/bin/jar

SOURCES       := $(shell find $(MAKEFILE_DIR)/src -name *.java -type f)
JFLAGS        := -Xlint:-options -source 1.3 -target 1.3


lib/sony-stubs.jar: $(SOURCES)
	$(JAVAC) $(JFLAGS) $(SOURCES)
	$(JAR) cf $@ -C src/ .


clean:
	rm lib/sony-stubs.jar

