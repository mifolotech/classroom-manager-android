package ci.example.gestionclasse.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import ci.example.gestionclasse.R;
import ci.example.gestionclasse.data.entity.Etudiant;
import ci.example.gestionclasse.databinding.ActivityListeEtudiantsBinding;
import ci.example.gestionclasse.util.Constantes;
import ci.example.gestionclasse.viewmodel.EtudiantViewModel;

// Liste des étudiants, réutilisée dans 3 situations (les 3 "modes"),
// avec recherche, compteur, partage et tri.
public class ListeEtudiantsActivity extends AppCompatActivity {

    // NOUVEAU (E5) : clé sous laquelle on sauvegarde le sens du tri pendant une rotation.
    private static final String ETAT_TRI_CROISSANT = "tri_croissant";

    private ActivityListeEtudiantsBinding binding;
    private EtudiantAdapter adapter;

    private String role;
    private String mode;
    private long profilId;

    // Message affiché quand la liste est vide (il dépend du mode).
    private int messageVide;

    // Tous les étudiants reçus de la base, le texte tapé dans la recherche,
    // et un drapeau qui indique si la base a déjà répondu.
    private List<Etudiant> tousLesEtudiants = new ArrayList<>();
    private String recherche = "";
    private boolean donneesChargees = false;

    // NOUVEAU (E5) : le sens du tri (vrai = A → Z) et le comparateur de texte de la langue du téléphone.
    // Collator : il connaît l'alphabet de la langue, donc "Émile" se range avec les E.
    private boolean triCroissant = true;
    private final Collator collator = Collator.getInstance(Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityListeEtudiantsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        role = getIntent().getStringExtra(Constantes.EXTRA_ROLE);
        if (role == null) {
            finish();
            return;
        }

        mode = getIntent().getStringExtra(Constantes.EXTRA_MODE_LISTE);
        if (mode == null) {
            mode = Constantes.MODE_VOIR;
        }

        profilId = getIntent().getLongExtra(Constantes.EXTRA_PROFIL_ID, Constantes.ID_AUCUN);

        // PROTECTION : choisir un destinataire n'a aucun sens sans savoir qui écrit.
        if (Constantes.MODE_CHOISIR_DESTINATAIRE.equals(mode)
                && profilId == Constantes.ID_AUCUN) {
            finish();
            return;
        }

        // NOUVEAU (E5) : PRIMARY ignore les majuscules et les accents pendant la comparaison.
        // Après une rotation, on retrouve le sens du tri choisi avant.
        collator.setStrength(Collator.PRIMARY);
        if (savedInstanceState != null) {
            triCroissant = savedInstanceState.getBoolean(ETAT_TRI_CROISSANT, true);
        }

        configurerTextes();

        // Les boutons "Ajouter" et "Partager" n'existent que pour l'enseignant en mode "voir".
        if (Constantes.MODE_VOIR.equals(mode) && Constantes.ROLE_ENSEIGNANT.equals(role)) {
            binding.btnAjouter.setVisibility(View.VISIBLE);
            binding.btnAjouter.setOnClickListener(v -> ouvrirFormulaireAjout());

            binding.btnPartager.setVisibility(View.VISIBLE);
            binding.btnPartager.setOnClickListener(v -> partagerListe());
        }
        binding.btnRetour.setOnClickListener(v -> finish());

        // NOUVEAU (E5) : le bouton de tri, présent dans les trois modes.
        mettreAJourBoutonTri();
        binding.btnTri.setOnClickListener(v -> {
            triCroissant = !triCroissant;        // on inverse le sens
            mettreAJourBoutonTri();
            rafraichir();
            binding.rvEtudiants.scrollToPosition(0);   // on revient en haut de la liste
        });

        adapter = new EtudiantAdapter(this::surClicEtudiant);
        binding.rvEtudiants.setLayoutManager(new LinearLayoutManager(this));
        binding.rvEtudiants.addItemDecoration(
                new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));
        binding.rvEtudiants.setAdapter(adapter);

