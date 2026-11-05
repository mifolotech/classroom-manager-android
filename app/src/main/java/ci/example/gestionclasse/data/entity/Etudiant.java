package ci.example.gestionclasse.data.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

// Table "etudiant" : une ligne par étudiant inscrit dans la classe.
@Entity(tableName = "etudiant")
public class Etudiant {

    // autoGenerate = true : Room donne tout seul un numéro unique à chaque nouvel étudiant (1, 2, 3...).
    @PrimaryKey(autoGenerate = true)
    private long id;

    private String nom;
    private String prenom;
    private String email;
    private String telephone;

    // Constructeur vide utilisé par Room.
    public Etudiant() {
    }

    // Constructeur pratique : l'id n'est pas fourni, Room le génère à l'insertion.
    @Ignore
    public Etudiant(String nom, String prenom, String email, String telephone) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getPrenom() { return prenom; }
    public void setPrenom(String prenom) { this.prenom = prenom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelephone() { return telephone; }
    public void setTelephone(String telephone) { this.telephone = telephone; }
}