package application;

import bot.GeoPalBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.IOException;

/**
 * Entry point for bot application
 * */
public class Application {
    public static void main(String[] args) throws TelegramApiException {
        Logger logger = LoggerFactory.getLogger(Application.class);
        try {
            GeoPalBot bot = new GeoPalBot();
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);
        } catch (Exception e) {
            logger.error("Application start failed: {}", e.getMessage());
        }
    }
}
