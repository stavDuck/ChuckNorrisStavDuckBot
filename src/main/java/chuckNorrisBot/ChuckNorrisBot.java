package chuckNorrisBot;

import com.azure.ai.translation.text.TextTranslationClient;
import com.azure.ai.translation.text.TextTranslationClientBuilder;
import com.azure.ai.translation.text.models.*;
import com.azure.core.credential.AzureKeyCredential;
import com.vdurmont.emoji.EmojiParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.net.URL;
import java.util.*;

public class ChuckNorrisBot extends TelegramLongPollingBot {
    private final String TELEGRAM_BOT_USERNAME = "ChuckNorris_StavDuck_bot";
    private final String TELEGRAM_BOT_TOKEN = "6906170960:AAFrsVYNy_iPbWaFu3MNxt_gHhuDYcLnNAI";
    private final String AZURE_TRANSLATOR_KEY = "adc55fbff1774d16af791f7f258eedf7";
    private final String AZURE_TRANSLATOR_ENDPOINT = "https://chuck-norris-bot.cognitiveservices.azure.com/";
    private final String NO_PROBLEM = "\uD83D\uDC4D No problem";
    private final String ERROR_LANGUAGE_NOT_FOUND = "\uD83D\uDD0E Sorry, language is not found, please try again";
    private final String ERROR_NUMBER_NOT_VALID = "❗ Number is not in between 1 to 100, please try again";
    private final String ERROR_INPUT_IS_NOT_NUMBER = "\uD83D\uDEAB Sorry, the information is not a Number, please enter a number between 1 to 100";
    private final String ERROR_NO_LANGUAGE_SELECTED = "⛔ Sorry, language is not selected, please set language first";
    private final String url = "http://www.hellaentertainment.com/blog/celebrities/80-chuck-norris-jokes-for-his-80th-birthday/";

    private String languageCode = null;
    private List<String> jokesList;

    // ctor
    public ChuckNorrisBot() {
        getJokesFromWebsite();
    }

    private void getJokesFromWebsite() {
         /* Constructing String Builder to
        append the string into the html */
        StringBuilder html = new StringBuilder();
        String val = null;
        Document document = null;
        // Try block to check exceptions
        try {
            document = Jsoup.connect(url).get();
            //System.out.println(document.body().text());
        }
        // Catch block to handle exceptions
        catch (Exception ex) {
            // No file found
            System.out.println(ex.getMessage());
        }

        // get the father of the list of jokes from the HTML file
        Element divElement = document.select("div.entry-content").first();
        Element olElement = divElement.select("ol").first();
        //  store the <li> elements in the local jokesList data member
        jokesList = new ArrayList<>();

        // Iterate through the <li> elements and add them to the ArrayList
        if (olElement != null) {
            Elements liItems = olElement.select("li");
            for (Element li : liItems) {
                jokesList.add(li.text());
            }
        }
    }

