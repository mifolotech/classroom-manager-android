package ci.example.gestionclasse.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import ci.example.gestionclasse.data.entity.Etudiant;

@Dao
public interface EtudiantDao {

    // Ajoute un étudiant et renvoie l'id que Room lui a attribué.
    @Insert
    long inserer(Etudiant etudiant);

    // Modifie la ligne qui a le même id que l'étudiant fourni.
    @Update
    void modifier(Etudiant etudiant);

    // Supprime la ligne de cet étudiant (et, par cascade, ses messages).
    @Delete
    void supprimer(Etudiant etudiant);

    // Tous les étudiants, triés par nom puis prénom. LiveData : la liste se met à jour toute seule.
    @Query("SELECT * FROM etudiant ORDER BY nom, prenom")
    LiveData<List<Etudiant>> observerTous();

    // Un étudiant précis, observé en direct (pour l'écran de détail).
    @Query("SELECT * FROM etudiant WHERE id = :id")
    LiveData<Etudiant> observerParId(long id);

    // Lecture IMMÉDIATE (sans LiveData) : à utiliser uniquement dans un thread d'arrière-plan.
    // Servira par exemple à retrouver le nom de l'expéditeur pour la notification.
    @Query("SELECT * FROM etudiant WHERE id = :id")
    Etudiant trouverParId(long id);
}