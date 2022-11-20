#   Copyright (C) 2022 John Törnblom
#
# This program is free software; you can redistribute it and/or modify it
# under the terms of the GNU General Public License as published by
# the Free Software Foundation; either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful, but
# WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program; see the file COPYING. If not see
# <http://www.gnu.org/licenses/>.


DISC_LABEL := ps5-jar-loader

#
# Host tools
#
MAKEFILE_DIR := $(dir $(realpath $(lastword $(MAKEFILE_LIST))))
BDJSDK_HOME  ?= $(MAKEFILE_DIR)/../../
BDSIGNER     := $(BDJSDK_HOME)/host/bin/bdsigner
MAKEFS       := $(BDJSDK_HOME)/host/bin/makefs
JAVA8_HOME   ?= $(BDJSDK_HOME)/host/jdk8
JAVA11_HOME  ?= $(BDJSDK_HOME)/host/jdk11
JAVAC        := $(JAVA11_HOME)/bin/javac
JAR          := $(JAVA11_HOME)/bin/jar

export JAVA8_HOME
export JAVA11_HOME

#
# Compilation artifacts
#
CLASSPATH := $(BDJSDK_HOME)/target/lib/enhanced-stubs.zip:$(BDJSDK_HOME)/target/lib/sony-stubs.jar
SOURCES   := $(wildcard src/org/homebrew/*.java)
JFLAGS    := -Xlint:-options

#
# Disc files
#
TMPL_DIRS  := $(shell find $(BDJSDK_HOME)/resources/AVCHD/ -type d)
TMPL_FILES := $(shell find $(BDJSDK_HOME)/resources/AVCHD/ -type f)

DISC_DIRS  := $(patsubst $(BDJSDK_HOME)/resources/AVCHD%,discdir%,$(TMPL_DIRS)) \
              discdir/BDMV/JAR
DISC_FILES := $(patsubst $(BDJSDK_HOME)/resources/AVCHD%,discdir%,$(TMPL_FILES)) \
              discdir/BDMV/JAR/00000.jar


all: $(DISC_LABEL).iso


discdir:
	mkdir -p $(DISC_DIRS)

discdir/BDMV/JAR/00000.jar: discdir $(SOURCES)
	$(JAVAC) $(JFLAGS) -cp $(CLASSPATH) $(SOURCES)
	$(JAR) cf $@ -C src/ .
	$(BDSIGNER) -keystore $(BDJSDK_HOME)/resources/sig.ks $@

discdir/%: discdir
	cp $(BDJSDK_HOME)/resources/AVCHD/$* $@


$(DISC_LABEL).iso: $(DISC_FILES)
	$(MAKEFS) -m 16m -t udf -o T=bdre,v=2.50,L=$(DISC_LABEL) $@ discdir


clean:
	rm -rf META-INF $(DISC_LABEL).iso discdir src/org/homebrew/*.class

