package ci.example.gestionclasse.ui;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.List;

import ci.example.gestionclasse.R;
import ci.example.gestionclasse.data.entity.Etudiant;
import ci.example.gestionclasse.data.entity.Message;
import ci.example.gestionclasse.databinding.ActivityConversationBinding;
import ci.example.gestionclasse.util.Avatar;
import ci.example.gestionclasse.util.Constantes;
import ci.example.gestionclasse.util.PiecesJointes;
import ci.example.gestionclasse.viewmodel.EtudiantViewModel;
import ci.example.gestionclasse.viewmodel.MessageViewModel;

// Écran de conversation entre l'étudiant connecté et un autre étudiant,
// avec pièces jointes, notifications et menu sur les messages.
public class ConversationActivity extends AppCompatActivity {

    // Types de fichiers proposés par le sélecteur : photos, vidéos, et documents (pdf, word, texte...).
    private static final String[] TYPES_ACCEPTES = {"image/*", "video/*", "application/*", "text/*"};

    private ActivityConversationBinding binding;
    private MessageViewModel messageViewModel;
    private MessageAdapter adapter;

    private long profilId;
    private long interlocuteurId;

    // Le sélecteur de fichiers. Il doit être enregistré au démarrage de l'écran (dans onCreate).
    private ActivityResultLauncher<String[]> selecteurFichier;

    // La demande de permission de notification (réponse : accordée ou refusée).
    private ActivityResultLauncher<String> demandeNotifications;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityConversationBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // PROTECTIONS : seul un étudiant, avec deux identifiants valides et différents, peut ouvrir cet écran.
        String role = getIntent().getStringExtra(Constantes.EXTRA_ROLE);
        profilId = getIntent().getLongExtra(Constantes.EXTRA_PROFIL_ID, Constantes.ID_AUCUN);
        interlocuteurId = getIntent().getLongExtra(
                Constantes.EXTRA_INTERLOCUTEUR_ID, Constantes.ID_AUCUN);

        if (!Constantes.ROLE_ETUDIANT.equals(role)
                || profilId == Constantes.ID_AUCUN
                || interlocuteurId == Constantes.ID_AUCUN
                || profilId == interlocuteurId) {
            finish();
            return;
        }

        messageViewModel = new ViewModelProvider(this).get(MessageViewModel.class);

