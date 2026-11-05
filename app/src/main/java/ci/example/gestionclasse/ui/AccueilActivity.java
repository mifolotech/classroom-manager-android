package ci.example.gestionclasse.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

import ci.example.gestionclasse.R;
import ci.example.gestionclasse.data.entity.Classe;
import ci.example.gestionclasse.data.entity.Etudiant;
import ci.example.gestionclasse.databinding.ActivityAccueilBinding;
import ci.example.gestionclasse.util.Constantes;
import ci.example.gestionclasse.viewmodel.ClasseViewModel;
import ci.example.gestionclasse.viewmodel.EtudiantViewModel;

// Écran d'accueil : mode, profil (étudiant), classe et accès aux autres écrans.
public class AccueilActivity extends AppCompatActivity {

    private ActivityAccueilBinding binding;
    private String role;
    // Profil courant : l'identifiant de l'étudiant connecté (ID_AUCUN pour l'enseignant).
    private long profilId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityAccueilBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        role = getIntent().getStringExtra(Constantes.EXTRA_ROLE);
        profilId = getIntent().getLongExtra(Constantes.EXTRA_PROFIL_ID, Constantes.ID_AUCUN);

        if (role == null) {
            finish();
            return;
        }

        boolean estEnseignant = Constantes.ROLE_ENSEIGNANT.equals(role);

        // PROTECTION : un étudiant doit avoir choisi son profil.
        if (!estEnseignant && profilId == Constantes.ID_AUCUN) {
            finish();
            return;
        }

        // Textes et boutons selon le rôle.
        binding.tvRole.setText(estEnseignant ? R.string.mode_enseignant : R.string.mode_etudiant);
        binding.btnClasse.setText(
                estEnseignant ? R.string.action_gerer_classe : R.string.action_consulter_classe);
        binding.btnEtudiants.setText(
                estEnseignant ? R.string.action_gerer_etudiants : R.string.action_voir_etudiants);

        // NOUVEAU (11.3) : le ViewModel des étudiants sert maintenant aux DEUX rôles.
        // On observe la liste complète pour afficher le nombre d'étudiants dans la bannière.
        EtudiantViewModel etudiantViewModel =
                new ViewModelProvider(this).get(EtudiantViewModel.class);
        etudiantViewModel.getEtudiants().observe(this, this::afficherNombreEtudiants);

        if (!estEnseignant) {
            binding.tvProfil.setVisibility(View.VISIBLE);
            binding.btnEcrire.setVisibility(View.VISIBLE);
            binding.btnChangerMode.setText(R.string.action_changer_profil);

            // OBSERVE le profil : si l'enseignant le supprime, l'écran se ferme tout seul.
            etudiantViewModel.getEtudiant(profilId).observe(this, this::afficherProfil);
        }

        // Clics des boutons (lambdas).
        binding.btnClasse.setOnClickListener(v -> startActivity(creerIntent(ClasseActivity.class)));
        binding.btnEtudiants.setOnClickListener(v -> ouvrirListe(Constantes.MODE_VOIR));
        binding.btnEcrire.setOnClickListener(
                v -> ouvrirListe(Constantes.MODE_CHOISIR_DESTINATAIRE));
        // finish() : ramène à l'écran précédent (le choix du profil pour l'étudiant, du rôle pour l'enseignant).
        binding.btnChangerMode.setOnClickListener(v -> finish());

        ClasseViewModel classeViewModel = new ViewModelProvider(this).get(ClasseViewModel.class);
        classeViewModel.getClasse().observe(this, this::afficherClasse);
    }

    // Prépare un Intent explicite vers un écran, avec le rôle ET le profil courant.
    private Intent creerIntent(Class<?> ecran) {
        Intent intent = new Intent(AccueilActivity.this, ecran);
        intent.putExtra(Constantes.EXTRA_ROLE, role);
        intent.putExtra(Constantes.EXTRA_PROFIL_ID, profilId);
        return intent;
    }

    // Ouvre la liste des étudiants dans le mode demandé.
    private void ouvrirListe(String mode) {
        Intent intent = creerIntent(ListeEtudiantsActivity.class);
        intent.putExtra(Constantes.EXTRA_MODE_LISTE, mode);
        startActivity(intent);
    }

    // NOUVEAU (11.3) : affiche "N étudiant(s)" dans la bannière. Appelée à chaque changement de la liste.
    private void afficherNombreEtudiants(List<Etudiant> liste) {
        binding.tvNbEtudiants.setVisibility(View.VISIBLE);
        // getQuantityString choisit "étudiant" ou "étudiants" selon le nombre.
        binding.tvNbEtudiants.setText(getResources().getQuantityString(
                R.plurals.nb_etudiants, liste.size(), liste.size()));
    }

    // Reçoit le profil lu dans la base. null signifie qu'il a été supprimé.
    private void afficherProfil(Etudiant etudiant) {
        if (etudiant == null) {
            finish();
            return;
        }
        String nomComplet = getString(R.string.nom_complet,
                etudiant.getNom(), etudiant.getPrenom());
        binding.tvProfil.setText(getString(R.string.profil_courant, nomComplet));
    }

    private void afficherClasse(Classe classe) {
        if (classe == null) {
            binding.tvClasseVide.setVisibility(View.VISIBLE);
            binding.layoutClasse.setVisibility(View.GONE);
        } else {
            binding.tvClasseVide.setVisibility(View.GONE);
            binding.layoutClasse.setVisibility(View.VISIBLE);
            binding.tvClasseIntitule.setText(classe.getIntitule());
            binding.tvClasseAnnee.setText(
                    getString(R.string.classe_annee, classe.getAnneeUniversitaire()));
            binding.tvClasseDescription.setText(classe.getDescription());
        }
    }
}