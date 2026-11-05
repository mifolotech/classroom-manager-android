package ci.example.gestionclasse.util;

import android.content.Context;

import androidx.appcompat.app.AppCompatDelegate;

// Gère le choix d'apparence de l'application : automatique (réglage du téléphone), clair ou sombre.
// Le choix est mémorisé dans les SharedPreferences : un petit fichier de réglages privé à l'application.
public final class ThemeHelper {

    // Nom du fichier de réglages et clé du mode choisi.
    private static final String FICHIER = "reglages";
    private static final String CLE_MODE = "mode_nuit";

    private ThemeHelper() {
    }

    // Lit le mode mémorisé. Par défaut : automatique (le téléphone décide).
    public static int getMode(Context contexte) {
        return contexte.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)
                .getInt(CLE_MODE, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
    }

    // Applique le mode mémorisé. Appelée une seule fois, au démarrage de l'application.
    public static void appliquerSauvegarde(Context contexte) {
        AppCompatDelegate.setDefaultNightMode(getMode(contexte));
    }

    // Mémorise le nouveau choix, puis l'applique tout de suite.
    // setDefaultNightMode recrée automatiquement les écrans ouverts avec les bonnes couleurs.
    public static void choisir(Context contexte, int mode) {
        contexte.getSharedPreferences(FICHIER, Context.MODE_PRIVATE)
                .edit()
                .putInt(CLE_MODE, mode)
                .apply();     // apply : enregistre en arrière-plan, sans bloquer l'écran
        AppCompatDelegate.setDefaultNightMode(mode);
    }
}