package com.unipi.sam.getnotes.groups;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.firebase.ui.database.FirebaseRecyclerAdapter;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.firebase.ui.database.paging.DatabasePagingOptions;
import com.firebase.ui.database.paging.FirebaseRecyclerPagingAdapter;
import com.google.android.material.card.MaterialCardView;
import com.unipi.sam.getnotes.R;
import com.unipi.sam.getnotes.table.Group;

public class OnlineGroupCardAdapter extends FirebaseRecyclerAdapter<Group, OnlineGroupCardAdapter.ViewHolder> {

    public OnlineGroupCardAdapter(@NonNull FirebaseRecyclerOptions<Group> options) {
        super(options);
    }
    public interface OnGroupClickListener{
        void onGroupClick(Group g);
    }
    private OnGroupClickListener listener;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.group_card_view, parent, false);
        return new ViewHolder(view);
    }
    @Override
    protected void onBindViewHolder(@NonNull ViewHolder holder, int position, @NonNull Group g) {
        holder.groupName.setText(g.getGroupName());
        holder.info.setText("Creato da: " + g.getAuthorName());
        holder.letter.setText(String.valueOf(g.getGroupName().charAt(0)).toUpperCase());
        holder.cardView.setOnClickListener(v -> {
            if(listener != null)
                listener.onGroupClick(g);
        });
    }

    public void setOnGroupClickListener(OnGroupClickListener listener) {
        this.listener = listener;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView groupName;
        private final TextView info;
        private final TextView letter;
        private final MaterialCardView cardView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            groupName = itemView.findViewById(R.id.groupName);
            info = itemView.findViewById(R.id.infoLabel);
            letter = itemView.findViewById(R.id.icon_letter);
            cardView = itemView.findViewById(R.id.parent);
        }
    }
}
