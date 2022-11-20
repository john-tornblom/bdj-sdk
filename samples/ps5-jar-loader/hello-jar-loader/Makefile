PS5_HOST ?= ps5
PS5_PORT ?= 9025


all: helloworld.jar


helloworld.jar: HelloWorld.class
	jar cfe $@ HelloWorld $<


%.class: %.java
	javac $<


clean:
	rm -f helloworld.jar *.class


test: helloworld.jar
	nc -q0 $(PS5_HOST) $(PS5_PORT) < $<
