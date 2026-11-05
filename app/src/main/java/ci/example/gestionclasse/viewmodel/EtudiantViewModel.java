package ci.example.gestionclasse.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import ci.example.gestionclasse.data.entity.Etudiant;
import ci.example.gestionclasse.data.repository.GestionRepository;

// ViewModel des écrans "étudiants" : il transmet les demandes au Repository.
public class EtudiantViewModel extends AndroidViewModel {

    private final GestionRepository repository;
    private final LiveData<List<Etudiant>> etudiants;

    public EtudiantViewModel(@NonNull Application application) {
        super(application);
        repository = new GestionRepository(application);
        etudiants = repository.observerEtudiants();
    }

    // La liste de tous les étudiants, observée en direct.
    public LiveData<List<Etudiant>> getEtudiants() {
        return etudiants;
    }

    // Un étudiant précis, observé en direct (écran de détail et de modification).
    public LiveData<Etudiant> getEtudiant(long id) {
        return repository.observerEtudiant(id);
    }

    public void ajouter(Etudiant etudiant) {
        repository.ajouterEtudiant(etudiant);
    }

    public void modifier(Etudiant etudiant) {
        repository.modifierEtudiant(etudiant);
    }

    public void supprimer(Etudiant etudiant) {
        repository.supprimerEtudiant(etudiant);
    }
}