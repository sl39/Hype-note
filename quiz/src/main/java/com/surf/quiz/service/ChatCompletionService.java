package com.surf.quiz.service;


import com.surf.quiz.dto.gpt.ChatResponse;
import com.surf.quiz.dto.gpt.Usage;
import com.surf.quiz.fegin.ChatCompletionClient;
import com.surf.quiz.dto.gpt.ChatRequest;
import com.surf.quiz.dto.gpt.Message;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import kr.co.shineware.nlp.komoran.constant.DEFAULT_MODEL;
import kr.co.shineware.nlp.komoran.core.Komoran;
import kr.co.shineware.nlp.komoran.model.Token;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatCompletionService {
    private final ChatCompletionClient chatCompletionClient;
    private final static String ROLE_USER = "user";
    private final static String MODEL = "gpt-3.5-turbo";
    private static final Set<String> STOPWORDS = new HashSet<>();

    static {
        try {
            BufferedReader br = new BufferedReader(new FileReader("src/main/resources/static/stopwords.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                STOPWORDS.add(line.trim());
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private static final Set<String> PUNCTUATIONS = new HashSet<>();

    static {
        try {
            BufferedReader br = new BufferedReader(new FileReader("src/main/resources/static/punctuations.txt"));
            String line;
            while ((line = br.readLine()) != null) {
                PUNCTUATIONS.add(line.trim());
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Value("${apikey}")
    private String apikey;

    public List<String> chatCompletions(int cnt, final String question) {
        if (cnt < 0 || question == null || question.trim().isEmpty()){
            return null;
        }

        // 텍스트 전처리
        String content = preprocessContent(question);

        // 키워드 추출
        List<String> keywords = extractKeywords(content);

        // 키워드가 포함된 문장 추출
        String analysisInput = extractKeywordSentences(content, keywords);
        System.out.println("analysisInput = " + analysisInput);

        Message systemMessage = Message.builder()
                .role("system")
                .content("Computer Science에 대한 문제를 만들고 싶어\n" +
                        "문제는 4지선다 문제로\n 정답은 1개야" +
                        "아래 형식으로 만들고 싶어\n" +
                        "    {\n" +
                        "      \"id\": 번호,\n" +
                        "      \"question\": \"문제 내용\",\n" +
                        "      \"example\": [\n" +
                        "        {\n" +
                        "          \"ex\": \"1\",\n" +
                        "          \"content\": \"보기 내용\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"ex\": \"2\",\n" +
                        "          \"content\": \"보기 내용\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"ex\": \"3\",\n" +
                        "          \"content\": \"보기 내용\"\n" +
                        "        },\n" +
                        "        {\n" +
                        "          \"ex\": \"4\",\n" +
                        "          \"content\": \"보기 내용\"\n" +
                        "        }\n" +
                        "      ],\n" +
                        "      \"answer\": \"정답\",\n" +
                        "      \"commentary\": \"해설 내용\"\n" +
                        "    }\n" +
                        "문제의 갯수는"+ cnt +"개로 List로 응답해줘\n")
                .build();


//        Message systemMessage = Message.builder()
//                .role("system")
//                .content("I would like to create Computer Science problems.\n" +
//                        "These problems should be in a multiple-choice question format with the following structure:\n" +
//                        "{\n" +
//                        "  \"id\": Problem number,\n" +
//                        "  \"question\": \"Problem content\",\n" +
//                        "  \"example\": [\n" +
//                        "    {\n" +
//                        "      \"ex\": \"1\",\n" +
//                        "      \"content\": \"Option content\"\n" +
//                        "    },\n" +
//                        "    {\n" +
//                        "      \"ex\": \"2\",\n" +
//                        "      \"content\": \"Option content\"\n" +
//                        "    },\n" +
//                        "    {\n" +
//                        "      \"ex\": \"3\",\n" +
//                        "      \"content\": \"Option content\"\n" +
//                        "    },\n" +
//                        "    {\n" +
//                        "      \"ex\": \"4\",\n" +
//                        "      \"content\": \"Option content\"\n" +
//                        "    }\n" +
//                        "  ],\n" +
//                        "  \"answer\": \"Correct answer\",\n" +
//                        "  \"commentary\": \"Explanation content\"\n" +
//                        "}\n" +
//                        "Please provide the total number of problems as " + cnt + " and present them in List format.\"")
//                .build();


        Message message = Message.builder()
                .role(ROLE_USER)
                .content(analysisInput)
                .build();
        ChatRequest chatRequest = ChatRequest.builder()
                .model(MODEL)
                .messages(Arrays.asList(systemMessage, message))
                .build();

        ChatResponse chatResponse = chatCompletionClient
                .chatCompletions(apikey, chatRequest);

        Usage usage = chatResponse.getUsage();
        System.out.println("Usage: " + usage);

        // 응답을 List에 넣기
        List<String> responses = chatResponse
                .getChoices()
                .stream()
                .map(choice -> choice.getMessage().getContent())
                .collect(Collectors.toList());

        return responses;

    }

    // 텍스트 전처리
    private String preprocessContent(String content) {
        String text = extractTextFromHtml(content);
        return text.replaceAll("[^a-zA-Z0-9가-힣|]", "");
    }

    // html 변환
    public String extractTextFromHtml(String html) {
        org.jsoup.nodes.Document document = Jsoup.parse(html);
        Elements elements = document.body().children();
        List<String> texts = new ArrayList<>();

        for (Element element : elements) {
            texts.add(element.text());
        }

        return String.join(". ", texts); // '|'를 구분자로 사용
    }

    // 키워드 추출
    private List<String> extractKeywords(String content) {
        return extractKeywordsWithKomoran(content, 5); // 상위 5개 키워드 추출
    }

    // 코모란
    private List<String> extractKeywordsWithKomoran(String content, int topN) {
        Komoran komoran = new Komoran(DEFAULT_MODEL.FULL);
        Map<String, Integer> wordCount = new HashMap<>();

        List<Token> tokens = komoran.analyze(content).getTokenList();
        for (Token token : tokens) {
            String word = token.getMorph();
            String pos = token.getPos();
            if ((pos.startsWith("NN") || pos.startsWith("SL")) && (!STOPWORDS.contains(word) && !PUNCTUATIONS.contains(word))) {
                wordCount.put(word, wordCount.getOrDefault(word, 0) + 1);
            }
        }

        return wordCount.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(topN)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    // 키워드 포함한 문장 추출
    private String extractKeywordSentences(String content, List<String> keywords) {
        List<String> sentences = Arrays.stream(content.split("\\|\\|\\|"))
                .filter(sentence -> keywords.stream().anyMatch(sentence::contains))
                .collect(Collectors.toList());

        return String.join("|||", sentences);
    }
}