package com.yube.bot;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;
import com.yube.utils.FileUtils;
import com.yube.utils.JsonUtils;
import com.yube.utils.TextUtils;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
    private final static String VIDEOS_PLAYLIST = "https://www.youtube.com/playlist?list=PLHHYuo8wPxUxa_yHIB3Je8RsNzDP08uut";
    private final ConcurrentMap<Long, BotState> map = new ConcurrentHashMap<>();
    private List<String> answers;
    private List<String> videoIds;
    private List<String[]> voiceMetadata;

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

    protected void loadResources() throws IOException, CsvException {
        loadAnswers();
        loadVideoIds();
        loadVoiceMetadata();
    }

    protected void loadAnswers() throws FileNotFoundException {
        FileReader fileReader = new FileReader("src/main/resources/text/answers.txt");
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        answers = bufferedReader.lines().collect(Collectors.toList());
    }

    protected void loadVideoIds() throws IOException {
        videoIds = getVideoIdsFromPlaylist(VIDEOS_PLAYLIST);
    }

    protected void loadVoiceMetadata() throws IOException, CsvException {
        CSVReader reader = new CSVReader(new FileReader("src/main/resources/sound/metadata.csv"));
        voiceMetadata = reader.readAll();
        reader.close();
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
                        File picture = FileUtils.getRandomFileFromDirectory(PICTURES_DIRECTORY);
                        sendPhotoMessage(chatId, null, picture);
                    } else if (checkTextIsCommand(text, "randomgif", isGroupChat)) {
                        File gif = FileUtils.getRandomFileFromDirectory(GIFS_DIRECTORY);
                        sendAnimationMessage(chatId, null, gif);
                    } else if (checkTextIsCommand(text, "voicelist", isGroupChat)) {
                        sendTextMessage(chatId, getVoiceList(), true);
                    } else if (checkTextIsCommand(text, "randomgachibass", isGroupChat)) {
                        String videoUrl = getRandomYoutubeVideoUrl();
                        sendTextMessage(chatId, videoUrl);
                    } else if (checkTextIsInlineCommand(text, "voice", isGroupChat)) {
                        List<Object> commandParams = extractInlineCommandParams(text);
                        if (commandParams.size() == 1) {
                            Object voiceParamValue = commandParams.get(0);
                            File voice = null;
                            if (voiceParamValue instanceof String) {
                                String voiceName = (String) voiceParamValue;
                                voice = getVoiceByName(voiceName);
                            } else if (voiceParamValue instanceof Integer) {
                                int voiceCode = (Integer) voiceParamValue;
                                voice = getVoiceByCode(voiceCode);
                            }
                            if (voice != null) {
                                sendVoiceMessage(chatId, null, voice);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            processTheException(e);
        }
    }

    private String getRandomYoutubeVideoUrl() {
        String videoId = videoIds.get((int) (videoIds.size() * Math.random()));
        return "https://www.youtube.com/watch?v=" + videoId;
    }

    private List<String> getVideoIdsFromPlaylist(String playlistUrl) throws IOException {
        Document pageDocument = Jsoup.connect(playlistUrl).get();
        String pageScriptsText = pageDocument.select("script").html();
        System.out.println(pageScriptsText);
        List<String> playlistVideoListRendererList =
                JsonUtils.getJsonListByContainingField(pageScriptsText, "playlistVideoListRenderer");
        List<String> videoIds = new ArrayList<>();
        if (!playlistVideoListRendererList.isEmpty()) {
            String playlistVideoListRenderer = playlistVideoListRendererList.get(0);
            JSONObject playlistVideoListRendererJson = new JSONObject(playlistVideoListRenderer)
                    .getJSONObject("playlistVideoListRenderer");
            List<JSONObject> contentJsons = JsonUtils.getJsonObjects(playlistVideoListRendererJson.getJSONArray("contents"));
            for (JSONObject contentJson : contentJsons) {
                if (!contentJson.has("playlistVideoRenderer")) continue;
                JSONObject playlistVideoRendererJson = contentJson.getJSONObject("playlistVideoRenderer");
                boolean isPlayable = playlistVideoRendererJson.getBoolean("isPlayable");
                if (!isPlayable) continue;
                String videoId = playlistVideoRendererJson.getString("videoId");
                videoIds.add(videoId);
            }
        }
        return videoIds;
    }

    private boolean checkTextIsCommand(String text, String command, boolean isGroupChat) {
        command = "/" + command;
        return isGroupChat ? (command + BOT_SHORTCUT).equals(text) : command.equals(text);
    }

    private List<Object> extractInlineCommandParams(String command) {
        List<Object> result = new ArrayList<>();
        Pattern p1 = Pattern.compile("(\".+\"|\\d+)");
        Matcher m1 = p1.matcher(command);
        while (m1.find()) {
            String group = m1.group();
            if (TextUtils.isInteger(group)) {
                result.add(Integer.parseInt(group));
            } else {
                result.add(group.substring(1, group.length() - 1));
            }
        }
        return result;
    }

    private boolean checkTextIsInlineCommand(String text, String command, boolean isGroupChat) {
        return isGroupChat ?
                text.matches("\\s*" + BOT_SHORTCUT + "\\s*" + command + "\\s+(\".+\"|\\d+)(\\s+(\".+\"|\\d+))*") :
                text.matches("\\s*" + command + "\\s+(\".+\"|\\d+)(\\s+(\".+\"|\\d+))*");
    }

    private File getVoiceByName(String name) throws UnsupportedEncodingException {
        Optional<String[]> searchedRow = voiceMetadata.stream()
                .filter(row -> row[1].contains(name))
                .findFirst();
        if (searchedRow.isPresent()) {
            String fileName = searchedRow.get()[2];
            return FileUtils.getFileFromDirectoryByName(VOICES_DIRECTORY, fileName);
        } else {
            return null;
        }
    }

    private File getVoiceByCode(int code) throws UnsupportedEncodingException {
        Optional<String[]> searchedRow = voiceMetadata.stream()
                .filter(row -> Integer.parseInt(row[0]) == code)
                .findFirst();
        if (searchedRow.isPresent()) {
            String fileName = searchedRow.get()[2];
            return FileUtils.getFileFromDirectoryByName(VOICES_DIRECTORY, fileName);
        } else {
            return null;
        }
    }

    private String getVoiceList() {
        String header = "List of available voice phrases:";
        List<String> bodyLines = new ArrayList<>();
        for (String[] row : voiceMetadata) {
            bodyLines.add(String.format("%d. %s", Integer.parseInt(row[0]), row[1]));
        }
        String body = String.join("\n", bodyLines);
        String footer = String.format("To get voice by phrase just type" +
                        " <b>voice <i>\"Voice Phrase\"</i></b> or <b>voice <i>Voice Code</i></b> in private chat and type" +
                        " <b>%s voice <i>\"Voice Phrase\"</i></b> or <b>%s voice <i>Voice Code</i></b> in group chat.",
                BOT_SHORTCUT, BOT_SHORTCUT);
        return header + "\n\n" + body + "\n\n" + footer;
    }

    private String getRandomAnswer() {
        return answers.get((int) (answers.size() * Math.random()));
    }
}
