package ci.example.gestionclasse.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import ci.example.gestionclasse.R;
import ci.example.gestionclasse.databinding.ActivityRoleBinding;
import ci.example.gestionclasse.util.Constantes;
import ci.example.gestionclasse.util.ThemeHelper;

// Écran de choix du mode : enseignant ou étudiant.
public class RoleActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivityRoleBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRoleBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.btnEnseignant.setOnClickListener(this);
        binding.btnEtudiant.setOnClickListener(this);

        // NOUVEAU (11.6) : le bouton rond d'apparence, en haut à droite de la bannière.
        binding.btnTheme.setOnClickListener(v -> afficherChoixTheme());
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btnEnseignant) {
            ouvrirAccueilEnseignant();
        } else if (view.getId() == R.id.btnEtudiant) {
            ouvrirChoixProfil();
        }
    }

    // Enseignant : accès direct à l'accueil.
    private void ouvrirAccueilEnseignant() {
        Intent intent = new Intent(RoleActivity.this, AccueilActivity.class);
        intent.putExtra(Constantes.EXTRA_ROLE, Constantes.ROLE_ENSEIGNANT);
        startActivity(intent);
    }

    // Étudiant : d'abord choisir QUI il est, dans la liste des étudiants.
    private void ouvrirChoixProfil() {
        Intent intent = new Intent(RoleActivity.this, ListeEtudiantsActivity.class);
        intent.putExtra(Constantes.EXTRA_ROLE, Constantes.ROLE_ETUDIANT);
        intent.putExtra(Constantes.EXTRA_MODE_LISTE, Constantes.MODE_CHOISIR_PROFIL);
        startActivity(intent);
    }

    // NOUVEAU (11.6) : une boîte de dialogue avec trois choix, dont celui en cours est pré-coché.
    private void afficherChoixTheme() {
        String[] libelles = {
                getString(R.string.theme_auto),
                getString(R.string.theme_clair),
                getString(R.string.theme_sombre)
        };
        // Les trois modes, dans le même ordre que les libellés.
        int[] modes = {
                AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM,
                AppCompatDelegate.MODE_NIGHT_NO,
                AppCompatDelegate.MODE_NIGHT_YES
        };

        // On cherche quel choix est actuellement actif pour le cocher.
        int modeActuel = ThemeHelper.getMode(this);
        int coche = 0;
        for (int i = 0; i < modes.length; i++) {
            if (modes[i] == modeActuel) {
                coche = i;
            }
        }

        new AlertDialog.Builder(this)
                .setTitle(R.string.theme_titre)
                .setSingleChoiceItems(libelles, coche, (dialog, which) -> {
                    // On ferme la boîte AVANT d'appliquer : le changement recrée l'écran.
                    dialog.dismiss();
                    ThemeHelper.choisir(this, modes[which]);
                })
                .setNegativeButton(R.string.action_annuler, null)
                .show();
    }
}