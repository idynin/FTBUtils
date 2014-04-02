# FTBUtils

A collection of command line utilities for Feed the Beast.
[![Build Status](https://travis-ci.org/idynin/FTBUtils.png?branch=master)](https://travis-ci.org/idynin/FTBUtils)

### Usage

```
java -jar ftbutils-0.0.1-SNAPSHOT.jar -help

88888888888 888888888888 88888888ba  88        88         88 88            
88               88      88      "8b 88        88   ,d    "" 88            
88               88      88      ,8P 88        88   88       88            
88aaaaa          88      88aaaaaa8P' 88        88 MM88MMM 88 88 ,adPPYba,  
88"""""          88      88""""""8b, 88        88   88    88 88 I8[    ""  
88               88      88      `8b 88        88   88    88 88  `"Y8ba,   
88               88      88      a8P Y8a.    .a8P   88,   88 88 aa    ]8I  
88               88      88888888P"   `"Y8888Y"'    "Y888 88 88 `"YbbdP"'  

FTBUtils
Copyright © 2014 Ilya Dynin

usage: java -jar ftbutils.jar [options]
 -checkversion <modpack> <version>      Checks if the recommended version
                                        matches passed version.
 -downloadmodpack <modpack> <version>   Download a Modpack.
                                        Version optional.
                                        Fetches recommended version if
                                        omitted.
 -downloadserver <modpack> <version>    Download a Modpack Server.
                                        Version optional.
                                        Fetches recommended version if
                                        omitted.
 -getversion <modpack>                  Get the recommended version of
                                        modpack.
 -help                                  Show this help.
 -listmodpacks                          List all available modpacks.
 -privatepack <packcode>                Perform the requested action in
                                        the packcode context.
 -status                                Print the status of all
                                        CreeperHost servers.
 -v                                     Verbose mode.

```
 
### Examples
 
Download a modpack server 
```sh
java -jar target/ftbutils-0.0.1-SNAPSHOT.jar -downloadserver 'FTB Lite'
Downloading modpack server FTB Lite version 1.2.3 from Atlanta
Download Progress:       21 MB / 21 MB  (5 MB / sec)                  
Downloading FTB Lite-server-1.2.3.zip complete!
```
Download a modpack for a particular version
```sh
java -jar target/ftbutils-0.0.1-SNAPSHOT.jar -downloadmodpack direwolf20 1.0.15
Downloading modpack Direwolf20 version 1.0.15 from Atlanta
Download Progress:      126 MB / 126 MB (8 MB / sec)                  
Downloading Direwolf20-1.0.15.zip complete!
```
Check the recommended modpack version 
 ```sh
java -jar ftbutils-0.0.1-SNAPSHOT.jar -getversion 'FTB Unleashed'
FTB Unleashed recommended version: 1.1.7
```
Compare a version number to the recommended version (returns error code -1 to indicate no match)
 ```sh
java -jar ftbutils-0.0.1-SNAPSHOT.jar -checkversion monster 1.0.5
Versions Do Not Match! New recommended version: 1.1.0

java -jar ftbutils-0.0.1-SNAPSHOT.jar -checkversion monster 1.1.0
Versions Match!
```
List all modpacks for a private packcode
```sh
java -jar target/ftbutils-0.0.1-SNAPSHOT.jar -privatepack sleepless -listmodpacks
Pack Name                          	Author              	MC Version	Pack Version
Sleepless Horrors                  	Sironin             	     1.6.4	1.7  
```
Print the status of CreeperHost server
```sh
java -jar target/ftbutils-0.0.1-SNAPSHOT.jar -status
Server                   	Address                            	    Latency
Atlanta                  	atlanta1.creeperrepo.net           	         32
Atlanta-2                	atlanta2.creeperrepo.net           	         42
Chicago                  	chicago2.creeperrepo.net           	Unreachable
Grantham                 	england3.creeperrepo.net           	         92
Los Angeles              	losangeles1.creeperrepo.net        	         82
Maidenhead               	england1.creeperrepo.net           	         94
Miami                    	miami1.creeperrepo.net             	         94
Nottingham               	england2.creeperrepo.net           	         89
```
