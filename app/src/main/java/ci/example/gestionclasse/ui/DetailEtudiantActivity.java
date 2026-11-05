package ci.example.gestionclasse.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import ci.example.gestionclasse.R;
import ci.example.gestionclasse.data.entity.Etudiant;
import ci.example.gestionclasse.databinding.ActivityDetailEtudiantBinding;
import ci.example.gestionclasse.util.Avatar;
import ci.example.gestionclasse.util.Constantes;
import ci.example.gestionclasse.viewmodel.EtudiantViewModel;

// Fiche détaillée d'un étudiant, avec les actions Appeler, Email, SMS, Modifier et Supprimer.
public class DetailEtudiantActivity extends AppCompatActivity {

    private ActivityDetailEtudiantBinding binding;
    private EtudiantViewModel viewModel;
    private String role;
    private boolean estEnseignant;

    // L'étudiant actuellement affiché, mis à jour à chaque changement de la base.
    private Etudiant etudiantCourant;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDetailEtudiantBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        role = getIntent().getStringExtra(Constantes.EXTRA_ROLE);
        long id = getIntent().getLongExtra(Constantes.EXTRA_ETUDIANT_ID, Constantes.ID_AUCUN);

        // Sans rôle ou sans étudiant, rien à afficher : on ferme l'écran.
        if (role == null || id == Constantes.ID_AUCUN) {
            finish();
            return;
        }
        estEnseignant = Constantes.ROLE_ENSEIGNANT.equals(role);

        // Modifier et Supprimer sont cachés pour l'étudiant.
        if (!estEnseignant) {
            binding.layoutActionsEnseignant.setVisibility(View.GONE);
        }

        binding.btnAppeler.setOnClickListener(v -> appeler());
        // Email et SMS : accessibles aux deux rôles comme "Appeler".
        binding.btnEmail.setOnClickListener(v -> ecrireEmail());
        binding.btnSms.setOnClickListener(v -> ecrireSms());
        binding.btnModifier.setOnClickListener(v -> ouvrirModification());
        binding.btnSupprimer.setOnClickListener(v -> demanderConfirmationSuppression());
        binding.btnRetour.setOnClickListener(v -> finish());

        viewModel = new ViewModelProvider(this).get(EtudiantViewModel.class);
        viewModel.getEtudiant(id).observe(this, this::afficherEtudiant);
    }

    // Reçoit l'étudiant lu dans la base. null signifie qu'il a été supprimé : on quitte l'écran.
    private void afficherEtudiant(Etudiant etudiant) {
        if (etudiant == null) {
            finish();
            return;
        }
        etudiantCourant = etudiant;
        binding.tvNomComplet.setText(
                getString(R.string.nom_complet, etudiant.getNom(), etudiant.getPrenom()));
        binding.tvEmail.setText(etudiant.getEmail());
        binding.tvTelephone.setText(etudiant.getTelephone());

        // NOUVEAU (11.4) : le grand avatar, avec les mêmes initiales et la même couleur que dans la liste.
        binding.tvInitiales.setText(Avatar.initiales(etudiant));
        binding.tvInitiales.setBackgroundTintList(
                ColorStateList.valueOf(Avatar.couleur(this, etudiant)));
    }

    // APPEL : ACTION_DIAL ouvre le composeur avec le numéro déjà saisi, SANS lancer l'appel.
    // Elle ne demande donc aucune permission.
    private void appeler() {
        if (etudiantCourant == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DIAL,
                Uri.fromParts("tel", etudiantCourant.getTelephone(), null));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // Aucune application ne sait composer un numéro.
            Toast.makeText(this, R.string.erreur_appel_indisponible, Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Email et SMS =====

    // EMAIL : ACTION_SENDTO avec une adresse "mailto:" ne réveille QUE les applications d'email.
    // Le destinataire est déjà rempli, l'utilisateur écrit puis envoie lui-même.
    private void ecrireEmail() {
        if (etudiantCourant == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SENDTO,
                Uri.fromParts("mailto", etudiantCourant.getEmail(), null));
        lancerAvecProtection(intent, R.string.erreur_aucune_app_email);
    }

    // SMS : même principe avec "smsto:". Aucun SMS n'est envoyé, donc aucune permission SEND_SMS.
    private void ecrireSms() {
        if (etudiantCourant == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SENDTO,
                Uri.fromParts("smsto", etudiantCourant.getTelephone(), null));
        lancerAvecProtection(intent, R.string.erreur_aucune_app_sms);
    }

    // Lance un Intent implicite en cachant le cas "aucune application ne sait le traiter".
    // On utilise try/catch plutôt que resolveActivity : depuis Android 11, resolveActivity peut
    // répondre "personne" à tort, à cause des règles de visibilité entre applications.
    private void lancerAvecProtection(Intent intent, int messageErreur) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, messageErreur, Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Modifier et supprimer =====

    private void ouvrirModification() {
        // PROTECTION dans le code, en plus du bouton caché.
        if (!estEnseignant || etudiantCourant == null) {
            return;
        }
        Intent intent = new Intent(this, FormulaireEtudiantActivity.class);
        intent.putExtra(Constantes.EXTRA_ROLE, role);
        intent.putExtra(Constantes.EXTRA_ETUDIANT_ID, etudiantCourant.getId());
        startActivity(intent);
    }

    // CONFIRMATION avant de supprimer : une boîte de dialogue avec deux choix.
    private void demanderConfirmationSuppression() {
        if (!estEnseignant || etudiantCourant == null) {
            return;
        }
        String nomComplet = getString(R.string.nom_complet,
                etudiantCourant.getNom(), etudiantCourant.getPrenom());

        new AlertDialog.Builder(this)
                .setTitle(R.string.dialog_supprimer_titre)
                .setMessage(getString(R.string.dialog_supprimer_message, nomComplet))
                .setPositiveButton(R.string.action_supprimer, (dialog, which) -> supprimer())
                .setNegativeButton(R.string.action_annuler, null)
                .show();
    }

    private void supprimer() {
        viewModel.supprimer(etudiantCourant);
        Toast.makeText(this, R.string.msg_etudiant_supprime, Toast.LENGTH_SHORT).show();
        finish();
    }
}