package ci.example.gestionclasse.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import ci.example.gestionclasse.data.entity.Classe;
import ci.example.gestionclasse.data.repository.GestionRepository;

// Le VIEWMODEL : il survit aux rotations de l'écran et sert d'intermédiaire entre l'écran et le Repository.
// AndroidViewModel : un ViewModel qui reçoit l'Application (nécessaire pour ouvrir la base).
public class ClasseViewModel extends AndroidViewModel {

    private final GestionRepository repository;
    private final LiveData<Classe> classe;

    public ClasseViewModel(@NonNull Application application) {
        super(application);
        repository = new GestionRepository(application);
        classe = repository.observerClasse();
    }

    // L'écran OBSERVE ce LiveData : il est rappelé à chaque changement de la table.
    public LiveData<Classe> getClasse() {
        return classe;
    }

    // L'écran demande l'enregistrement ; le Repository s'occupe du thread d'arrière-plan.
    public void enregistrer(Classe classe) {
        repository.enregistrerClasse(classe);
    }
}