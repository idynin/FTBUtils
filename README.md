# FTBUtils

A collection of command line utilities for Feed the Beast.

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
 -checkversion <modpack> <version>   Checks if the recommended version
                                     matches passed version
 -downloadserver <modpack>           Download a Modpack Server
 -getversion <modpack>               Get the recommended version of
                                     modpack
 -help                               Show this help
 -listmodpacks                       List all available modpacks
 -privatepack <packcode>             Perform the requested action in the
                                     packcode context
 -v                                  Verbose mode
```
 
### Examples
 
Download a modpack server 
```sh
java -jar ftbutils-0.0.1-SNAPSHOT.jar -downloadserver direwolf20
Downloading modpack Direwolf20 version 1.0.18 from Atlanta
Download Progress:      130 MB/130 MB   (6 MB/sec)                      
Downloading Direwolf20-1.0.18.zip complete!
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