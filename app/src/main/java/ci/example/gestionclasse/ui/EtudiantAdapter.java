package ci.example.gestionclasse.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ci.example.gestionclasse.R;
import ci.example.gestionclasse.data.entity.Etudiant;
import ci.example.gestionclasse.databinding.ItemEtudiantBinding;
import ci.example.gestionclasse.util.Avatar;

// L'ADAPTER : relie la liste d'étudiants au RecyclerView.
// Il crée les lignes, les remplit et signale les clics à l'écran.
public class EtudiantAdapter
        extends RecyclerView.Adapter<EtudiantAdapter.EtudiantViewHolder> {

    // Contrat d'écoute : l'écran dit quoi faire quand on touche un étudiant.
    public interface OnEtudiantClickListener {
        void onEtudiantClick(Etudiant etudiant);
    }

    // Copie locale de la liste affichée. Elle est remplacée à chaque changement de la base.
    private final List<Etudiant> etudiants = new ArrayList<>();
    private final OnEtudiantClickListener listener;

    public EtudiantAdapter(OnEtudiantClickListener listener) {
        this.listener = listener;
    }

    // Appelée par l'écran quand la base envoie une nouvelle liste.
    // notifyDataSetChanged : "toute la liste a changé, redessine-la".
    public void setEtudiants(List<Etudiant> nouveaux) {
        etudiants.clear();
        etudiants.addAll(nouveaux);
        notifyDataSetChanged();
    }

    // Fabrique une ligne vide à partir de item_etudiant.xml.
    @NonNull
    @Override
    public EtudiantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemEtudiantBinding itemBinding = ItemEtudiantBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false);
        return new EtudiantViewHolder(itemBinding);
    }

    // Remplit une ligne avec l'étudiant placé à cette position.
    @Override
    public void onBindViewHolder(@NonNull EtudiantViewHolder holder, int position) {
        Etudiant etudiant = etudiants.get(position);
        Context contexte = holder.itemView.getContext();

        holder.binding.tvNomComplet.setText(
                contexte.getString(R.string.nom_complet, etudiant.getNom(), etudiant.getPrenom()));
        holder.binding.tvContact.setText(
                contexte.getString(R.string.etudiant_contact,
                        etudiant.getEmail(), etudiant.getTelephone()));

        // NOUVEAU (11.4) : la pastille. Initiales du prénom et du nom, et une couleur propre à l'étudiant.
        // setBackgroundTintList colore le cercle blanc du dessin avec cette couleur.
        holder.binding.tvInitiales.setText(Avatar.initiales(etudiant));
        holder.binding.tvInitiales.setBackgroundTintList(
                ColorStateList.valueOf(Avatar.couleur(contexte, etudiant)));

        // Clic sur la ligne : on redemande la position ACTUELLE au moment du clic.
        holder.itemView.setOnClickListener(v -> {
            int positionActuelle = holder.getBindingAdapterPosition();
            if (positionActuelle != RecyclerView.NO_POSITION) {
                listener.onEtudiantClick(etudiants.get(positionActuelle));
            }
        });
    }

    @Override
    public int getItemCount() {
        return etudiants.size();
    }

    // Une ligne déjà construite : elle garde ses vues en mémoire pour être réutilisée.
    static class EtudiantViewHolder extends RecyclerView.ViewHolder {

        final ItemEtudiantBinding binding;

        EtudiantViewHolder(ItemEtudiantBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}