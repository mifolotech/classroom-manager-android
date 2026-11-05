package ci.example.gestionclasse;

import android.app.Application;

import ci.example.gestionclasse.util.ThemeHelper;

// La classe Application : Android la crée AVANT tous les écrans, une seule fois au démarrage du processus.
// C'est l'endroit idéal pour appliquer l'apparence choisie : même une notification qui ouvre directement
// une conversation, application fermée, aura les bonnes couleurs.
public class GestionClasseApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        ThemeHelper.appliquerSauvegarde(this);
    }
}