# GeoPal Telegram Bot

Welcome to GeoPal, your personal location-sharing assistant on Telegram! With GeoPal, you can easily add friends to your list of contacts, so that you can share your current location with them in real-time. You can also remove friends from your contact list and stop sharing your location with them at any time. 

GeoPal is built on the Telegram Bot API and uses the Google Maps API to fetch location data.

## Getting Started

To get started with GeoPal, simply start a chat with the bot by searching for [@location_sharing_bot](https://t.me/location_sharing_bot) in the Telegram app. Once you've started a chat with the bot, you can use the following commands:

- /start - Register with GeoPal and get started.
- /add_friend - Add a friend to your contact list.
- /remove_friend - Remove a friend from your contact list.
- /friend_list - View your list of friends.
- /share_location - Share your current location with your friends and let them know where you're at in just one click.
- /help - Get help with using GeoPal.

## Overview
![Demo](resources/demo.gif) \

![Scroll over the friend list](resources/friend_list.gif) \

![Abort every action](resources/abort.gif) \

## How to run

To run the GeoPal Telegram Bot, you will need to set up a Telegram Bot account and obtain a Google Maps API key. Follow these steps to set up the bot:
* Telgram Bot Token - you can get that by the [BotFather](https://telegram.me/BotFather)
* Google Maps Service API (Geocoding API is enough) - [Google Maps Services API](https://developers.google.com/maps/documentation/geocoding)

Then, you would need to place those tokens into the [config.properties](src/main/resources/config.properties).

Once you've done that, you can simply start the [Application](src/main/java/application/Application.java) and use the GeoPalBot.
