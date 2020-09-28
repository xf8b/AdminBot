---
layout: default
title: xf8bot Commands
permalink: /docs/commands/
---
# xf8bot Commands
To see the documentation for the commands below, click on the command name.
## Info
* `help`: Shows the commands available and gives information on them.  
* [`information`](https://xf8b.github.io/xf8bot/docs/commands/info/info/): Shows some information about xf8bot.  
* `memberinfo`: Shows information about the member.    
* [`ping`](https://xf8b.github.io/xf8bot/docs/commands/info/ping/): Shows the ping. Pretty useless.  
## Music
* `join`: Joins your current VC.  
* `leave`: Leaves the current VC.
* `play`: Plays audio in the current VC.
* `skip`: Skips the current audio playing.
* `pause`: Pauses the current audio playing.
* `volume`: Changes the volume of the audio in the current VC.  
## Administration (Admin Only)  
### Level 1
* [`nickname`](https://xf8b.github.io/xf8bot/docs/commands/administration/level_1/nickname/): Sets or resets the specified member's nickname.  
* `mute`: Mutes the specified member for the amount of time specified.  
* `warn`: Warns the specified member for the specified reason.  
* `removewarn`: Removes warns with the specified warn id and reason for the specified member.  
* `warns`: Gets the warns for the specified member.  
### Level 2
* `kick`: Kicks the specified member for the specified reason.  
* [`clear`](https://xf8b.github.io/xf8bot/docs/commands/administration/level_2/clear/): Clears the specified amount of messages from the current text channel.  
### Level 3
* `ban`: Bans the specified member for the specified reason.  
* `unban`: Unbans the specified member.  
### Level 4
* [`prefix`](https://xf8b.github.io/xf8bot/docs/commands/other/prefix/): Sets the prefix for xf8bot. (NOTE: This is actually not in the `Administration` section, it is in the `Other` section)  
* [`administrators`](https://xf8b.github.io/xf8bot/docs/commands/administration/level_4/administrators/): Adds to, removes from, or gets all the roles that can do administrator only commands.  
## Bot Administrator Only  
`eval`: Evaluates code. Bot administrators only!  
`shutdown`: Shuts down the bot. Bot administrators only!  
`say`: Sends the passed in content to the current channel.  
## Other
* `slap`: Slaps the person.
