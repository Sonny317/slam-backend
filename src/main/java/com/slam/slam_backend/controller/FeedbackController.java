package com.slam.slam_backend.controller;

import com.slam.slam_backend.entity.Feedback;
import com.slam.slam_backend.entity.GameFeedback;
import com.slam.slam_backend.entity.User;
import com.slam.slam_backend.entity.Event;
import com.slam.slam_backend.repository.FeedbackRepository;
import com.slam.slam_backend.repository.GameFeedbackRepository;
import com.slam.slam_backend.repository.UserRepository;
import com.slam.slam_backend.repository.EventRepository;
import com.slam.slam_backend.repository.EventGameRepository;
import com.slam.slam_backend.entity.EventGame;
import com.slam.slam_backend.repository.EventRsvpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackRepository feedbackRepository;
    private final GameFeedbackRepository gameFeedbackRepository;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final EventGameRepository eventGameRepository;
    private final EventRsvpRepository eventRsvpRepository;
    
    private boolean isAdmin(User user) {
        if (user == null) return false;
        String role = user.getRole().name();
        System.out.println("Checking role: " + role);
        return "ADMIN".equals(role) || "PRESIDENT".equals(role) || "STAFF".equals(role);
    }

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
            
            // 새로운 필드들을 comment에 JSON으로 저장
            Map<String, Object> additionalData = new HashMap<>();
            additionalData.put("leadershipInterest", body.get("leadershipInterest"));
            additionalData.put("reelsParticipation", body.get("reelsParticipation"));
            additionalData.put("goodPoints", body.get("goodPoints"));
            additionalData.put("improvements", body.get("improvements"));
            
            // Step 2 게임 데이터도 추가
            additionalData.put("gameRatings", body.get("gameRatings"));
            additionalData.put("gameNotes", body.get("gameNotes"));
            
            // 기존 comment와 새로운 데이터를 합쳐서 저장
            String existingComment = (String) body.getOrDefault("comment", "");
            String jsonData = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(additionalData);
            f.setComment((existingComment != null ? existingComment : "") + "\n\n[ADDITIONAL_DATA]" + jsonData);
            
            feedbackRepository.save(f);

            // 게임 평점 및 메모 처리
            @SuppressWarnings("unchecked")
            Map<String, Integer> gameRatings = (Map<String, Integer>) body.get("gameRatings");
            @SuppressWarnings("unchecked")
            Map<String, Object> gameNotes = (Map<String, Object>) body.get("gameNotes");
            
            if (gameRatings != null) {
                for (Map.Entry<String, Integer> e : gameRatings.entrySet()) {
                    if (e.getValue() > 0) { // 0점 이상인 경우만 저장
                        GameFeedback g = new GameFeedback();
                        g.setEventId(eventId);
                        g.setGameId(e.getKey());
                        g.setRating(e.getValue());
                        
                        // 필수 필드들에 기본값 설정
                        g.setEngagement(e.getValue()); // rating과 동일한 값으로 설정
                        g.setDifficulty(3); // 중간값으로 설정
                        g.setActualParticipants(1); // 최소값으로 설정
                        
                        // 게임 메모 처리 (Positive/Negative 옵션과 Add your own feedback)
                        StringBuilder commentBuilder = new StringBuilder();
                        if (gameNotes != null && gameNotes.containsKey(e.getKey())) {
                            Object notes = gameNotes.get(e.getKey());
                            if (notes instanceof List && !((List<?>) notes).isEmpty()) {
                                @SuppressWarnings("unchecked")
                                List<String> noteList = (List<String>) notes;
                                commentBuilder.append(String.join("; ", noteList));
                            }
                        }
                        
                        g.setComment(commentBuilder.toString());
                        g.setActualDuration(null);
                        g.setOrganizerNotes("");
                        g.setSubmittedBy("anonymous"); // 익명 사용자로 설정
                        
                        gameFeedbackRepository.save(g);
                    }
                }
            }
            
            return ResponseEntity.ok(Map.of("success", true, "message", "Feedback submitted successfully"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    @GetMapping("/event/{eventId}/summary")
    public ResponseEntity<?> eventSummary(@PathVariable Long eventId) {
        List<Feedback> all = feedbackRepository.findByEventId(eventId);
        int count = all.size();
        double overallAvg = all.stream().filter(f -> f.getOverall() != null).mapToInt(Feedback::getOverall).average().orElse(0);
        double npsAvg = all.stream().filter(f -> f.getNps() != null).mapToInt(Feedback::getNps).average().orElse(0);
        double participantsFitAvg = all.stream().filter(f -> f.getParticipantsFit() != null).mapToInt(Feedback::getParticipantsFit).average().orElse(0);
        double interactionOpportunityAvg = all.stream().filter(f -> f.getInteractionOpportunity() != null).mapToInt(Feedback::getInteractionOpportunity).average().orElse(0);
        double languageConfidenceAvg = all.stream().filter(f -> f.getLanguageConfidence() != null).mapToInt(Feedback::getLanguageConfidence).average().orElse(0);
        
        // Response Rate 계산 (실제 Will attend 수 대비 피드백 제출자 비율)
        Event event = eventRepository.findById(eventId).orElse(null);
        double responseRate = 0.0;
        if (event != null) {
            // 실제 "Will attend" 수 조회 (RSVP에서 isAttending = true인 사람들)
            long willAttendCount = eventRsvpRepository.countByEvent_IdAndIsAttendingTrue(eventId);
            System.out.println("Event ID: " + eventId + ", Will Attend: " + willAttendCount + ", Feedback Count: " + count);
            if (willAttendCount > 0) {
                responseRate = (double) count / willAttendCount * 100;
            } else {
                // Will attend가 0이면 0%로 표시
                responseRate = 0.0;
            }
        } else {
            System.out.println("Event not found for ID: " + eventId);
        }
        
        // JSON 데이터에서 추가 정보 추출
        List<String> goodPoints = new ArrayList<>();
        List<String> improvements = new ArrayList<>();
        List<String> top3Activities = new ArrayList<>();
        long leadershipYes = 0;
        long reelsYes = 0;
        
        for (Feedback f : all) {
            if (f.getComment() != null && f.getComment().contains("[ADDITIONAL_DATA]")) {
                try {
                    String jsonPart = f.getComment().substring(f.getComment().indexOf("[ADDITIONAL_DATA]") + 17);
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> additionalData = mapper.readValue(jsonPart, Map.class);
                    
                    String goodPoint = (String) additionalData.get("goodPoints");
                    if (goodPoint != null && !goodPoint.trim().isEmpty()) {
                        goodPoints.add(goodPoint);
                    }
                    
                    String improvement = (String) additionalData.get("improvements");
                    if (improvement != null && !improvement.trim().isEmpty()) {
                        improvements.add(improvement);
                    }
                    
                    String leadership = (String) additionalData.get("leadershipInterest");
                    if (leadership != null && leadership.startsWith("yes")) {
                        leadershipYes++;
                    }
                    
                    String reels = (String) additionalData.get("reelsParticipation");
                    if (reels != null && reels.startsWith("yes")) {
                        reelsYes++;
                    }
                } catch (Exception e) {
                    // JSON 파싱 실패 시 무시
                }
            }
            
            // top3Activities는 별도로 처리 (additionalData가 아닌 직접 필드)
            String top3 = f.getTop3Activities();
            if (top3 != null && !top3.trim().isEmpty()) {
                top3Activities.add(top3);
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("count", count);
        response.put("overallAvg", overallAvg);
        response.put("npsAvg", npsAvg);
        response.put("participantsFitAvg", participantsFitAvg);
        response.put("interactionOpportunityAvg", interactionOpportunityAvg);
        response.put("languageConfidenceAvg", languageConfidenceAvg);
        response.put("responseRate", responseRate);
        response.put("goodPoints", goodPoints);
        response.put("improvements", improvements);
        response.put("top3Activities", top3Activities);
        response.put("leadershipInterestCount", leadershipYes);
        response.put("reelsParticipationCount", reelsYes);
        
        return ResponseEntity.ok(response);
    }

    @GetMapping("/event/{eventId}/games")
    public ResponseEntity<?> eventGames(@PathVariable Long eventId) {
        // GameFeedback 테이블에서 직접 데이터 가져오기
        List<GameFeedback> list = gameFeedbackRepository.findByEventId(eventId);
        
        // 게임별 평점 집계
        Map<String, Object> gameStats = list.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    GameFeedback::getGameId,
                    java.util.stream.Collectors.collectingAndThen(
                        java.util.stream.Collectors.toList(),
                        feedbacks -> {
                            if (feedbacks.isEmpty()) return new HashMap<>();
                            
                            double avgRating = feedbacks.stream()
                                    .mapToInt(GameFeedback::getRating)
                                    .average()
                                    .orElse(0.0);
                            
                            // Custom Feedback Count 계산 (Add your own feedback 텍스트가 있는 항목 수)
                            long customFeedbackCount = feedbacks.stream()
                                    .filter(f -> {
                                        if (f.getComment() == null || f.getComment().trim().isEmpty()) {
                                            return false;
                                        }
                                        String comment = f.getComment();
                                        // 세미콜론으로 구분된 항목들 중에서 실제 커스텀 피드백 텍스트 찾기
                                        String[] parts = comment.split(";");
                                        for (String part : parts) {
                                            String trimmed = part.trim();
                                            if (!trimmed.isEmpty() && 
                                                !trimmed.contains("Fun & engaging") && 
                                                !trimmed.contains("Helped me connect") && 
                                                !trimmed.contains("Right duration") && 
                                                !trimmed.contains("Boring") && 
                                                !trimmed.contains("Group size issue") && 
                                                !trimmed.contains("Confusing") && 
                                                !trimmed.contains("unclear")) {
                                                return true; // 실제 커스텀 피드백 텍스트가 있음
                                            }
                                        }
                                        return false;
                                    })
                                    .count();
                                    
                            Map<String, Object> result = new HashMap<>();
                            result.put("gameId", feedbacks.get(0).getGameId());
                            result.put("rating", avgRating);
                            result.put("responseCount", feedbacks.size());
                            result.put("customFeedbackCount", customFeedbackCount);
                            return result;
                        }
                    )
                ));
        
        return ResponseEntity.ok(gameStats.values());
    }

    @GetMapping("/event/{eventId}/game/{gameId}/details")
    public ResponseEntity<?> getGameDetails(@PathVariable Long eventId, @PathVariable String gameId) {
        try {
            List<GameFeedback> gameFeedbacks = gameFeedbackRepository.findByEventIdAndGameId(eventId, gameId);
            
            List<Map<String, Object>> details = new ArrayList<>();
            for (GameFeedback feedback : gameFeedbacks) {
                Map<String, Object> detail = new HashMap<>();
                detail.put("id", feedback.getId());
                detail.put("rating", feedback.getRating());
                detail.put("comment", feedback.getComment());
                detail.put("submittedBy", feedback.getSubmittedBy());
                detail.put("createdAt", feedback.getCreatedAt());
                
                // Positive/Negative 옵션 파싱 (comment에서)
                List<String> positiveOptions = new ArrayList<>();
                List<String> negativeOptions = new ArrayList<>();
                
                if (feedback.getComment() != null) {
                    String comment = feedback.getComment();
                    
                    // Positive 옵션들
                    if (comment.contains("Fun & engaging")) positiveOptions.add("Fun & engaging");
                    if (comment.contains("Helped me connect")) positiveOptions.add("Helped me connect");
                    if (comment.contains("Right duration")) positiveOptions.add("Right duration");
                    
                    // Negative 옵션들
                    if (comment.contains("Boring")) negativeOptions.add("Boring");
                    if (comment.contains("Group size issue")) negativeOptions.add("Group size issue (too many/few)");
                    if (comment.contains("Confusing") || comment.contains("unclear")) negativeOptions.add("Confusing / unclear");
                }
                
                detail.put("positiveOptions", positiveOptions);
                detail.put("negativeOptions", negativeOptions);
                
                // Add your own feedback 내용 추출
                String customFeedback = "";
                if (feedback.getComment() != null) {
                    String comment = feedback.getComment();
                    // 세미콜론으로 구분된 항목들 중에서 옵션이 아닌 실제 텍스트 찾기
                    String[] parts = comment.split(";");
                    for (String part : parts) {
                        String trimmed = part.trim();
                        if (!trimmed.isEmpty() && 
                            !trimmed.contains("Fun & engaging") && 
                            !trimmed.contains("Helped me connect") && 
                            !trimmed.contains("Right duration") && 
                            !trimmed.contains("Boring") && 
                            !trimmed.contains("Group size issue") && 
                            !trimmed.contains("Confusing") && 
                            !trimmed.contains("unclear")) {
                            customFeedback = trimmed;
                            break;
                        }
                    }
                }
                
                detail.put("customFeedback", customFeedback);
                details.add(detail);
            }
            
            return ResponseEntity.ok(details);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/event/{eventId}/details")
    public ResponseEntity<?> eventFeedbackDetails(@PathVariable Long eventId) {
        List<Feedback> all = feedbackRepository.findByEventId(eventId);
        
        List<Map<String, Object>> details = new ArrayList<>();
        for (Feedback f : all) {
            Map<String, Object> detail = new HashMap<>();
            detail.put("id", f.getId());
            detail.put("role", f.getRole());
            detail.put("overall", f.getOverall());
            detail.put("participantsFit", f.getParticipantsFit());
            detail.put("interactionOpportunity", f.getInteractionOpportunity());
            detail.put("languageConfidence", f.getLanguageConfidence());
            detail.put("nps", f.getNps());
            detail.put("top3Activities", f.getTop3Activities());
            detail.put("comment", f.getComment());
            detail.put("createdAt", f.getCreatedAt());
            
            // JSON 데이터에서 추가 정보 추출
            if (f.getComment() != null && f.getComment().contains("[ADDITIONAL_DATA]")) {
                try {
                    String jsonPart = f.getComment().substring(f.getComment().indexOf("[ADDITIONAL_DATA]") + 17);
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    @SuppressWarnings("unchecked")
                    Map<String, Object> additionalData = mapper.readValue(jsonPart, Map.class);
                    
                    detail.put("leadershipInterest", additionalData.get("leadershipInterest"));
                    detail.put("reelsParticipation", additionalData.get("reelsParticipation"));
                    detail.put("goodPoints", additionalData.get("goodPoints"));
                    detail.put("improvements", additionalData.get("improvements"));
                    
                    // Step 2 게임별 데이터 추출
                    @SuppressWarnings("unchecked")
                    Map<String, Integer> gameRatings = (Map<String, Integer>) additionalData.get("gameRatings");
                    if (gameRatings != null) {
                        detail.put("gameRatings", gameRatings);
                    }
                    
                    @SuppressWarnings("unchecked")
                    Map<String, Object> gameNotes = (Map<String, Object>) additionalData.get("gameNotes");
                    if (gameNotes != null) {
                        detail.put("gameNotes", gameNotes);
                    }
                } catch (Exception e) {
                    // JSON 파싱 실패 시 기본값
                    detail.put("leadershipInterest", "");
                    detail.put("reelsParticipation", "");
                    detail.put("goodPoints", "");
                    detail.put("improvements", "");
                    detail.put("gameRatings", new HashMap<>());
                    detail.put("gameNotes", new HashMap<>());
                }
            } else {
                detail.put("leadershipInterest", "");
                detail.put("reelsParticipation", "");
                detail.put("goodPoints", "");
                detail.put("improvements", "");
                detail.put("gameRatings", new HashMap<>());
                detail.put("gameNotes", new HashMap<>());
            }
            
            details.add(detail);
        }
        
        return ResponseEntity.ok(details);
    }

    @DeleteMapping("/event/{eventId}")
    public ResponseEntity<?> deleteEventFeedback(@PathVariable Long eventId, Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Login required."));
        }
        
        String userEmail = authentication.getName();
        User user = userRepository.findByEmail(userEmail).orElse(null);
        
        System.out.println("Delete feedback request - User email: " + userEmail);
        System.out.println("User found: " + (user != null));
        if (user != null) {
            System.out.println("User role: " + user.getRole());
            System.out.println("Is admin: " + isAdmin(user));
        }
        
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "로그인이 필요합니다."));
        }
        
        // 관리자 권한 체크
        if (!isAdmin(user)) {
            return ResponseEntity.status(403).body(Map.of("error", "관리자 권한이 필요합니다. 현재 역할: " + user.getRole()));
        }
        
        try {
            // 해당 이벤트의 모든 피드백 삭제
            List<Feedback> feedbacks = feedbackRepository.findByEventId(eventId);
            System.out.println("Found " + feedbacks.size() + " feedbacks to delete for event " + eventId);
            
            if (!feedbacks.isEmpty()) {
                feedbackRepository.deleteAll(feedbacks);
                System.out.println("Deleted " + feedbacks.size() + " feedbacks");
            }
            
            // 해당 이벤트의 모든 게임 피드백 삭제
            List<GameFeedback> gameFeedbacks = gameFeedbackRepository.findByEventId(eventId);
            System.out.println("Found " + gameFeedbacks.size() + " game feedbacks to delete for event " + eventId);
            
            if (!gameFeedbacks.isEmpty()) {
                gameFeedbackRepository.deleteAll(gameFeedbacks);
                System.out.println("Deleted " + gameFeedbacks.size() + " game feedbacks");
            }
            
            // 해당 이벤트의 모든 게임 할당 삭제 (EventGame 테이블)
            List<EventGame> eventGames = eventGameRepository.findByEventId(eventId);
            System.out.println("Found " + eventGames.size() + " event games to delete for event " + eventId);
            
            if (!eventGames.isEmpty()) {
                eventGameRepository.deleteAll(eventGames);
                System.out.println("Deleted " + eventGames.size() + " event games");
            }
            
            int totalDeleted = feedbacks.size() + gameFeedbacks.size() + eventGames.size();
            System.out.println("Total deleted: " + totalDeleted);
            
            return ResponseEntity.ok(Map.of("message", "피드백이 삭제되었습니다.", "deletedCount", totalDeleted));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/events")
    public ResponseEntity<?> getEventsWithFeedback() {
        try {
            // 피드백이 있는 이벤트 ID들을 가져옴
            List<Long> eventIdsWithFeedback = feedbackRepository.findDistinctEventIds();
            
            // 게임이 할당된 이벤트 ID들도 가져옴 (EventGame 테이블에서)
            List<Long> eventIdsWithGames = eventGameRepository.findDistinctEventIds();
            
            // 두 리스트를 합쳐서 중복 제거
            Set<Long> allEventIds = new HashSet<>();
            allEventIds.addAll(eventIdsWithFeedback);
            allEventIds.addAll(eventIdsWithGames);
            
            if (allEventIds.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }
            
            // 해당 이벤트들의 상세 정보를 가져옴
            List<Event> events = eventRepository.findByIdIn(new ArrayList<>(allEventIds));
            
            // EventDTO로 변환하여 반환
            List<Map<String, Object>> eventList = new ArrayList<>();
            for (Event event : events) {
                Map<String, Object> eventData = new HashMap<>();
                eventData.put("id", event.getId());
                eventData.put("eventId", event.getId());
                eventData.put("title", event.getTitle());
                eventData.put("eventTitle", event.getTitle());
                eventData.put("branch", event.getBranch());
                eventData.put("eventDateTime", event.getEventDateTime());
                eventData.put("date", event.getEventDateTime());
                eventData.put("location", event.getLocation());
                eventData.put("description", event.getDescription());
                eventData.put("imageUrl", event.getImageUrl());
                eventData.put("capacity", event.getCapacity());
                eventData.put("currentAttendees", event.getCurrentAttendees());
                eventData.put("price", event.getPrice());
                eventList.add(eventData);
            }
            
            return ResponseEntity.ok(eventList);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}


