package main;
import chuckNorrisBot.ChuckNorrisBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class ChuckNorrisMain {
    public static void main(String[] args) {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new ChuckNorrisBot());
        }
        catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
