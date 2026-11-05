package ci.example.gestionclasse.util;

import android.util.Patterns;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Regroupe les règles de validation, pour ne pas les écrire dans chaque écran.
public final class Validateur {

    // Année universitaire : 4 chiffres, un tiret, 4 chiffres.
    private static final Pattern ANNEE = Pattern.compile("^(\\d{4})-(\\d{4})$");

    // Téléphone : un "+" facultatif au début, puis 8 à 15 chiffres.
    private static final Pattern TELEPHONE = Pattern.compile("^\\+?\\d{8,15}$");

    private Validateur() {
    }

    // Vrai si le texte est de la forme 2026-2027 : deux années CONSÉCUTIVES.
    public static boolean estAnneeUniversitaireValide(String texte) {
        Matcher m = ANNEE.matcher(texte);
        if (!m.matches()) {
            return false;
        }
        int debut = Integer.parseInt(m.group(1));
        int fin = Integer.parseInt(m.group(2));
        return fin == debut + 1;
    }

    // Vrai si l'email a un format correct. Patterns.EMAIL_ADDRESS est le validateur fourni par Android.
    public static boolean estEmailValide(String texte) {
        return Patterns.EMAIL_ADDRESS.matcher(texte).matches();
    }

    // Retire les espaces d'un numéro : "07 07 07 07 07" devient "0707070707".
    public static String normaliserTelephone(String texte) {
        return texte.replace(" ", "");
    }

    // Vrai si le numéro (espaces ignorés) respecte le modèle ci-dessus.
    public static boolean estTelephoneValide(String texte) {
        return TELEPHONE.matcher(normaliserTelephone(texte)).matches();
    }
}