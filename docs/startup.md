---
layout: default
title: Startup Guide
permalink: /docs/startup/
---

# Startup Guide
You need:  
* A bot token (See [How To Get A Token](#how-to-get-a-token))
* Java 15 (I recommend [AdoptOpenJDK](https://adoptopenjdk.net))  
* A MongoDB Server (You can use the [cloud server they provide for free](https://www.mongodb.com/cloud/atlas/signup) or host your own)
1. Download the code, and run `gradlew shadowJar` (or `./gradlew shadowJar` for Mac and Linux).    
2. The jar file will be in `build/libs`.  
3. Run that using `java -jar xf8bot-x.x.x-all.jar`.  
4. It will fail because of the invalid token.  
You must go to `config.toml` and fill out the fields that are under `required`.  
5. Invite the bot to your server (See [How To Get The Invite](#how-to-get-the-invite)).  
6. You're done.  
## Optional
* You can add `-t token` to the command run in step 3, and you don't have to do step 4.  
## Config
You must setup (if you use the config file):
* The MongoDB connection URL (See [The MongoDB Documentation](https://docs.mongodb.com/manual/reference/connection-string/))
* The MongoDB database name 
* The token (See [How To Get A Token](#how-to-get-a-token))
* The bot administrators (Copy paste your user ID into the brackets (See [Discord Support](https://support.discord.com/hc/en-us/articles/206346498-Where-can-I-find-my-User-Server-Message-ID-)))
* The sharding strategy (I recommend to keep it at `RECOMMENDED`)
You can remove the log dump webhook field by putting a # before it.
## How To Get A Token
1. Go to the [Discord Developer Portal](https://discord.com/developers/applications).
2. Login with your Discord account if you need to.
3. Click **New Application**.
4. Type the name that you want the bot to be named.
5. Click **Bot** on the side, and then click **Add Bot**. Press **Confirm**.
6. You now have the token. Do not share this token, as it is basically the key to the bot. If you ever accidentally leak it, press the **Regenerate** button.
## How To Get The Invite
1. Go to the [Discord Developer Portal](https://discord.com/developers/applications).
2. Login with your Discord account if you need to.
3. Click the application that you created in the [How To Get A Token](#how-to-get-a-token) section.
4. Copy the **Client ID**.
5. The template for the invite link is `https://discordapp.com/oauth2/authorize?client_id=CLIENT_ID&permissions=8&scope=bot`. Replace `CLIENT_ID` with the client id.
6. You now have the invite. Copy paste the link into your browser and add it to your server.
