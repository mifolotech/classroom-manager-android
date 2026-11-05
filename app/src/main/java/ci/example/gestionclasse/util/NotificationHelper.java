package ci.example.gestionclasse.util;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import java.util.Objects;

import ci.example.gestionclasse.R;
import ci.example.gestionclasse.data.database.AppDatabase;
import ci.example.gestionclasse.data.entity.Etudiant;
import ci.example.gestionclasse.data.entity.Message;
import ci.example.gestionclasse.ui.AccueilActivity;
import ci.example.gestionclasse.ui.ConversationActivity;
import ci.example.gestionclasse.ui.RoleActivity;

// Construit et affiche la notification d'un nouveau message.
public final class NotificationHelper {

    // Identifiant du canal : tous les messages passent par ce canal.
    private static final String CANAL_ID = "messages";

    private NotificationHelper() {
    }

    // Crée le canal (Android 8 et plus). On peut l'appeler plusieurs fois : si le canal existe déjà,
    // le système ne fait rien.
    private static void creerCanal(Context contexte) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel canal = new NotificationChannel(
                    CANAL_ID,
                    contexte.getString(R.string.notif_canal_nom),
                    NotificationManager.IMPORTANCE_HIGH);   // HIGH : la notification s'affiche en haut de l'écran
            canal.setDescription(contexte.getString(R.string.notif_canal_description));

            NotificationManager gestionnaire = contexte.getSystemService(NotificationManager.class);
            gestionnaire.createNotificationChannel(canal);
        }
    }

    // À appeler dans un thread D'ARRIÈRE-PLAN : la méthode lit la base pour trouver le nom de l'expéditeur.
    public static void notifierNouveauMessage(Context contexte, Message message) {

        // 1. PERMISSION : sur Android 13 et plus, sans l'autorisation de l'utilisateur, on n'affiche rien.
        // Le message est quand même enregistré : seule la notification est ignorée.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(contexte, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        // L'utilisateur peut aussi avoir désactivé les notifications de l'application dans les paramètres.
        NotificationManagerCompat gestionnaire = NotificationManagerCompat.from(contexte);
        if (!gestionnaire.areNotificationsEnabled()) {
            return;
        }

        // 2. L'expéditeur : on lit son nom dans la base (lecture immédiate, sans LiveData).
        Etudiant expediteur = AppDatabase.getInstance(contexte)
                .etudiantDao().trouverParId(message.getExpediteurId());
        if (expediteur == null) {
            return;
        }
        String nomExpediteur = contexte.getString(
                R.string.nom_complet, expediteur.getNom(), expediteur.getPrenom());

        // 3. Le texte : le message, ou "Pièce jointe" s'il n'y a que le fichier.
        String texte = message.getContenu();
        if (texte == null || texte.isEmpty()) {
            texte = contexte.getString(R.string.notif_piece_jointe);
        }

        creerCanal(contexte);

        // 4. UN CODE UNIQUE PAR CONVERSATION. Le système compare les PendingIntent sans regarder les extras :
        // avec le même code, deux conversations différentes se confondraient.
        // Ce même code sert d'identifiant à la notification : un nouveau message de la même personne
        // MET À JOUR la notification existante au lieu d'en empiler une autre.
        int code = Objects.hash(message.getDestinataireId(), message.getExpediteurId());

        // 5. L'action au toucher : ouvrir la conversation, vue par le DESTINATAIRE.
        // L'application peut être fermée : l'Intent transporte donc tout ce qu'il faut savoir.
        Intent racine = new Intent(contexte, RoleActivity.class);

        Intent accueil = new Intent(contexte, AccueilActivity.class);
        accueil.putExtra(Constantes.EXTRA_ROLE, Constantes.ROLE_ETUDIANT);
        accueil.putExtra(Constantes.EXTRA_PROFIL_ID, message.getDestinataireId());

        Intent conversation = new Intent(contexte, ConversationActivity.class);
        conversation.putExtra(Constantes.EXTRA_ROLE, Constantes.ROLE_ETUDIANT);
        conversation.putExtra(Constantes.EXTRA_PROFIL_ID, message.getDestinataireId());
        conversation.putExtra(Constantes.EXTRA_INTERLOCUTEUR_ID, message.getExpediteurId());

        // TaskStackBuilder : construit la PILE DE RETOUR (Rôle, puis Accueil, puis Conversation).
        // Ainsi, le bouton Retour mène à l'accueil du destinataire au lieu de quitter l'application.
        // FLAG_IMMUTABLE : aucune autre application ne peut modifier cet Intent (obligatoire sur Android 12+).
        // FLAG_UPDATE_CURRENT : si le même PendingIntent existe, on met à jour ses extras.
        PendingIntent action = TaskStackBuilder.create(contexte)
                .addNextIntent(racine)
                .addNextIntent(accueil)
                .addNextIntent(conversation)
                .getPendingIntent(code, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        // 6. La notification elle-même.
        NotificationCompat.Builder notification = new NotificationCompat.Builder(contexte, CANAL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(nomExpediteur)                       // le nom de l'expéditeur
                .setContentText(texte)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(texte)) // texte long déplié
                .setColor(ContextCompat.getColor(contexte, R.color.primaire))
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setPriority(NotificationCompat.PRIORITY_HIGH)        // pour Android 7 et moins (pas de canal)
                .setAutoCancel(true)                                  // elle disparaît quand on la touche
                .setContentIntent(action);

        // 7. Affichage.
        gestionnaire.notify(code, notification.build());
    }
}