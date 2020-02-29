package com.acts.gachibot;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class GachiBot extends Bot {

    private final static String PICTURES_DIRECTORY = "pics";
    private final ConcurrentMap<Long, BotState> map = new ConcurrentHashMap<>();

    public static void main(String[] args) {
//        if (args == null || args.length != 2) {
//            System.out.println("You must run bot with 2 args - BotToken and bot UserName");
//        } else {
//            ApiContextInitializer.init();
//            Bot.runBot(new GachiBot(args[0], args[1]));
//        }

        ApiContextInitializer.init();
        Bot.runBot(new GachiBot("1121619285:AAHF7b8rYO-ZP1rfWY-YaU3Kx0hldY_86H0", "GachiBot"));
    }

    protected GachiBot(String token, String botName) {
        super(token, botName);
    }

    protected void processTheException(Exception e) {
        e.printStackTrace();
        System.out.println(e.toString());
    }

    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            String text = update.getMessage().getText().trim();
            map.putIfAbsent(chatId, BotState.WORKING);
            if (text.equals("/start")) {
                map.put(chatId, BotState.WORKING);
                sendTextMessage(chatId, "Чого тобі, шкіряний чоловіче?");
            } else if (map.get(chatId) != BotState.STOPPED) {
                switch (text) {
                    case "/kill":
                        sendTextMessage(chatId, "Прощавай жорстокий світ!");
                        map.put(chatId, BotState.STOPPED);
                        break;
                    case "/talk":
                        List<String> answers = Arrays.asList(
                                "Ass we can!", "Fuck you!", "Fucking slaves!",
                                "Take it boy!", "Sucktion!", "Oh shit, i'm sorry",
                                "Sorry for what?", "Ah ah ah...", "That's turns me on",
                                "Deep dark fantasies...", "Dungeon master", "Do you like what you see?",
                                "Stick finger in my ass", "I smoke Marlboro, you smoke cocks");
                        String answer = answers.get((int) (answers.size() * Math.random()));
                        sendTextMessage(chatId, answer);
                        break;
                    case "/randompic":
                        String[] filePaths = getResourceFolderFilePaths(PICTURES_DIRECTORY);
                        String filePath = getClass().getClassLoader()
                                .getResource(PICTURES_DIRECTORY + "/" + filePaths[(int) (filePaths.length * Math.random())]).getPath();
                        File picture = new File(filePath);
                        sendPhotoMessage(chatId, null, picture);
                        break;
                    default:
                        sendTextMessage(chatId, "Не знаю, що ти пишеш, але думаю, що тебе звуть Юра/Стас/Андрій/Валентин");
                        break;
                }
            }
        }
    }

    private String[] getResourceFolderFilePaths(String folder) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(folder);
        return new File(url.getPath()).list();
    }
}
