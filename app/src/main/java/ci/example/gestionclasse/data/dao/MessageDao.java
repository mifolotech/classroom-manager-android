package ci.example.gestionclasse.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import ci.example.gestionclasse.data.entity.Message;

@Dao
public interface MessageDao {

    // Enregistre un message et renvoie son id.
    @Insert
    long inserer(Message message);

    // NOUVEAU (E1) : supprime le message. Room repère la ligne grâce à la clé primaire (l'id du message).
    @Delete
    void supprimer(Message message);

    // La CONVERSATION entre deux étudiants a et b : les messages envoyés de a vers b
    // ET ceux envoyés de b vers a, du plus ancien au plus récent.
    @Query("SELECT * FROM message "
            + "WHERE (expediteurId = :a AND destinataireId = :b) "
            + "OR (expediteurId = :b AND destinataireId = :a) "
            + "ORDER BY dateEnvoi ASC, id ASC")
    LiveData<List<Message>> observerConversation(long a, long b);
}