package ci.example.gestionclasse.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import ci.example.gestionclasse.data.entity.Classe;

// @Dao : cette interface liste les opérations autorisées sur la table "classe".
// On n'écrit PAS le code : Room le génère à partir des annotations.
@Dao
public interface ClasseDao {

    // Lit la classe. LiveData : l'écran est averti automatiquement dès que la table change.
    // Si aucune classe n'existe encore, la valeur est null (c'est ce qui permettra de proposer la création).
    @Query("SELECT * FROM classe WHERE id = 1 LIMIT 1")
    LiveData<Classe> observerClasse();

    // Sert à la CRÉATION et à la MODIFICATION : la ligne a toujours l'id 1.
    // REPLACE : si la ligne existe déjà, elle est remplacée par la nouvelle version.
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void enregistrer(Classe classe);
}