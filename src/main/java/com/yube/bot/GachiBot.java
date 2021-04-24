package com.yube.bot;

import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.*;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class GachiBot extends Bot {

    private final static String PICTURES_DIRECTORY = "pics";
    private final static String GIFS_DIRECTORY = "gifs";
    private final static String VOICES_DIRECTORY = "sound";
    private final static String BOT_SHORTCUT = "@deep_dark_bot";
    private final ConcurrentMap<Long, BotState> map = new ConcurrentHashMap<>();
    private List<String> answers;

    protected GachiBot(String token, String botName) throws Exception {
        super(token, botName);
        loadResources();
    }

    public static void main(String[] args) throws Exception {
//        if (args == null || args.length != 2) {
//            System.out.println("You must run bot with 2 args - BotToken and bot UserName");
//        } else {
//            ApiContextInitializer.init();
//            Bot.runBot(new GachiBot(args[0], args[1]));
//        }
        ApiContextInitializer.init();
        Bot.runBot(new GachiBot("1121619285:AAHF7b8rYO-ZP1rfWY-YaU3Kx0hldY_86H0", "GachiBot"));
    }

    protected void loadResources() throws FileNotFoundException {
        FileReader fileReader = new FileReader("src/main/resources/text/answers.txt");
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
                    } else if (checkTextIsCommand(text, "voicelist", isGroupChat)) {
                        sendTextMessage(chatId, getVoiceList(), true);
                    } else if (checkTextIsInlineCommand(text, "voice", isGroupChat)) {
                        List<String> commandParams = extractInlineCommandParams(text);
                        if (commandParams.size() == 1) {
                            String voiceName = commandParams.get(0);
                            File voice = getFileFromDirectoryByPartialName(VOICES_DIRECTORY, voiceName);
                            sendVoiceMessage(chatId, null, voice);
                        }
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

    private List<String> extractInlineCommandParams(String command) {
        List<String> result = new ArrayList<>();
        Pattern p1 = Pattern.compile("\"[A-Za-z0-9\\- ]+\"");
        Matcher m1 = p1.matcher(command);
        while (m1.find()) {
            String group = m1.group();
            result.add(group.substring(1, group.length() - 1));
        }
        return result;
    }

    private boolean checkTextIsInlineCommand(String text, String command, boolean isGroupChat) {
        return isGroupChat ?
                text.matches("\\s*" + BOT_SHORTCUT + "\\s*" + command + "\\s+(\"[A-Za-z0-9\\- ]+\")*") :
                text.matches("\\s*" + command + "\\s+(\"[A-Za-z0-9\\- ]+\")*");
    }

    private String getVoiceList() {
        return getVoiceListFromDirectory(VOICES_DIRECTORY);
    }

    private String getVoiceListFromDirectory(String directory) {
        String[] filePaths = getResourceFolderFilePaths(directory);
        List<String> voiceList = Arrays.stream(filePaths)
                .map(this::deleteExtension)
                .collect(Collectors.toList());
        List<String> resultList = new ArrayList<>();
        for (int i = 0; i < voiceList.size(); i++) {
            resultList.add(String.format("%d. %s", (i + 1), voiceList.get(i)));
        }
        String header = "List of available voice phrases:";
        String body = String.join("\n", resultList);
        String footer = String.format("To get voice by phrase just type" +
                " <b>voice \"Your voice phrase\"</b> in private chat or type" +
                " <b>%s voice \"Your voice phrase\"</b> in group chat.", BOT_SHORTCUT);
        return header + "\n\n" + body + "\n\n" + footer;
    }

    private String deleteExtension(String fileName) {
        int index = fileName.indexOf('.');
        return fileName.substring(0, index);
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

    private File getFileFromDirectoryByPartialName(String directory, String name) throws UnsupportedEncodingException {
        String[] filePaths = getResourceFolderFilePaths(directory);
        for (String filePath : filePaths) {
            if (filePath.contains(name)) {
                String fullFilePath = URLDecoder.decode(getClass().getClassLoader()
                        .getResource(directory + "/" + filePath).getPath(), "UTF-8");
                return new File(fullFilePath);
            }
        }
        return null;
    }

    private String[] getResourceFolderFilePaths(String folder) {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        URL url = loader.getResource(folder);
        return new File(url.getPath()).list();
    }
}
