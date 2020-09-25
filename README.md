# AdminBot  
[![Build Status](https://travis-ci.com/xf8b/AdminBot.svg?branch=master)](https://travis-ci.com/xf8b/AdminBot)  
A simple bot used for administration.  
## Startup Guide
You need:  
* A bot token  
1. Download the code, and run `gradlew shadowJar`.    
2. The jar file will be in `build/libs`.  
3. Run that using `java -jar theJar.jar`.  
4. It will fail because of the invalid token.   
You must go to `secrets/config.toml` and fill out the token.  
5. Invite the bot to your server.  
6. You're done.  
## Docs
See [https://xf8b.github.io/AdminBot/docs/](https://xf8b.github.io/AdminBot/docs/).
## License  
AdminBot is licensed under GPL v3. You can see details at [COPYING.md](COPYING.md).  
## Notices
The command handler for `>ping` is licensed under Apache 2.0. The file contains a notice.
