package com.yube.bot;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GachiBot extends Bot {

    private final static String PICTURES_DIRECTORY = "pics";
    private final static String GIFS_DIRECTORY = "gifs";
    private final ConcurrentMap<Long, BotState> map = new ConcurrentHashMap<>();
    private List<String> answers;

    protected GachiBot(String token, String botName) throws Exception {
        super(token, botName);
        loadResources();
    }

    public static void main(String[] args) throws Exception {
        if (args == null || args.length != 2) {
            System.out.println("You must run bot with 2 args - BotToken and bot UserName");
        } else {
            ApiContextInitializer.init();
            Bot.runBot(new GachiBot(args[0], args[1]));
        }
    }

    protected void loadResources() throws FileNotFoundException {
        FileReader fileReader = new FileReader("src/main/resources/answers.txt");
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        answers = bufferedReader.lines().collect(Collectors.toList());
    }

    protected void processTheException(Exception e) {
        e.printStackTrace();
    }

    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage().hasText()) {
                Message message = update.getMessage();
                long chatId = message.getChatId();
                String text = update.getMessage().getText().trim();
                map.putIfAbsent(chatId, BotState.WORKING);
                if (text.equals("/start")) {
                    map.put(chatId, BotState.WORKING);
                    sendTextMessage(chatId, "What are you want, leatherman?");
                } else if (map.get(chatId) != BotState.STOPPED) {
                    switch (text) {
                        case "/kill":
                            sendTextMessage(chatId, "Goodbye cruel world!");
                            map.put(chatId, BotState.STOPPED);
                            break;
                        case "/talk":
                            String answer = getRandomAnswer();
                            sendTextMessage(chatId, answer);
                            break;
                        case "/randompic":
                            File picture = getRandomFileFromDirectory(PICTURES_DIRECTORY);
                            sendPhotoMessage(chatId, null, picture);
                            break;
                        case "/randomgif":
                            File gif = getRandomFileFromDirectory(GIFS_DIRECTORY);
                            sendAnimationMessage(chatId, null, gif);
                            break;
                        default:
                            sendTextMessage(chatId, "I can't recognize what are you saying.\n" +
                                    "Try to spit off fat cock from your mouth " +
                                    "and repeat with something more understandable (see 'command list')");
                            break;
                    }
                }
            }
        } catch (Exception e) {
            processTheException(e);
        }
    }

    private String getRandomAnswer() {
        return answers.get((int) (answers.size() * Math.random()));
    }

    private File getRandomFileFromDirectory(String directory) {
        String[] filePaths = getResourceFolderFilePaths(directory);
        String filePath = getClass().getClassLoader()
                .getResource(directory + "/" + filePaths[(int) (filePaths.length * Math.random())]).getPath();
        return new File(filePath);
    }

    private String[] getResourceFolderFilePaths(String folder) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(folder);
        return new File(url.getPath()).list();
    }
}
