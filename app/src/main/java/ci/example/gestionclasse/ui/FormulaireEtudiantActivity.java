package ci.example.gestionclasse.ui;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import ci.example.gestionclasse.R;
import ci.example.gestionclasse.data.entity.Etudiant;
import ci.example.gestionclasse.databinding.ActivityFormulaireEtudiantBinding;
import ci.example.gestionclasse.util.Constantes;
import ci.example.gestionclasse.util.Validateur;
import ci.example.gestionclasse.viewmodel.EtudiantViewModel;

// Formulaire d'ajout ET de modification d'un étudiant. Réservé à l'enseignant.
public class FormulaireEtudiantActivity extends AppCompatActivity {

    private ActivityFormulaireEtudiantBinding binding;
    private EtudiantViewModel viewModel;

    // Identifiant de l'étudiant à modifier, ou ID_AUCUN pour un ajout.
    private long etudiantId;
    private boolean modeModification;
    private boolean formulaireInitialise = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFormulaireEtudiantBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // PROTECTION : seul l'enseignant a le droit d'ouvrir ce formulaire.
        String role = getIntent().getStringExtra(Constantes.EXTRA_ROLE);
        if (!Constantes.ROLE_ENSEIGNANT.equals(role)) {
            finish();
            return;
        }

        etudiantId = getIntent().getLongExtra(Constantes.EXTRA_ETUDIANT_ID, Constantes.ID_AUCUN);
        modeModification = etudiantId != Constantes.ID_AUCUN;

        // Après une rotation, Android a déjà restauré les champs tapés par l'utilisateur :
        // on ne les remplit pas une seconde fois avec la base, ce qui les écraserait.
        formulaireInitialise = savedInstanceState != null;

        viewModel = new ViewModelProvider(this).get(EtudiantViewModel.class);

        binding.btnEnregistrer.setOnClickListener(v -> enregistrer());
        binding.btnAnnuler.setOnClickListener(v -> finish());

        if (modeModification) {
            binding.tvTitreFormulaire.setText(R.string.form_titre_modif);
            // On lit l'étudiant dans la base pour préremplir les champs.
            viewModel.getEtudiant(etudiantId).observe(this, this::remplirChamps);
        } else {
            binding.tvTitreFormulaire.setText(R.string.form_titre_ajout);
        }
    }

    // Remplit les champs UNE seule fois, pour ne jamais écraser ce que l'enseignant tape.
    private void remplirChamps(Etudiant etudiant) {
        if (etudiant == null || formulaireInitialise) {
            return;
        }
        formulaireInitialise = true;
        binding.etNom.setText(etudiant.getNom());
        binding.etPrenom.setText(etudiant.getPrenom());
        binding.etEmail.setText(etudiant.getEmail());
        binding.etTelephone.setText(etudiant.getTelephone());
    }

    private void enregistrer() {
        String nom = binding.etNom.getText().toString().trim();
        String prenom = binding.etPrenom.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String telephone = binding.etTelephone.getText().toString().trim();

        // VALIDATION : on s'arrête à la première erreur, dans l'ordre des champs.
        if (nom.isEmpty()) {
            afficherErreur(binding.etNom, getString(R.string.erreur_champ_obligatoire));
            return;
        }
        if (prenom.isEmpty()) {
            afficherErreur(binding.etPrenom, getString(R.string.erreur_champ_obligatoire));
            return;
        }
        if (email.isEmpty()) {
            afficherErreur(binding.etEmail, getString(R.string.erreur_champ_obligatoire));
            return;
        }
        if (!Validateur.estEmailValide(email)) {
            afficherErreur(binding.etEmail, getString(R.string.erreur_email));
            return;
        }
        if (telephone.isEmpty()) {
            afficherErreur(binding.etTelephone, getString(R.string.erreur_champ_obligatoire));
            return;
        }
        if (!Validateur.estTelephoneValide(telephone)) {
            afficherErreur(binding.etTelephone, getString(R.string.erreur_telephone));
            return;
        }

        // On enregistre le numéro sans espaces, pour que l'appel fonctionne correctement.
        Etudiant etudiant = new Etudiant(nom, prenom, email,
                Validateur.normaliserTelephone(telephone));

        if (modeModification) {
            // Même id : @Update remplace la ligne existante au lieu d'en créer une autre.
            etudiant.setId(etudiantId);
            viewModel.modifier(etudiant);
            Toast.makeText(this, R.string.msg_etudiant_modifie, Toast.LENGTH_SHORT).show();
        } else {
            viewModel.ajouter(etudiant);
            Toast.makeText(this, R.string.msg_etudiant_ajoute, Toast.LENGTH_SHORT).show();
        }

        // La liste et le détail OBSERVENT la base : ils se mettent à jour tout seuls.
        finish();
    }

    private void afficherErreur(EditText champ, String message) {
        champ.setError(message);
        champ.requestFocus();
    }
}