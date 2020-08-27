# AdminBot  
[![Build Status](https://travis-ci.com/xf8b/AdminBot.svg?branch=master)](https://travis-ci.com/xf8b/AdminBot)  
A simple bot used for administration.  
## Commands  
### Non Administrator Only  
`help`: Shows the commands available and gives information on them.  
`info`: Shows some information about AdminBot.  
`memberinfo`: Shows information about the member.  
`prefix`: Sets the prefix for AdminBot.  
`ping`: Shows the ping. Pretty useless.  
### Administrator Only  
`nickname`: Sets or resets the specified member's nickname.  
`clear`: Clears the specified amount of messages from the current text channel.  
`mute`: Mutes the specified member for the amount of time specified.  
`warn`: Warns the specified member for the specified reason.  
`removewarn`: Removes warns with the specified warn id and reason for the specified member.  
`warns`: Gets the warns for the specified member.  
`kick`: Kicks the specified member for the specified reason.  
`ban`: Bans the specified member for the specified reason.  
`unban`: Unbans the specified member.  
`administrators`: Adds to, removes from, or gets all the roles that can do administrator only commands.  
### Bot Administrator Only  
`eval`: Evaluates code. Bot administrators only!  
`shutdown`: Shuts down the bot. Bot administrators only!  
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
## License  
AdminBot is licensed under GPL v3. You can see details at [COPYING.md](COPYING.md).  
## Notices
The command handler for `>ping` is licensed under Apache 2.0. The file contains a notice.
