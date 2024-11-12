# SoundByte

## What it does:
When this bot is present inside a Discord server and someone joins a voice channel, it joins the voice channel with them and plays a sound file (think a ringtone). This was a dumb but fun idea I had a few years ago making a private bot in a server with friends. Now, this bot is intended to be public for people to use and customize for their servers.

If you want to invite it to your server you can do so here: https://discord.com/oauth2/authorize?client_id=871455134260011048&permissions=0&scope=bot

## How it works: AWS
The public bot is hosted on an AWS EC2 instance, but it can run on a local machine just fine. However, it is still necessary to create an AWS account, as well as set up the proper IAM settings so that the program to use DynamoDB and S3 for information and file storage. Upon executing the program in a terminal, it should set up almost everything automatically, and anything it can't do on its own will prompt you to take action. This will include manually inputting information into previously created DynamoDB tables, putting your Discord API token in SecretManager under the name "DiscordRingtones", and other little things. 

## How to run:
Download the latest released jar file. 
Open a terminal instance, and run "java -jar SoundByte.jar".

Once again, it is necessary to have an AWS account set up and IAM permissions already configured to run this program.
