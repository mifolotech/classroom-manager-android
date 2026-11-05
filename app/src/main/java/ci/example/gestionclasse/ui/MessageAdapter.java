package ci.example.gestionclasse.ui;

import android.graphics.Paint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ci.example.gestionclasse.R;
import ci.example.gestionclasse.data.entity.Message;
import ci.example.gestionclasse.util.ApercuImage;
import ci.example.gestionclasse.util.PiecesJointes;

// L'ADAPTER de la conversation : deux types de lignes (envoyé, reçu).
public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

    // L'écran dit quoi faire quand on touche une pièce jointe.
    public interface OnPieceJointeClickListener {
        void onPieceJointeClick(Message message);
    }

    // L'écran dit quoi faire quand on reste appuyé sur un message (E1).
    public interface OnMessageLongClickListener {
        void onMessageLongClick(Message message);
    }

    private static final int TYPE_ENVOYE = 1;
    private static final int TYPE_RECU = 2;

    private final List<Message> messages = new ArrayList<>();
    private final long profilId;
    private final OnPieceJointeClickListener listener;
    private OnMessageLongClickListener longClickListener;

    private final SimpleDateFormat formatDate =
            new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());

    public MessageAdapter(long profilId, OnPieceJointeClickListener listener) {
        this.profilId = profilId;
        this.listener = listener;
    }

    // Permet à l'écran de brancher son écouteur de long appui (E1).
    public void setOnMessageLongClickListener(OnMessageLongClickListener longClickListener) {
        this.longClickListener = longClickListener;
    }

    public void setMessages(List<Message> nouveaux) {
        messages.clear();
        messages.addAll(nouveaux);
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getExpediteurId() == profilId ? TYPE_ENVOYE : TYPE_RECU;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == TYPE_ENVOYE)
                ? R.layout.item_message_envoye
                : R.layout.item_message_recu;
        View vue = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(vue);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messages.get(position);

        // Le LONG APPUI (E1), défini en premier car plusieurs éléments de la bulle l'utilisent :
        // un élément qui a déjà un clic court "avalerait" sinon le long appui.
        // Renvoyer true signifie : "j'ai géré cet appui".
        View.OnLongClickListener appuiLong = v -> {
            if (longClickListener == null) {
                return false;
            }
            longClickListener.onMessageLongClick(message);
            return true;
        };

        // Le texte : masqué s'il est vide (message qui ne contient qu'une pièce jointe).
        String contenu = message.getContenu();
        if (contenu == null || contenu.isEmpty()) {
            holder.tvContenu.setVisibility(View.GONE);
        } else {
            holder.tvContenu.setVisibility(View.VISIBLE);
            holder.tvContenu.setText(contenu);
        }

        // La pièce jointe : un lien souligné, ou rien du tout.
        // Les lignes sont RÉUTILISÉES par le RecyclerView : on traite toujours les DEUX cas.
        if (message.getPieceJointeUri() == null) {
            holder.tvPieceJointe.setVisibility(View.GONE);
            holder.tvPieceJointe.setOnClickListener(null);
        } else {
            holder.tvPieceJointe.setVisibility(View.VISIBLE);
            holder.tvPieceJointe.setText(PiecesJointes.libelle(
                    holder.itemView.getContext(),
                    message.getPieceJointeType(),
                    message.getPieceJointeNom()));
            holder.tvPieceJointe.setPaintFlags(
                    holder.tvPieceJointe.getPaintFlags() | Paint.UNDERLINE_TEXT_FLAG);
            holder.tvPieceJointe.setOnClickListener(v -> listener.onPieceJointeClick(message));
        }

        // NOUVEAU (E2) : la MINIATURE, seulement pour les photos.
        boolean estPhoto = message.getPieceJointeUri() != null
                && Message.TYPE_PHOTO.equals(message.getPieceJointeType());
        if (estPhoto) {
            holder.ivApercu.setVisibility(View.VISIBLE);
            ApercuImage.charger(holder.ivApercu, Uri.parse(message.getPieceJointeUri()));
            // Un appui sur la miniature ouvre la photo en grand, comme le lien.
            holder.ivApercu.setOnClickListener(v -> listener.onPieceJointeClick(message));
            holder.ivApercu.setOnLongClickListener(appuiLong);
        } else {
            // Ligne réutilisée pour autre chose qu'une photo : on nettoie la miniature précédente.
            holder.ivApercu.setVisibility(View.GONE);
            holder.ivApercu.setImageDrawable(null);
            holder.ivApercu.setTag(null);   // un chargement en retard sera ainsi ignoré
            holder.ivApercu.setOnClickListener(null);
            holder.ivApercu.setOnLongClickListener(null);
        }

        holder.tvDate.setText(formatDate.format(new Date(message.getDateEnvoi())));

        // Branche le long appui sur la ligne entière et sur le lien de la pièce jointe (E1).
        holder.itemView.setOnLongClickListener(appuiLong);
        holder.tvPieceJointe.setOnLongClickListener(appuiLong);
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {

        final ImageView ivApercu;
        final TextView tvContenu;
        final TextView tvPieceJointe;
        final TextView tvDate;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivApercu = itemView.findViewById(R.id.ivApercu);
            tvContenu = itemView.findViewById(R.id.tvContenu);
            tvPieceJointe = itemView.findViewById(R.id.tvPieceJointe);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }
}