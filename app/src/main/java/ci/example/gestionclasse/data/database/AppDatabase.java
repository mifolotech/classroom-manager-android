package ci.example.gestionclasse.data.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ci.example.gestionclasse.data.dao.ClasseDao;
import ci.example.gestionclasse.data.dao.EtudiantDao;
import ci.example.gestionclasse.data.dao.MessageDao;
import ci.example.gestionclasse.data.entity.Classe;
import ci.example.gestionclasse.data.entity.Etudiant;
import ci.example.gestionclasse.data.entity.Message;

// @Database : déclare la base. entities = les 3 tables. version = 1 : première version du schéma.
// exportSchema = false : on ne génère pas de fichier de schéma (inutile pour ce projet).
@Database(
        entities = {Classe.class, Etudiant.class, Message.class},
        version = 1,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    // Les trois "portes d'accès" : Room écrit le code de ces méthodes à notre place.
    public abstract ClasseDao classeDao();

    public abstract EtudiantDao etudiantDao();

    public abstract MessageDao messageDao();

    // Une seule instance de la base pour toute l'application (volatile : visible par tous les threads).
    private static volatile AppDatabase instance;

    // Équipe de threads d'arrière-plan : les ÉCRITURES s'y exécutent pour ne pas bloquer l'écran.
    public static final ExecutorService ecritureExecutor = Executors.newFixedThreadPool(4);

    // Renvoie la base, en la créant au premier appel (modèle "singleton").
    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            // synchronized : évite que deux threads créent la base en même temps.
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            "gestion_classe.db"
                    ).build();
                }
            }
        }
        return instance;
    }
}