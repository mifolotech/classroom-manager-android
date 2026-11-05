package ci.example.gestionclasse.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import java.util.List;

import ci.example.gestionclasse.data.dao.ClasseDao;
import ci.example.gestionclasse.data.dao.EtudiantDao;
import ci.example.gestionclasse.data.dao.MessageDao;
import ci.example.gestionclasse.data.database.AppDatabase;
import ci.example.gestionclasse.data.entity.Classe;
import ci.example.gestionclasse.data.entity.Etudiant;
import ci.example.gestionclasse.data.entity.Message;
import ci.example.gestionclasse.util.NotificationHelper;

// Le REPOSITORY : l'unique porte d'entrée vers les données.
public class GestionRepository {

    // Étiquette qui identifie les messages de cette classe dans le Logcat.
    private static final String TAG = "GestionRepository";

    private final Application application;
    private final ClasseDao classeDao;
    private final EtudiantDao etudiantDao;
    private final MessageDao messageDao;

    public GestionRepository(Application application) {
        this.application = application;
        AppDatabase base = AppDatabase.getInstance(application);
        classeDao = base.classeDao();
        etudiantDao = base.etudiantDao();
        messageDao = base.messageDao();
    }

    // Exécute un travail d'écriture en ARRIÈRE-PLAN, protégé par un try/catch.
    // Sans cette protection, une exception dans ce thread arrêterait toute l'application.
    // Runnable : un bloc de code que l'on peut passer en paramètre.
    private void ecrire(Runnable travail) {
        AppDatabase.ecritureExecutor.execute(() -> {
            try {
                travail.run();
            } catch (Exception e) {
                Log.e(TAG, "Erreur pendant une écriture dans la base", e);
            }
        });
    }

    // ===== CLASSE =====

    public LiveData<Classe> observerClasse() {
        return classeDao.observerClasse();
    }

    public void enregistrerClasse(Classe classe) {
        ecrire(() -> classeDao.enregistrer(classe));
    }

    // ===== ÉTUDIANTS =====

    public LiveData<List<Etudiant>> observerEtudiants() {
        return etudiantDao.observerTous();
    }

    public LiveData<Etudiant> observerEtudiant(long id) {
        return etudiantDao.observerParId(id);
    }

    public void ajouterEtudiant(Etudiant etudiant) {
        ecrire(() -> etudiantDao.inserer(etudiant));
    }

    public void modifierEtudiant(Etudiant etudiant) {
        ecrire(() -> etudiantDao.modifier(etudiant));
    }

    public void supprimerEtudiant(Etudiant etudiant) {
        ecrire(() -> etudiantDao.supprimer(etudiant));
    }

    // ===== MESSAGES =====

    public LiveData<List<Message>> observerConversation(long a, long b) {
        return messageDao.observerConversation(a, b);
    }

    // Enregistre le message, puis notifie le destinataire (dans le même thread d'arrière-plan).
    public void envoyerMessage(Message message) {
        ecrire(() -> {
            messageDao.inserer(message);
            NotificationHelper.notifierNouveauMessage(application, message);
        });
    }
    // NOUVEAU (E1) : supprime un message, en arrière-plan et avec la protection try/catch.
    public void supprimerMessage(Message message) {
        ecrire(() -> messageDao.supprimer(message));
    }
}