        // Enregistre le sélecteur. OpenDocument : l'utilisateur choisit UN fichier parmi ceux de son téléphone.
        // Le résultat (l'adresse du fichier, ou null si annulé) arrive dans surFichierChoisi.
        selecteurFichier = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(), this::surFichierChoisi);

        // Enregistre la demande de permission. Si elle est refusée, l'application continue de fonctionner :
        // on explique seulement que les alertes ne s'afficheront pas.
        demandeNotifications = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(), accordee -> {
                    if (!accordee) {
                        Toast.makeText(this, R.string.notif_refusee, Toast.LENGTH_LONG).show();
                    }
                });

        // Liste des messages : on passe aussi la méthode appelée au clic sur une pièce jointe.
        adapter = new MessageAdapter(profilId, this::ouvrirPieceJointe);
        // Menu du long appui sur un message (copier, supprimer).
        adapter.setOnMessageLongClickListener(this::afficherMenuMessage);

        LinearLayoutManager disposition = new LinearLayoutManager(this);
        disposition.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(disposition);
        binding.rvMessages.setAdapter(adapter);

        // Ces boutons sont maintenant des cartes rondes : setOnClickListener fonctionne sur toute vue.
        binding.btnRetour.setOnClickListener(v -> finish());
        binding.btnEnvoyer.setOnClickListener(v -> envoyer());
        binding.btnJoindre.setOnClickListener(v -> selecteurFichier.launch(TYPES_ACCEPTES));
        binding.btnRetirerPiece.setOnClickListener(v -> {
            messageViewModel.effacerPieceJointe();
            afficherPieceChoisie();
        });

        EtudiantViewModel etudiantViewModel =
                new ViewModelProvider(this).get(EtudiantViewModel.class);
        etudiantViewModel.getEtudiant(interlocuteurId).observe(this, this::afficherInterlocuteur);
        etudiantViewModel.getEtudiant(profilId).observe(this, etudiant -> {
            if (etudiant == null) {
                finish();
            }
        });

        messageViewModel.getConversation(profilId, interlocuteurId)
                .observe(this, this::afficherMessages);

        // Après une rotation, la pièce choisie est encore dans le ViewModel : on la réaffiche.
        afficherPieceChoisie();

        // On ne demande pas la permission à chaque rotation de l'écran, seulement à l'ouverture.
        if (savedInstanceState == null) {
            verifierPermissionNotifications();
        }
    }

    // ===== Menu sur un message (appui long) =====

    // Appelée quand on reste appuyé sur un message. On construit le menu selon ce qui est possible.
    private void afficherMenuMessage(Message message) {
        String contenu = message.getContenu();
        boolean peutCopier = contenu != null && !contenu.isEmpty();
        // On ne peut supprimer que SES PROPRES messages.
        boolean peutSupprimer = message.getExpediteurId() == profilId;
        // Rien à proposer (par exemple un message reçu qui ne contient qu'un fichier) : pas de menu.
        // NOUVEAU (11.6) : un court message explique pourquoi, au lieu de ne rien faire.
        if (!peutCopier && !peutSupprimer) {
            Toast.makeText(this, R.string.msg_aucune_action, Toast.LENGTH_SHORT).show();
            return;
        }

        // Deux listes parallèles : le texte de chaque choix, et l'action qui lui correspond.
        // Runnable : un bloc de code que l'on peut ranger dans une liste et exécuter plus tard.
        List<String> libelles = new ArrayList<>();
        List<Runnable> actions = new ArrayList<>();

        if (peutCopier) {
            libelles.add(getString(R.string.menu_copier));
            actions.add(() -> copierTexte(contenu));
        }
        if (peutSupprimer) {
            libelles.add(getString(R.string.action_supprimer));
            actions.add(() -> demanderConfirmationSuppression(message));
        }

        // setItems : une boîte de dialogue qui affiche une liste de choix. "which" = le numéro du choix touché.
        new AlertDialog.Builder(this)
                .setItems(libelles.toArray(new String[0]),
                        (dialog, which) -> actions.get(which).run())
                .show();
    }

    // Copie le texte dans le presse-papiers du téléphone.
    private void copierTexte(String texte) {
        ClipboardManager presse = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (presse == null) {
            return;
        }
        presse.setPrimaryClip(ClipData.newPlainText("message", texte));

        // Depuis Android 13, le système affiche lui-même une confirmation : notre message serait en double.
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            Toast.makeText(this, R.string.msg_texte_copie, Toast.LENGTH_SHORT).show();
        }
    }

    // Une suppression est irréversible : on demande confirmation avant.
    private void demanderConfirmationSuppression(Message message) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_msg_supprimer_titre)
                .setMessage(R.string.dialog_msg_supprimer_texte)
                .setPositiveButton(R.string.action_supprimer, (dialog, which) -> {
                    // DOUBLE PROTECTION : même si le menu est atteint autrement, on ne supprime
                    // que les messages dont on est l'expéditeur.
                    if (message.getExpediteurId() != profilId) {
                        return;
                    }
                    // La conversation est OBSERVÉE : le message disparaît tout seul de l'écran.
                    messageViewModel.supprimer(message);
                    Toast.makeText(this, R.string.msg_message_supprime, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.action_annuler, null)
                .show();
    }

    // ===== Permission de notification =====

    // Sur Android 13 et plus, l'utilisateur doit autoriser les notifications.
    private void verifierPermissionNotifications() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            return;     // avant Android 13, aucune demande n'est nécessaire
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            return;     // déjà autorisée
        }

        // true quand l'utilisateur a DÉJÀ refusé une fois : on lui explique avant de redemander.
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.notif_dialog_titre)
                    .setMessage(R.string.notif_dialog_message)
                    .setPositiveButton(R.string.action_autoriser,
                            (dialog, which) -> demandeNotifications.launch(
                                    Manifest.permission.POST_NOTIFICATIONS))
                    .setNegativeButton(R.string.action_plus_tard, null)
                    .show();
        } else {
            // Première demande : la fenêtre du système s'affiche directement.
            demandeNotifications.launch(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    // ===== Choix d'un fichier =====

    // Appelée quand l'utilisateur a choisi un fichier (uri) ou fermé le sélecteur (null).
    private void surFichierChoisi(Uri uri) {
        if (uri == null) {
            return;
        }

        // GARDER L'ACCÈS : sans cet appel, l'autorisation donnée par le sélecteur disparaît
        // à l'arrêt de l'application ou au redémarrage du téléphone.
        try {
            getContentResolver().takePersistableUriPermission(
                    uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        } catch (SecurityException e) {
            // Ce fournisseur de fichiers n'autorise pas l'accès durable : l'accès reste temporaire.
        }

        String type = PiecesJointes.typeDepuisMime(getContentResolver().getType(uri));
        String nom = PiecesJointes.nomDuFichier(getContentResolver(), uri);

        messageViewModel.choisirPieceJointe(uri.toString(), type, nom);
        afficherPieceChoisie();
    }

    // Affiche ou cache la ligne "pièce choisie" selon l'état du ViewModel.
    private void afficherPieceChoisie() {
        if (messageViewModel.aPieceJointe()) {
            binding.layoutPieceChoisie.setVisibility(View.VISIBLE);
            binding.tvPieceChoisie.setText(PiecesJointes.libelle(
                    this, messageViewModel.getPieceType(), messageViewModel.getPieceNom()));
        } else {
            binding.layoutPieceChoisie.setVisibility(View.GONE);
        }
    }

    // ===== Ouverture d'une pièce jointe =====

    // Appelée quand on touche la pièce jointe d'un message (le lien ou la miniature).
    private void ouvrirPieceJointe(Message message) {
        String adresse = message.getPieceJointeUri();
        if (adresse == null) {
            return;
        }
        Uri uri = Uri.parse(adresse);

        // Le fichier a-t-il été supprimé ou son accès perdu ? On vérifie AVANT d'essayer de l'ouvrir.
        if (!PiecesJointes.estLisible(getContentResolver(), uri)) {
            Toast.makeText(this, R.string.erreur_fichier_introuvable, Toast.LENGTH_SHORT).show();
            return;
        }

        String mime = getContentResolver().getType(uri);
        if (mime == null) {
            mime = "*/*";
        }

        // "Ouvre ce fichier avec une application qui sait le lire", en lui prêtant le droit de le lire.
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, mime);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Aucune application n'est installée pour ce type de fichier.
            Toast.makeText(this, R.string.erreur_aucune_app_fichier, Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Affichage =====

    private void afficherInterlocuteur(Etudiant etudiant) {
        if (etudiant == null) {
            finish();
            return;
        }
        String nomComplet = getString(R.string.nom_complet,
                etudiant.getNom(), etudiant.getPrenom());
        binding.tvTitre.setText(getString(R.string.conversation_titre, nomComplet));

        // NOUVEAU (11.5) : l'avatar de l'interlocuteur, avec les mêmes initiales et la même couleur
        // que dans la liste et la fiche.
        binding.tvInitiales.setText(Avatar.initiales(etudiant));
        binding.tvInitiales.setBackgroundTintList(
                ColorStateList.valueOf(Avatar.couleur(this, etudiant)));
    }

    private void afficherMessages(List<Message> messages) {
        adapter.setMessages(messages);

        boolean vide = messages.isEmpty();
        binding.tvVide.setVisibility(vide ? View.VISIBLE : View.GONE);

        if (!vide) {
            binding.rvMessages.scrollToPosition(messages.size() - 1);
        }
    }

    // ===== Envoi =====

    private void envoyer() {
        String texte = binding.etMessage.getText().toString().trim();
        boolean aPiece = messageViewModel.aPieceJointe();

        // VALIDATION : il faut un texte OU une pièce jointe (les deux, c'est possible aussi).
        if (texte.isEmpty() && !aPiece) {
            binding.etMessage.setError(getString(R.string.erreur_message_vide));
            binding.etMessage.requestFocus();
            return;
        }

        Message message = new Message(
                profilId, interlocuteurId, texte, System.currentTimeMillis());

        // Si une pièce est en attente, on la range dans le message.
        if (aPiece) {
            message.setPieceJointeUri(messageViewModel.getPieceUri());
            message.setPieceJointeType(messageViewModel.getPieceType());
            message.setPieceJointeNom(messageViewModel.getPieceNom());
        }

        // Le Repository enregistre le message, puis déclenche la notification pour le destinataire.
        messageViewModel.envoyer(message);

        // Le message est parti : on vide le champ et la pièce en attente.
        binding.etMessage.setText("");
        messageViewModel.effacerPieceJointe();
        afficherPieceChoisie();
    }
}