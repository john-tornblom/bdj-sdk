PS5_HOST ?= ps5
PS5_PORT ?= 9025


all: props.jar


props.jar: PrintSystemProperties.class
	jar cfe $@ PrintSystemProperties $<


%.class: %.java
	javac $<


clean:
	rm -f *.jar *.class


test_props: props.jar
	nc -q0 $(PS5_HOST) $(PS5_PORT) < $<
