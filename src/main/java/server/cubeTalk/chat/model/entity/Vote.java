package server.cubeTalk.chat.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class Vote {
    private int support;
    private int opposite;
    private List<String> mvp;

    public Vote addMVP(String newMvp) {
        List<String> updatedMvpList = new ArrayList<>(this.mvp);
        updatedMvpList.add(newMvp);

        return Vote.builder()
                .support(this.support)
                .opposite(this.opposite)
                .mvp(updatedMvpList)
                .build();
    }


    public String calculateMVP() {
        Map<String, Integer> frequencyMap = new HashMap<>();

        // 각 닉네임의 등장 횟수 계산
        for (String name : this.mvp) {
            frequencyMap.put(name, frequencyMap.getOrDefault(name, 0) + 1);
        }

        // 등장 횟수가 가장 많은 MVP 선정
        return frequencyMap.entrySet().stream()
                .max(Map.Entry.comparingByValue())  // 가장 높은 값(등장 횟수)을 가진 항목을 찾음
                .map(Map.Entry::getKey)            // 해당 키(닉네임)를 반환
                .orElse(null);                     // 빈 경우에는 null 반환
    }
}
