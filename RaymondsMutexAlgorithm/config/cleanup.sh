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

#cd $HOME
#rm -rf *.txt
#echo 'deleted text files'
n=1

cat $CONFIG | sed -e "s/#.*//" | sed -e "/^\s*$/d" |
(
    read i
    echo $i
    for ((y=1; y<=$i; y++))
    do
    {
    	read line
        host=$( echo $line | awk '{ print $1 }' )

        echo $host
        ssh $netid@$host killall -u $netid &
        sleep 1

        n=$(( n + 1 ))
    }
    done
   
)


echo "Cleanup complete"
