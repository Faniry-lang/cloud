package itu.cloud.service;

import itu.cloud.config.AuthProperties;
import itu.cloud.config.AuthProperties.AppMode;
import org.springframework.stereotype.Service;


@Service
public class ConnectivityService {

    private final AuthProperties authProperties;
    private final FirestoreService firestoreService;

    private Boolean cachedOnlineStatus = null;
    private long lastCheck = 0;
    private static final long CACHE_DURATION_MS = 30000; // 30 secondes

    public ConnectivityService(AuthProperties authProperties, FirestoreService firestoreService) {
        this.authProperties = authProperties;
        this.firestoreService = firestoreService;
    }

    public boolean isOnline() {
        AppMode mode = authProperties.getMode();

        switch (mode) {
            case ONLINE:
                return true;
            case OFFLINE:
                return false;
            case AUTO:
            default:
                return checkFirebaseConnectivity();
        }
    }

    /**
     * Force le rafraichissement du cache de connectivite
     */
    public void refreshConnectivity() {
        cachedOnlineStatus = null;
        lastCheck = 0;
    }

    /**
     * Verifie la connectivite Firebase avec cache
     */
    private boolean checkFirebaseConnectivity() {
        long now = System.currentTimeMillis();

        // Utiliser le cache si valide
        if (cachedOnlineStatus != null && (now - lastCheck) < CACHE_DURATION_MS) {
            return cachedOnlineStatus;
        }

        // Verifier la connectivite
        try {
            cachedOnlineStatus = firestoreService.isAvailable();
        } catch (Exception e) {
            cachedOnlineStatus = false;
        }
        lastCheck = now;

        return cachedOnlineStatus;
    }

    /**
     * Retourne le mode actuel configure
     */
    public String getCurrentMode() {
        return authProperties.getMode().name();
    }

    /**
     * Retourne le statut effectif (online/offline)
     */
    public String getEffectiveStatus() {
        return isOnline() ? "ONLINE" : "OFFLINE";
    }
}

