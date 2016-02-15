#!/bin/bash

#!/bin/bash


# Change this to your netid
netid=txt141130

#
# Root directory of your project
PROJDIR=$HOME/CS6378/Project2

#
# This assumes your config file is named "config.txt"
# and is located in your project directory
#
CONFIG=$PROJDIR/config.txt

#
# Directory your java classes are in
#
BINDIR=$PROJDIR/bin

#
# Your main project class
#
PROG=SystemClass


n=1

cat $CONFIG | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read i
    for ((y=1; y<=$i; y++))	
    do
    {
	read line 
        nodenumber[$n]=$( echo $n )
        count[$n]=$( echo $line | wc -w )
        host[$n]=$( echo $line | awk '{ print $1 }' )
        port[$n]=$( echo $line | awk '{ print $2 }' )

        n=$(( n + 1 ))
    }
    done

cat $CONFIG | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
        read i
        c=1
        for ((y=1; y<=$i; y++))	
    	do
   	{
		read line 

                for ((x=3; x<=${count[$c]}; x++))
                do
                {

                        neigh=$( echo $line | awk '{ print $"'$x'" }')
                        if  [ "$neigh" == "true" ]; then
                                DR[$c]="true"

                        else
                                neighbour[$c]=${neighbour[$c]}${host[$neigh]}.utdallas.edu-${port[$neigh]},
                        fi


                }
                done
                c=$(( c + 1 ))
	}

        done

	read requests
	read delay

	for ((y=1; y<=$i; y++))	
    	do
   	{
		read line
		node=$( echo $line | awk '{ print $1 }' )
 		app_port[$node]=$( echo $line | awk '{ print $2 }' )
		


	}
	done

        for ((c=1; c<=$i; c++))
        do
        {


        ssh $netid@${host[$c]} java -cp $BINDIR $PROG  ${nodenumber[$c]} ${host[$c]}.utdallas.edu ${port[$c]} ${app_port[$c]} ${neighbour[$c]} $requests $delay $i ${DR[$c]} &
        
        }
        done

))

                         


