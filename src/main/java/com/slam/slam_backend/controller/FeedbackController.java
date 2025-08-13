package com.slam.slam_backend.controller;

import com.slam.slam_backend.entity.Feedback;
import com.slam.slam_backend.entity.GameFeedback;
import com.slam.slam_backend.repository.FeedbackRepository;
import com.slam.slam_backend.repository.GameFeedbackRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;
    private final GameFeedbackRepository gameFeedbackRepository;

    @PostMapping("/member/submit")
    public ResponseEntity<?> submitMemberFeedback(@RequestBody Map<String, Object> body) {
        try {
            Long eventId = Long.valueOf(String.valueOf(body.get("eventId")));
            String top3 = (String) body.getOrDefault("top3Activities", "");
            Feedback f = new Feedback();
            f.setEventId(eventId);
            f.setRole("member");
            f.setOverall((Integer) body.get("overall"));
            f.setParticipantsFit((Integer) body.get("participantsFit"));
            f.setInteractionOpportunity((Integer) body.get("interactionOpportunity"));
            f.setLanguageConfidence((Integer) body.get("languageConfidence"));
            f.setNps((Integer) body.get("nps"));
            f.setTop3Activities(top3);
            f.setComment((String) body.get("comment"));
            feedbackRepository.save(f);

            Map<String, Integer> gameRatings = (Map<String, Integer>) body.get("gameRatings");
            if (gameRatings != null) {
                for (Map.Entry<String, Integer> e : gameRatings.entrySet()) {
                    GameFeedback g = new GameFeedback();
                    g.setEventId(eventId);
                    g.setGameId(e.getKey());
                    g.setRating(e.getValue());
                    gameFeedbackRepository.save(g);
                }
            }
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/event/{eventId}/summary")
    public ResponseEntity<?> eventSummary(@PathVariable Long eventId) {
        List<Feedback> all = feedbackRepository.findByEventId(eventId);
        int count = all.size();
        double overallAvg = all.stream().filter(f -> f.getOverall() != null).mapToInt(Feedback::getOverall).average().orElse(0);
        double npsAvg = all.stream().filter(f -> f.getNps() != null).mapToInt(Feedback::getNps).average().orElse(0);
        return ResponseEntity.ok(Map.of(
                "count", count,
                "overallAvg", overallAvg,
                "npsAvg", npsAvg
        ));
    }

    @GetMapping("/event/{eventId}/games")
    public ResponseEntity<?> eventGames(@PathVariable Long eventId) {
        List<GameFeedback> list = gameFeedbackRepository.findByEventId(eventId);
        return ResponseEntity.ok(list);
    }
}


