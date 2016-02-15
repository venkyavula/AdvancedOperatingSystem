AOS Project2 Readme.txt

Team 

Thiriloshini txt141130
Nithya 	nxs143630
Venkatesh Avula vxa141230


Instructions

1.Compile all java files in /src folder and make changes to the script.sh/cleanup.sh files in /config by giving folder structure details where to deploy.
2.Change config.txt according to configuration. 
3.Once you run the script.sh, 
	In home directory following files will be created. 
	a. All log files with node_<<nodeNumber>>.txt and <<nodename>>_spanningTree.txt will be created.
	b. dest.txt ==> intermediate file which contains all intermediate spanning tree info 
	c. test.txt ==> output file which has test strategy 1 (basic CS Enter , CSEXit executions will be saved )
	d. failure.txt ==> incase if there is mutual exclusion failed this file will get created.

4. Run cleanup.sh which will clean all the opened connections on DC machine.

