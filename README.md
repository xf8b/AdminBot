# xf8bot   
[![Build Status](https://travis-ci.com/xf8b/xf8bot.svg?branch=production)](https://travis-ci.com/xf8b/xf8bot)  
A general purpose bot that helps you.
## Startup Guide
**A more detailed startup guide is available in [the documentation](https://xf8b.github.io/xf8bot/docs/startup/).**  
You need:  
* A bot token (Get it from the [Discord Developer Portal](https://discord.com/developers/applications))
* Java 15 (I recommend [AdoptOpenJDK](https://adoptopenjdk.net))  
* A MongoDB Server (you can use the cloud server they provide for free or host your own)
1. Download the code, and run `gradlew shadowJar` (or `./gradlew shadowJar` for Mac and Linux).    
2. The jar file will be in `build/libs`.  
3. Run that using `java -jar xf8bot-x.x.x-all.jar`.  
4. It will fail because of the invalid token.  
You must go to `config.toml` and fill out the fields that are under `required`.  
5. Invite the bot to your server.  
6. You're done.  
### Optional
* You can add `-t token` to the command run in step 3, and you don't have to do step 4.
## Docs
See [https://xf8b.github.io/xf8bot/docs/](https://xf8b.github.io/xf8bot/docs/).
## License  
xf8bot is licensed under GPL v3. You can see details at [COPYING.md](COPYING.md).  
## Notices
The command handler for `>ping` is licensed under Apache 2.0. The file contains a notice.
