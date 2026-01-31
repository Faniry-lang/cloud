package itu.cloud.controllers;

import itu.cloud.collections.SignalementCollection;
import itu.cloud.firebase.services.SignalementFirebaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/firebase/signalements")
public class SignalementFirebaseController {

    private final SignalementFirebaseService signalementFirebaseService;

    public SignalementFirebaseController(SignalementFirebaseService signalementFirebaseService) {
        this.signalementFirebaseService = signalementFirebaseService;
    }

    @GetMapping
    public Map<String, Object> getSignalementsWithStats() throws ExecutionException, InterruptedException {
        List<SignalementCollection> signalements = signalementFirebaseService.find(null);
        
        int totalCount = signalements.size();
        double totalSurface = 0;
        double totalBudget = 0;
        int completedCount = 0;
        int inProgressCount = 0;

        for (SignalementCollection sig : signalements) {
            totalSurface += (sig.getSurfaceM2() != null ? sig.getSurfaceM2() : 0);
            totalBudget += (sig.getBudget() != null ? sig.getBudget() : 0);
            
            Integer statut = sig.getStatut();
            if (statut == null) statut = 0; 
            
            if (statut == 2) {
                completedCount++;
            } else if (statut == 1) {
                inProgressCount++;
            }
        }

        double progress = totalCount > 0 ? ((completedCount + (inProgressCount * 0.5)) / totalCount) * 100 : 0;

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalCount", totalCount);
        stats.put("totalSurface", Math.round(totalSurface * 100.0) / 100.0);
        stats.put("totalBudget", Math.round(totalBudget * 100.0) / 100.0);
        stats.put("progress", Math.round(progress * 10.0) / 10.0);

        Map<String, Object> response = new HashMap<>();
        response.put("signalements", signalements);
        response.put("stats", stats);
        
        return response;
    }
}
