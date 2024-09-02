package server.cubeTalk.common.util;

import org.springframework.stereotype.Service;

import java.util.Random;


public class RandomNicknameGenerator {
    private static final String[] ADJECTIVES = {
            "멍멍짖는 ", "냥냥거리는 ", "마라탕탕후루후루 ", "매콤달콤한 ", "칭얼거리는 ", "너무귀여운 ", "맴맴거리는 ", "사랑이모자란 ", "귀엽게공격하는 "
    };

    private static final String[] NOUNS = {
            "고양이", "강아지", "떡볶이", "감자칩", "양아치", "염소", "마이쮸 ", "매미", "레서판다"
    };

    private static final Random RANDOM = new Random();

    public static String generateNickname() {
        String adjective = ADJECTIVES[RANDOM.nextInt(ADJECTIVES.length)];
        String noun = NOUNS[RANDOM.nextInt(NOUNS.length)];
        return adjective + noun;
    }

}
