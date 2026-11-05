package ci.example.gestionclasse.util;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import java.io.InputStream;

import ci.example.gestionclasse.R;
import ci.example.gestionclasse.data.entity.Message;

// Outils pour les pièces jointes, partagés par l'écran et l'adapter.
public final class PiecesJointes {

    private PiecesJointes() {
    }

    // Déduit le type (photo, vidéo ou document) à partir du type MIME du fichier.
    // Un type MIME ressemble à "image/jpeg" ou "video/mp4" : avant la barre, la grande famille.
    public static String typeDepuisMime(String mime) {
        if (mime != null) {
            if (mime.startsWith("image/")) {
                return Message.TYPE_PHOTO;
            }
            if (mime.startsWith("video/")) {
                return Message.TYPE_VIDEO;
            }
        }
        return Message.TYPE_DOCUMENT;
    }

    // Lit le nom du fichier auprès du système (colonne DISPLAY_NAME).
    public static String nomDuFichier(ContentResolver resolver, Uri uri) {
        // try avec ressource : le Cursor est fermé automatiquement à la fin du bloc.
        try (Cursor curseur = resolver.query(
                uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (curseur != null && curseur.moveToFirst()) {
                int colonne = curseur.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (colonne >= 0) {
                    String nom = curseur.getString(colonne);
                    if (nom != null) {
                        return nom;
                    }
                }
            }
        } catch (Exception e) {
            // Lecture impossible : on utilise le plan B ci-dessous.
        }
        String segment = uri.getLastPathSegment();
        return segment != null ? segment : "fichier";
    }

    // Texte affiché : une icône selon le type, puis le nom (modèles pj_photo, pj_video, pj_document).
    public static String libelle(Context contexte, String type, String nom) {
        int format = R.string.pj_document;
        if (Message.TYPE_PHOTO.equals(type)) {
            format = R.string.pj_photo;
        } else if (Message.TYPE_VIDEO.equals(type)) {
            format = R.string.pj_video;
        }
        return contexte.getString(format, nom);
    }

    // Vrai si le fichier est encore accessible (non supprimé, accès toujours autorisé).
    // On essaie simplement de l'ouvrir en lecture.
    public static boolean estLisible(ContentResolver resolver, Uri uri) {
        try (InputStream flux = resolver.openInputStream(uri)) {
            return flux != null;
        } catch (Exception e) {
            return false;
        }
    }
}