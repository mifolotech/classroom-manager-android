package ci.example.gestionclasse.util;

// Valeurs partagées entre plusieurs écrans. Les centraliser évite les fautes de frappe.
public final class Constantes {

    // ===== Clés des "extras" qui voyagent dans les Intents =====
    public static final String EXTRA_ROLE = "ci.example.gestionclasse.ROLE";
    public static final String EXTRA_ETUDIANT_ID = "ci.example.gestionclasse.ETUDIANT_ID";
    // NOUVEAU : l'identifiant de l'étudiant "connecté" (le profil courant).
    public static final String EXTRA_PROFIL_ID = "ci.example.gestionclasse.PROFIL_ID";
    // NOUVEAU : le mode d'affichage de la liste des étudiants.
    public static final String EXTRA_MODE_LISTE = "ci.example.gestionclasse.MODE_LISTE";

    // NOUVEAU : l'étudiant avec qui on discute (l'autre côté de la conversation).
    public static final String EXTRA_INTERLOCUTEUR_ID = "ci.example.gestionclasse.INTERLOCUTEUR_ID";

    // Valeur signifiant "aucun étudiant" (un identifiant réel n'est jamais -1).
    public static final long ID_AUCUN = -1;

    // ===== Les deux rôles =====
    public static final String ROLE_ENSEIGNANT = "ENSEIGNANT";
    public static final String ROLE_ETUDIANT = "ETUDIANT";

    // ===== Les trois modes de la liste des étudiants =====
    public static final String MODE_VOIR = "VOIR";
    public static final String MODE_CHOISIR_PROFIL = "CHOISIR_PROFIL";
    public static final String MODE_CHOISIR_DESTINATAIRE = "CHOISIR_DESTINATAIRE";

    private Constantes() {
    }
}