        // À chaque lettre tapée dans la recherche, on filtre à nouveau la liste.
        binding.etRecherche.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                recherche = s.toString();
                rafraichir();
            }
        });

        // OBSERVE la liste : on garde la liste complète, puis on l'affiche (filtrée et triée).
        EtudiantViewModel viewModel = new ViewModelProvider(this).get(EtudiantViewModel.class);
        viewModel.getEtudiants().observe(this, liste -> {
            tousLesEtudiants = liste;
            donneesChargees = true;
            rafraichir();
        });
    }

    // NOUVEAU (E5) : appelée par Android avant de détruire l'écran (par exemple à la rotation).
    // On y range le sens du tri pour le retrouver dans onCreate.
    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(ETAT_TRI_CROISSANT, triCroissant);
    }

    // Titre et message "liste vide" selon le mode.
    private void configurerTextes() {
        if (Constantes.MODE_CHOISIR_PROFIL.equals(mode)) {
            binding.tvTitreListe.setText(R.string.etudiants_titre_profil);
            messageVide = R.string.etudiants_vide_profil;
        } else if (Constantes.MODE_CHOISIR_DESTINATAIRE.equals(mode)) {
            binding.tvTitreListe.setText(R.string.etudiants_titre_destinataire);
            messageVide = R.string.etudiants_vide_destinataire;
        } else {
            binding.tvTitreListe.setText(R.string.etudiants_titre);
            messageVide = R.string.etudiants_vide;
        }
    }

    // ===== NOUVEAU (E5) : Tri =====

    // Le bouton affiche le tri ACTUEL : "Tri : A → Z" ou "Tri : Z → A".
    private void mettreAJourBoutonTri() {
        binding.btnTri.setText(triCroissant ? R.string.tri_croissant : R.string.tri_decroissant);
    }

    // Trie la liste par nom, puis par prénom, puis par identifiant (pour que l'ordre soit toujours identique).
    // croissant = false : on inverse le comparateur, ce qui donne Z → A.
    private void trier(List<Etudiant> liste, boolean croissant) {
        // Comparator : une règle qui dit, pour deux étudiants, lequel passe avant l'autre.
        Comparator<Etudiant> comparateur = (a, b) -> {
            int resultat = collator.compare(a.getNom(), b.getNom());
            if (resultat == 0) {
                resultat = collator.compare(a.getPrenom(), b.getPrenom());
            }
            if (resultat == 0) {
                resultat = Long.compare(a.getId(), b.getId());
            }
            return resultat;
        };
        if (!croissant) {
            comparateur = comparateur.reversed();
        }
        Collections.sort(liste, comparateur);
    }

    // Construit la liste à afficher : sans soi-même (mode destinataire), filtrée par la recherche, puis triée.
    private void rafraichir() {
        // Tant que la base n'a pas répondu, on n'affiche rien (évite de montrer "liste vide" par erreur).
        if (!donneesChargees) {
            return;
        }

        String requete = recherche.trim().toLowerCase(Locale.getDefault());
        List<Etudiant> aAfficher = new ArrayList<>();

        for (Etudiant e : tousLesEtudiants) {
            // En mode destinataire, on ne s'écrit pas à soi-même.
            boolean estSoiMeme = Constantes.MODE_CHOISIR_DESTINATAIRE.equals(mode)
                    && e.getId() == profilId;
            if (estSoiMeme) {
                continue;
            }

            // La recherche porte sur "nom prénom", sans tenir compte des majuscules.
            String nomComplet = (e.getNom() + " " + e.getPrenom()).toLowerCase(Locale.getDefault());
            if (requete.isEmpty() || nomComplet.contains(requete)) {
                aAfficher.add(e);
            }
        }

        // NOUVEAU (E5) : on trie le résultat dans le sens choisi.
        trier(aAfficher, triCroissant);

        adapter.setEtudiants(aAfficher);

        // Compteur : "1 étudiant", "3 étudiants" (getQuantityString choisit la bonne forme).
        binding.tvCompteur.setText(getResources().getQuantityString(
                R.plurals.nb_etudiants, aAfficher.size(), aAfficher.size()));

        // Liste vide : "aucun résultat" si une recherche est en cours, sinon le message du mode.
        boolean vide = aAfficher.isEmpty();
        binding.tvVide.setText(requete.isEmpty() ? messageVide : R.string.etudiants_aucun_resultat);
        binding.tvVide.setVisibility(vide ? View.VISIBLE : View.GONE);
        binding.rvEtudiants.setVisibility(vide ? View.GONE : View.VISIBLE);
    }

    // ===== Partage de la liste (E4) =====

    // Écrit la liste de TOUTE la classe en texte, puis ouvre la fenêtre de partage d'Android.
    private void partagerListe() {
        // PROTECTION dans le code, en plus du bouton caché : seul l'enseignant peut exporter ces données.
        if (!Constantes.ROLE_ENSEIGNANT.equals(role)) {
            return;
        }
        // Rien à partager : on prévient au lieu d'envoyer un texte vide.
        if (!donneesChargees || tousLesEtudiants.isEmpty()) {
            Toast.makeText(this, R.string.partage_vide, Toast.LENGTH_SHORT).show();
            return;
        }

        // NOUVEAU (E5) : on partage une COPIE triée de A à Z, quel que soit le tri affiché à l'écran.
        List<Etudiant> classe = new ArrayList<>(tousLesEtudiants);
        trier(classe, true);

        StringBuilder texte = new StringBuilder();
        texte.append(getString(R.string.partage_titre, classe.size())).append("\n");

        int numero = 1;
        for (Etudiant e : classe) {
            texte.append("\n").append(getString(R.string.partage_ligne,
                    numero, e.getNom(), e.getPrenom(), e.getEmail(), e.getTelephone()));
            numero++;
        }

        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.partage_objet));
        intent.putExtra(Intent.EXTRA_TEXT, texte.toString());

        try {
            startActivity(Intent.createChooser(intent, getString(R.string.partage_chooser)));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, R.string.erreur_aucune_app_partage, Toast.LENGTH_SHORT).show();
        }
    }

    // ===== Navigation (inchangée) =====

    // Réaction au clic sur une ligne : elle dépend du mode.
    private void surClicEtudiant(Etudiant etudiant) {
        if (Constantes.MODE_CHOISIR_PROFIL.equals(mode)) {
            ouvrirAccueilEtudiant(etudiant);
        } else if (Constantes.MODE_CHOISIR_DESTINATAIRE.equals(mode)) {
            ouvrirConversation(etudiant);
        } else {
            ouvrirDetail(etudiant);
        }
    }

    // L'étudiant a choisi son profil : on ouvre l'accueil avec son identifiant.
    private void ouvrirAccueilEtudiant(Etudiant etudiant) {
        Intent intent = new Intent(this, AccueilActivity.class);
        intent.putExtra(Constantes.EXTRA_ROLE, role);
        intent.putExtra(Constantes.EXTRA_PROFIL_ID, etudiant.getId());
        startActivity(intent);
    }

    // Ouvre la conversation entre le profil courant et l'étudiant choisi.
    private void ouvrirConversation(Etudiant etudiant) {
        Intent intent = new Intent(this, ConversationActivity.class);
        intent.putExtra(Constantes.EXTRA_ROLE, role);
        intent.putExtra(Constantes.EXTRA_PROFIL_ID, profilId);
        intent.putExtra(Constantes.EXTRA_INTERLOCUTEUR_ID, etudiant.getId());
        startActivity(intent);
    }

    private void ouvrirFormulaireAjout() {
        Intent intent = new Intent(this, FormulaireEtudiantActivity.class);
        intent.putExtra(Constantes.EXTRA_ROLE, role);
        startActivity(intent);
    }

    private void ouvrirDetail(Etudiant etudiant) {
        Intent intent = new Intent(this, DetailEtudiantActivity.class);
        intent.putExtra(Constantes.EXTRA_ROLE, role);
        intent.putExtra(Constantes.EXTRA_ETUDIANT_ID, etudiant.getId());
        startActivity(intent);
    }
}