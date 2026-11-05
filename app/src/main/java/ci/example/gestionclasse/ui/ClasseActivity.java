package ci.example.gestionclasse.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import ci.example.gestionclasse.R;
import ci.example.gestionclasse.data.entity.Classe;
import ci.example.gestionclasse.databinding.ActivityClasseBinding;
import ci.example.gestionclasse.util.Constantes;
import ci.example.gestionclasse.util.Validateur;
import ci.example.gestionclasse.viewmodel.ClasseViewModel;

// Écran de la classe : l'enseignant la crée et la modifie, l'étudiant la consulte seulement.
public class ClasseActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityClasseBinding binding;
    private ClasseViewModel classeViewModel;

    private boolean estEnseignant;

    // Vrai une fois que le formulaire de l'enseignant a été rempli avec la classe lue dans la base.
    // On ne le remplit qu'UNE fois, pour ne jamais écraser ce que l'enseignant est en train de taper.
    private boolean formulaireInitialise = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityClasseBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Rôle envoyé par AccueilActivity. Sans rôle, on ferme l'écran.
        String role = getIntent().getStringExtra(Constantes.EXTRA_ROLE);
        if (role == null) {
            finish();
            return;
        }
        estEnseignant = Constantes.ROLE_ENSEIGNANT.equals(role);

        // Même principe : après une rotation, les champs sont déjà restaurés, on ne les écrase pas.
        formulaireInitialise = savedInstanceState != null;

        configurerSelonRole();

        binding.btnEnregistrer.setOnClickListener(this);
        binding.btnRetour.setOnClickListener(this);

        // OBSERVE la classe : afficherClasse est rappelée à chaque changement de la table.
        classeViewModel = new ViewModelProvider(this).get(ClasseViewModel.class);
        classeViewModel.getClasse().observe(this, this::afficherClasse);
    }

    // L'étudiant est en LECTURE SEULE : champs désactivés et bouton d'enregistrement caché.
    private void configurerSelonRole() {
        if (!estEnseignant) {
            binding.tvLectureSeule.setVisibility(View.VISIBLE);
            binding.etIntitule.setEnabled(false);
            binding.etAnnee.setEnabled(false);
            binding.etDescription.setEnabled(false);
            binding.btnEnregistrer.setVisibility(View.GONE);
        }
    }

    // Reçoit la classe lue dans la base, ou null si elle n'existe pas encore.
    private void afficherClasse(Classe classe) {
        if (!estEnseignant) {
            // ÉTUDIANT : s'il n'y a pas de classe, on affiche un message au lieu du formulaire vide.
            if (classe == null) {
                binding.layoutFormulaire.setVisibility(View.GONE);
                binding.tvClasseAbsente.setVisibility(View.VISIBLE);
            } else {
                binding.layoutFormulaire.setVisibility(View.VISIBLE);
                binding.tvClasseAbsente.setVisibility(View.GONE);
                remplirChamps(classe);
            }
            return;
        }

        // ENSEIGNANT : on remplit le formulaire une seule fois, à la première réponse de la base.
        if (!formulaireInitialise) {
            formulaireInitialise = true;
            if (classe != null) {
                remplirChamps(classe);
            }
        }

        // Le bouton dit "Créer" tant qu'il n'y a pas de classe, puis "Enregistrer les modifications".
        if (classe == null) {
            binding.btnEnregistrer.setText(R.string.action_creer_classe);
        } else {
            binding.btnEnregistrer.setText(R.string.action_enregistrer_classe);
        }
    }

    private void remplirChamps(Classe classe) {
        binding.etIntitule.setText(classe.getIntitule());
        binding.etAnnee.setText(classe.getAnneeUniversitaire());
        binding.etDescription.setText(classe.getDescription());
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnEnregistrer) {
            enregistrer();
        } else if (view.getId() == R.id.btnRetour) {
            finish();
        }
    }

    // Valide le formulaire, puis enregistre la classe dans Room.
    private void enregistrer() {
        // PROTECTION dans le code : même si le bouton était atteint, un étudiant ne peut rien enregistrer.
        if (!estEnseignant) {
            return;
        }

        String intitule = binding.etIntitule.getText().toString().trim();
        String annee = binding.etAnnee.getText().toString().trim();
        String description = binding.etDescription.getText().toString().trim();

        // VALIDATION : on s'arrête à la première erreur, dans l'ordre des champs à l'écran.
        if (intitule.isEmpty()) {
            afficherErreur(binding.etIntitule, getString(R.string.erreur_champ_obligatoire));
            return;
        }
        if (annee.isEmpty()) {
            afficherErreur(binding.etAnnee, getString(R.string.erreur_champ_obligatoire));
            return;
        }
        if (!Validateur.estAnneeUniversitaireValide(annee)) {
            afficherErreur(binding.etAnnee, getString(R.string.erreur_annee));
            return;
        }
        if (description.isEmpty()) {
            afficherErreur(binding.etDescription, getString(R.string.erreur_champ_obligatoire));
            return;
        }

        // Tout est valide : on construit l'objet et on l'envoie au ViewModel.
        // Le Repository l'écrit en arrière-plan ; REPLACE crée la ligne ou la remplace.
        Classe classe = new Classe(intitule, description, annee);
        classeViewModel.enregistrer(classe);

        Toast.makeText(this, R.string.msg_classe_enregistree, Toast.LENGTH_SHORT).show();

        // On quitte l'écran : AccueilActivity, qui OBSERVE la classe, se mettra à jour toute seule.
        finish();
    }

    // Affiche le message sur le champ et y place le curseur.
    private void afficherErreur(EditText champ, String message) {
        champ.setError(message);
        champ.requestFocus();
    }
}