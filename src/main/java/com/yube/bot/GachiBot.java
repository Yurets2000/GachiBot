package com.yube.bot;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class GachiBot extends Bot {

    private final static String PICTURES_DIRECTORY = "pics";
    private final static String GIFS_DIRECTORY = "gifs";
    private final static String BOT_SHORTCUT = "@deep_dark_bot";
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
//        ApiContextInitializer.init();
//        Bot.runBot(new GachiBot("1121619285:AAHF7b8rYO-ZP1rfWY-YaU3Kx0hldY_86H0", "GachiBot"));
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
                boolean isGroupChat = message.getChat().isGroupChat() || message.getChat().isSuperGroupChat();
                String text = update.getMessage().getText().trim();
                map.putIfAbsent(chatId, BotState.WORKING);
                if (checkTextIsCommand(text, "start", isGroupChat)) {
                    map.put(chatId, BotState.WORKING);
                    sendTextMessage(chatId, "What are you want, leatherman?");
                } else if (map.get(chatId) != BotState.STOPPED) {
                    if (checkTextIsCommand(text, "stop", isGroupChat)) {
                        sendTextMessage(chatId, "Goodbye cruel world!");
                        map.put(chatId, BotState.STOPPED);
                    } else if (checkTextIsCommand(text, "talk", isGroupChat)) {
                        String answer = getRandomAnswer();
                        sendTextMessage(chatId, answer);
                    } else if (checkTextIsCommand(text, "randompic", isGroupChat)) {
                        File picture = getRandomFileFromDirectory(PICTURES_DIRECTORY);
                        sendPhotoMessage(chatId, null, picture);
                    } else if (checkTextIsCommand(text, "randomgif", isGroupChat)) {
                        File gif = getRandomFileFromDirectory(GIFS_DIRECTORY);
                        sendAnimationMessage(chatId, null, gif);
                    }
                }
            }
        } catch (Exception e) {
            processTheException(e);
        }
    }

    private boolean checkTextIsCommand(String text, String command, boolean isGroupChat) {
        command = "/" + command;
        return isGroupChat ? (command + BOT_SHORTCUT).equals(text) : command.equals(text);
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
