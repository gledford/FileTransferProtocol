JFLAGS = -g
JCC = javac

default: Client.class Server.class 

Client.class: com/client/Client.java
	$(JCC) $(JFLAGS) com/client/Client.java

Server.class: com/server/Server.java
	$(JCC) $(JFLAGS) com/server/Server.java

clean: 
	$(RM) *.class received.txt