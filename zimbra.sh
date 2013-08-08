#!/bin/bash

case "$1" in
	"-describe" )
		echo "SC_DESCRIBE#OP#quiesce"
		echo "SC_DESCRIBE#OP#unquiesce"
		echo "SC_DESCRIBE#DESCRIPTION#Plug-in to handle nothing."
		echo "SC_DESCRIBE#PARAMETER#SCRIPT_PARAM1#a useless parameter#SCRIPT_PARAM1=abc123#Y"
        echo "SC_DESCRIBE#PARAMETER#SCRIPT_PARAM2#another useless parameter#SCRIPT_PARAM2=abc123#N"
        echo "SC_DESCRIBE#PARAMETER#SCRIPT_PARAM3#another useless and optional parameter#SCRIPT_PARAM2=abc123"
		echo "describe completed successfully"
		
		exit 0;
		;;
	"-quiesce" ) 
		echo "SC_MSG#INFO#23.02.2010#Quiescing database"
		echo "SC_MSG#INFO#23.02.2010#Executing command dbmcli"
		echo "SC_MSG#DEBUG#23.02.2010#Command dbmcli returned"
		echo "version abc.123.xyz"
		echo "dbstate online"
		echo "DATA_VOLUME_0001=abc123"
		echo "SC_MSG#INFO#23.02.2010#Quiescing database finished successfully"
		echo "SC_PRESERVE#DB007#VOL_01#/sapdb/DB007/sapdata1"
		echo "SC_PRESERVE#DB007#VOL_02#/sapdb/DB007/sapdata2"
		echo "SC_PRESERVE#DB007#VOL_03#/sapdb/DB007/sapdata3"
		;;
	"-unquiesce" )
		echo "SC_MSG#INFO#23.02.2010#Unquiescing"
		echo "SC_MSG#DEBUG#23.02.2010#Command dbmcli returned"
		echo "invalid parameter"
        echo "SC_MSG#ERROR#23.02.2010#Unquiescing database failed"
		exit 1
		;;
	"-scdump" )
		echo "SC_MSG#INFO#01.03.2011#Requesting db version for db SID1"
		echo "SC_DUMP#DB#SID1#10.2.0.4"
		echo "SC_MSG#INFO#01.03.2011#Requesting db version for db SID2"
		echo "SC_DUMP#DB#SID2#11.2.0.8"
		echo "SC_MSG#INFO#01.03.2011#Requesting db version for db SID3"
		echo "SC_DUMP#DB#SID3#8.1.0.1"
		echo "SC_MSG#INFO#01.03.2011#Requesting os version"
		uname_a=$(uname -a)
		echo "SC_DUMP#OS#$uname_a"
		echo "SC_MSG#INFO#01.03.2011#Requesting snapdrive version"
		sd=$(snapdrive version | grep -v Daemon)
		echo "SC_DUMP#snapdrive#$sd"
		;;
	"-discover" )
		echo "SC_DISCOVER#DATA#DISCOVERED#sapfiler2-gig#scriptdb_sapdata1_SCRIPT"
		echo "SC_DISCOVER#DATA#DISCOVERED#sapfiler2-gig#scriptdb_sapdata2_SCRIPT"
		echo "SC_DISCOVER#DATA#DISCOVERED#sapfiler2-gig#scriptdb_sapdata3_SCRIPT"
		echo "SC_DISCOVER#DATA#RESULT#0"
		echo "SC_DISCOVER#ONLINE_LOG#DISCOVERED#sapfiler2-gig#scriptdb_saplog_SCRIPT"
		echo "SC_DISCOVER#ONLINE_LOG#FAILED#/this/is/my/filesystem"
		echo "SC_DISCOVER#ONLINE_LOG#RESULT#1"
		echo "SC_DISCOVER#ENV#SCRIPT_DEITMAR_1#hello world"
		echo "SC_DISCOVER#ENV#SCRIPT_DEITMAR_2#blah blah"
		echo "SC_DISCOVER#ENV#SCRIPT_DEITMAR_3#foo, bar"
		;;
	* )
		echo "not implemented";
		exit 1
		;;
esac

exit 0;