    @Override
    public String getBotUsername() {
        return TELEGRAM_BOT_USERNAME;
    }
    @Override
    public String getBotToken() {
        return TELEGRAM_BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        // We check if the update has a message and the message has text
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            String text = message.getText().toLowerCase(); //set the input not case sensitive

            //// Chat starts
            if(text.equals("/start")){
                // set default answers for user
                setLanguageReply(text, message);
            }

            //// user request help
            else if(text.equals("/help")){
                // invoke help function
                String responstText =":sparkles: Welcome to my new Chuck Norris Bot \uD83E\uDE84 \n\n";
                responstText += ":battery: To start use /start \n";
                responstText += ":alien: To set up the language write \"set language\" and add the language you want. \n NOTE: English is by default \n\n";
                responstText += "✅ Then after choosing language you can type any number in between 1 - 100 to get one of many Chuck Norris jokes, \n \n";
                responstText += "Enjoy ! :sparkling_heart::collision:\uD83D\uDC4C";
                String res = EmojiParser.parseToUnicode(responstText);
                sendMessage(createSendMessage(message.getChatId(), res));
            }

            //// User set language
            else if(text.startsWith("set language")){
                String language = text.substring(13);
                System.out.println(language);
                String translatedText = null;

                // check if language exist in map in Azure Translator API
                if(isLanguageExist(language)){
                    translatedText = translateText(NO_PROBLEM);
                } else {
                    translatedText = translateText(ERROR_LANGUAGE_NOT_FOUND);
                }
                sendMessage(createSendMessage(message.getChatId(), translatedText));
            }

            // for any other case - expected to get number from user
            else  {
                String translatedText = null;
                try {
                    int jokeIndex = Integer.parseInt(text.trim());
                    if (jokeIndex >= 1 && jokeIndex <= 100) {
                        String joke = getChuckNorrisJoke(jokeIndex);
                        translatedText = translateText(joke);
                    }
                    else{
                        translatedText = translateText(ERROR_NUMBER_NOT_VALID);
                    }
                }
                // if catch the exception - the value is not a number
                catch (NumberFormatException e){
                    translatedText = translateText(ERROR_INPUT_IS_NOT_NUMBER);
                }
                // sending the message to the user
                sendMessage(createSendMessage(message.getChatId(), translatedText));
            }
        }
    }

    // Function check if input language from user exist, if true - save the language code
    private boolean isLanguageExist(String language){
        String tempLanguageCode = null;
        TextTranslationClient client = new TextTranslationClientBuilder()
                .endpoint(AZURE_TRANSLATOR_ENDPOINT)
                .credential(new AzureKeyCredential(AZURE_TRANSLATOR_KEY))
                .buildClient();

        GetLanguagesResult languages = client.getLanguages();

        System.out.println("Translation Languages:");
        for (Map.Entry<String, TranslationLanguage> translationLanguage : languages.getTranslation().entrySet()) {
            // System.out.println(translationLanguage.getValue().getName());
            if (translationLanguage.getValue().getName().toLowerCase()
                    .equals(language)) {
                tempLanguageCode = translationLanguage.getKey();
                System.out.println("language: " + language + " code: " + languageCode);
            }
        }
        if(tempLanguageCode != null){
            languageCode = tempLanguageCode;
            return true;
        }
        return false;
    }
    private String getChuckNorrisJoke(int index) {
        // all jokes were loaded at the start of the application
        String joke = jokesList.get(index-1);
        System.out.println(joke);
        return joke;
    }

    private String translateText(String textToTranslate) {
        // Requirement - check if language is set
        if(languageCode != null) {
            TextTranslationClient client = new TextTranslationClientBuilder()
                    .endpoint(AZURE_TRANSLATOR_ENDPOINT)
                    .credential(new AzureKeyCredential(AZURE_TRANSLATOR_KEY))
                    .buildClient();

            String from = "en";

            // BEGIN: getTextTranslationMultiple
            List<String> targetLanguages = new ArrayList<>();
            targetLanguages.add(languageCode);
            List<InputTextItem> content = new ArrayList<>();
            content.add(new InputTextItem(textToTranslate));

            List<TranslatedTextItem> translations = client.translate(targetLanguages, content, null, from, TextType.PLAIN, null, ProfanityAction.NO_ACTION, ProfanityMarker.ASTERISK, false, false, null, null, null, false);
            Translation textTranslation = translations.get(0).getTranslations().get(0);
            System.out.println("translated to : " + textTranslation.getText());
            return textTranslation.getText();
        }
        else {
            return ERROR_NO_LANGUAGE_SELECTED;
        }
    }

    // Function create SendMessage object
    private SendMessage createSendMessage(Long chatId, String text){
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        return message;
    }

    // Function send the message
    private void sendMessage(SendMessage message) {
        try {
            execute(message);
        }
        //catch (TelegramApiException e) {
        catch (Exception e){
            e.printStackTrace();
        }
    }

    // Function generate answer to user after Start
    // Function creates defult response answers for user
    private void setLanguageReply(String text, Message message) {
        String textForSendMessage = "Please select the language to show the joke. \n use format \"set language\" and write the language.\n\n After chosing the language you are free to ask for any of Chuck Norris jokes.";
        SendMessage sendMessage = createSendMessage(message.getChatId(), textForSendMessage);
        // keyboardButton
        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        replyKeyboardMarkup.setSelective(true);

        // create list of buttons
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        // english
        KeyboardRow keyboardRowEnglish = new KeyboardRow();
        KeyboardButton keyboardButtonEnglish = new KeyboardButton("set language english");
        keyboardRowEnglish.add(keyboardButtonEnglish);
        // hebrew
        KeyboardRow keyboardRowHebrew = new KeyboardRow();
        KeyboardButton keyboardButtonHebrew = new KeyboardButton("set language hebrew");
        keyboardRowHebrew.add(keyboardButtonHebrew);
        // russia
        KeyboardRow keyboardRowRussian = new KeyboardRow();
        KeyboardButton keyboardButtonRussian = new KeyboardButton("set language russian");
        keyboardRowRussian.add(keyboardButtonRussian);

        // add all
        keyboardRows.addAll(Arrays.asList(keyboardRowEnglish, keyboardRowHebrew, keyboardRowRussian));
        replyKeyboardMarkup.setKeyboard(keyboardRows);
        sendMessage.setReplyMarkup(replyKeyboardMarkup);
        sendMessage(sendMessage);
    }
}
