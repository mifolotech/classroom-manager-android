package ci.example.gestionclasse.data.entity;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

// Table "message". Les deux CLÉS ÉTRANGÈRES relient un message à l'étudiant qui l'envoie
// et à celui qui le reçoit.
// onDelete = CASCADE : si un étudiant est supprimé, ses messages le sont aussi (décision validée).
// indices : accélèrent les recherches par expéditeur et destinataire.
@Entity(
        tableName = "message",
        foreignKeys = {
                @ForeignKey(entity = Etudiant.class,
                        parentColumns = "id",
                        childColumns = "expediteurId",
                        onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Etudiant.class,
                        parentColumns = "id",
                        childColumns = "destinataireId",
                        onDelete = ForeignKey.CASCADE)
        },
        indices = {@Index("expediteurId"), @Index("destinataireId")}
)
public class Message {

    // Les trois types de pièce jointe possibles.
    public static final String TYPE_PHOTO = "PHOTO";
    public static final String TYPE_VIDEO = "VIDEO";
    public static final String TYPE_DOCUMENT = "DOCUMENT";

    @PrimaryKey(autoGenerate = true)
    private long id;

    private long expediteurId;     // l'id de l'étudiant qui envoie
    private long destinataireId;   // l'id de l'étudiant qui reçoit
    private String contenu;        // le texte du message
    private long dateEnvoi;        // date et heure en millisecondes (System.currentTimeMillis())

    // Pièce jointe : facultative, donc ces trois champs peuvent valoir null.
    @Nullable
    private String pieceJointeUri;   // adresse locale du fichier choisi
    @Nullable
    private String pieceJointeType;  // TYPE_PHOTO, TYPE_VIDEO ou TYPE_DOCUMENT
    @Nullable
    private String pieceJointeNom;   // nom du fichier, pour l'affichage

    public Message() {
    }

    // Constructeur pratique : message sans pièce jointe (on les ajoute ensuite avec les setters).
    @Ignore
    public Message(long expediteurId, long destinataireId, String contenu, long dateEnvoi) {
        this.expediteurId = expediteurId;
        this.destinataireId = destinataireId;
        this.contenu = contenu;
        this.dateEnvoi = dateEnvoi;
    }

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public long getExpediteurId() { return expediteurId; }
    public void setExpediteurId(long expediteurId) { this.expediteurId = expediteurId; }

    public long getDestinataireId() { return destinataireId; }
    public void setDestinataireId(long destinataireId) { this.destinataireId = destinataireId; }

    public String getContenu() { return contenu; }
    public void setContenu(String contenu) { this.contenu = contenu; }

    public long getDateEnvoi() { return dateEnvoi; }
    public void setDateEnvoi(long dateEnvoi) { this.dateEnvoi = dateEnvoi; }

    @Nullable
    public String getPieceJointeUri() { return pieceJointeUri; }
    public void setPieceJointeUri(@Nullable String pieceJointeUri) {
        this.pieceJointeUri = pieceJointeUri;
    }

    @Nullable
    public String getPieceJointeType() { return pieceJointeType; }
    public void setPieceJointeType(@Nullable String pieceJointeType) {
        this.pieceJointeType = pieceJointeType;
    }

    @Nullable
    public String getPieceJointeNom() { return pieceJointeNom; }
    public void setPieceJointeNom(@Nullable String pieceJointeNom) {
        this.pieceJointeNom = pieceJointeNom;
    }
}