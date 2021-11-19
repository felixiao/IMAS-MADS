# IMAS-MADS
An IMAS project

Usage:

1. Run configuration
   1. In Run->Edit configuration-> + for add new configuration -> Select Maven
   2. In Parameters-> Command Line -> add following:
      1. exec:java -f pom.xml
   3. In Parameters-> Profiles -> add following: 
      1. Test
   4. Run Test Configuration, the GUI Agent system will run.


2. After running the GUI Agent system, to start the agents:
   1. Select user1 agent on the left side window
   2. Right-click the user1 agent
   3. Select Send Message
   4. In the popup window, Select request in the Communicative dropdown menu
   5. Type "Start" in Content box
   6. Click 'Ok' to send message