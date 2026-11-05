package ci.example.gestionclasse.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

// @Entity : cette classe devient une TABLE nommée "classe" dans la base.
// Chaque objet Classe correspond à UNE LIGNE de cette table.
@Entity(tableName = "classe")
public class Classe {

    // L'application ne gère qu'UNE classe : sa ligne a toujours cet identifiant.
    public static final long ID_UNIQUE = 1;

    // @PrimaryKey : l'identifiant unique de la ligne.
    @PrimaryKey
    private long id;

    private String intitule;
    private String description;
    private String anneeUniversitaire;

    // Constructeur vide : Room s'en sert pour reconstruire un objet depuis une ligne de la table.
    public Classe() {
    }

    // Constructeur pratique pour NOUS. @Ignore : Room ne doit pas l'utiliser.
    @Ignore
    public Classe(String intitule, String description, String anneeUniversitaire) {
        this.id = ID_UNIQUE;
        this.intitule = intitule;
        this.description = description;
        this.anneeUniversitaire = anneeUniversitaire;
    }

    // Getters et setters : Room les utilise pour lire et remplir les champs privés.
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getIntitule() { return intitule; }
    public void setIntitule(String intitule) { this.intitule = intitule; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getAnneeUniversitaire() { return anneeUniversitaire; }
    public void setAnneeUniversitaire(String anneeUniversitaire) {
        this.anneeUniversitaire = anneeUniversitaire;
    }
}