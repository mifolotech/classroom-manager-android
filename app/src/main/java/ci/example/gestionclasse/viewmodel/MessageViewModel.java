package ci.example.gestionclasse.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import ci.example.gestionclasse.data.entity.Message;
import ci.example.gestionclasse.data.repository.GestionRepository;

// ViewModel de la messagerie.
public class MessageViewModel extends AndroidViewModel {

    private final GestionRepository repository;

    // Pièce jointe choisie mais PAS ENCORE envoyée. Un ViewModel survit à la rotation de l'écran.
    private String pieceUri;
    private String pieceType;
    private String pieceNom;

    public MessageViewModel(@NonNull Application application) {
        super(application);
        repository = new GestionRepository(application);
    }

    public LiveData<List<Message>> getConversation(long a, long b) {
        return repository.observerConversation(a, b);
    }

    public void envoyer(Message message) {
        repository.envoyerMessage(message);
    }

    // NOUVEAU (E1) : supprime un message.
    public void supprimer(Message message) {
        repository.supprimerMessage(message);
    }

    // ===== Pièce jointe en attente =====

    public void choisirPieceJointe(String uri, String type, String nom) {
        pieceUri = uri;
        pieceType = type;
        pieceNom = nom;
    }

    public void effacerPieceJointe() {
        pieceUri = null;
        pieceType = null;
        pieceNom = null;
    }

    public boolean aPieceJointe() {
        return pieceUri != null;
    }

    public String getPieceUri() {
        return pieceUri;
    }

    public String getPieceType() {
        return pieceType;
    }

    public String getPieceNom() {
        return pieceNom;
    }
}