package ci.example.gestionclasse.util;

import android.content.Context;

import androidx.core.content.ContextCompat;

import java.util.Locale;

import ci.example.gestionclasse.R;
import ci.example.gestionclasse.data.entity.Etudiant;

// Outils pour l'avatar d'un étudiant : ses initiales et sa couleur.
// La liste et la fiche utilisent les mêmes règles, donc un étudiant a le même avatar partout.
public final class Avatar {

    // Les quatre couleurs de la palette, utilisées à tour de rôle.
    private static final int[] COULEURS = {
            R.color.primaire,
            R.color.accent_orange,
            R.color.accent_vert,
            R.color.accent_rouge
    };

    private Avatar() {
    }

    // Initiales : première lettre du prénom, puis première lettre du nom ("Awa Koné" donne "AK").
    public static String initiales(Etudiant etudiant) {
        String initiales = (premiereLettre(etudiant.getPrenom())
                + premiereLettre(etudiant.getNom())).toUpperCase(Locale.getDefault());
        return initiales.isEmpty() ? "?" : initiales;
    }

    // Couleur de l'avatar, choisie avec l'identifiant de l'étudiant (et non sa position dans la liste) :
    // la couleur d'un étudiant ne change donc jamais, même quand la liste est triée ou filtrée.
    // Les couleurs viennent de colors.xml : elles s'adaptent au mode sombre.
    public static int couleur(Context contexte, Etudiant etudiant) {
        int indice = (int) (Math.abs(etudiant.getId()) % COULEURS.length);
        return ContextCompat.getColor(contexte, COULEURS[indice]);
    }

    // Première lettre d'un texte, ou "" s'il est vide.
    // offsetByCodePoints : prend un "caractère complet", même s'il est composé de plusieurs unités.
    private static String premiereLettre(String texte) {
        if (texte == null) {
            return "";
        }
        String propre = texte.trim();
        if (propre.isEmpty()) {
            return "";
        }
        return propre.substring(0, propre.offsetByCodePoints(0, 1));
    }
}