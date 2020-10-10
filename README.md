# xf8bot   
[![Build Status](https://travis-ci.com/xf8b/xf8bot.svg?branch=production)](https://travis-ci.com/xf8b/xf8bot)  
A general purpose bot that helps you.
## Startup Guide
**A more detailed startup guide is available in [the documentation](https://xf8b.github.io/documentation/xf8bot/startup/).**  
You need:  
* A bot token (Get it from the [Discord Developer Portal](https://discord.com/developers/applications))
* Java 15 (I recommend [AdoptOpenJDK](https://adoptopenjdk.net))  
* A MongoDB Server (you can use the cloud server they provide for free or host your own)
### Using Docker
**You must have a GitHub account and a PAT for authenticating!**
See [the GitHub docs](https://docs.github.com/en/free-pro-team@latest/packages/managing-container-images-with-github-container-registry/pushing-and-pulling-docker-images#authenticating-to-github-container-registry) if you need help.
1. Install Docker if you haven't.
2. Authenticate to the GitHub Container Registry.
3. Run `docker pull ghcr.io/xf8b/xf8bot` to pull the image.
4. Run `docker run xf8bot:TAG ARGS`, with `TAG` being the version you would like to run (latest or stable) and `ARGS` being the program arguments that you pass in. For a full list of program arguments, see [the documentation](https://xf8b.github.io/documentation/xf8bot/args/).
5. Invite the bot to your server.  
6. You're done.  
### Self Compiling
1. Download the code, and run `gradlew shadowJar` (or `./gradlew shadowJar` for Mac and Linux).    
2. The jar file will be in `build/libs`.  
3. Run that using `java -jar xf8bot-x.x.x-all.jar`. (optionally you can add program arguments so you don't have to do step 4. see [the documentation](https://xf8b.github.io/documentation/xf8bot/args/))
4. It will fail because of the invalid token.  
You must go to `config.toml` and fill out the fields that are under `required`.  
5. Invite the bot to your server.  
6. You're done.  
## Docs
See [the documentation](https://xf8b.github.io/documentation/xf8bot/).
## License  
xf8bot is licensed under GPL v3. You can see details at [COPYING.md](COPYING.md).  
## Notices
The command handler for `>ping` is licensed under Apache 2.0. The file contains a notice.
